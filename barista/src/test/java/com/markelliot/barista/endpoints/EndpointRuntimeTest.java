/*
 * (c) Copyright 2025 Mark Elliot. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markelliot.barista.endpoints;

import static org.assertj.core.api.Assertions.assertThat;

import com.markelliot.barista.authz.AuthToken;
import com.markelliot.barista.authz.Authz;
import com.markelliot.barista.authz.VerifiedAuthToken;
import com.markelliot.barista.serde.DispatchingSerDe;
import com.markelliot.result.Result;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class EndpointRuntimeTest {
    
    private EndpointRuntime<Exception> endpointRuntime;
    private TestAuthz testAuthz;
    
    @BeforeEach
    void setUp() {
        testAuthz = new TestAuthz();
        endpointRuntime = EndpointRuntime.<Exception>builder()
                .authz(testAuthz)
                .serde(DispatchingSerDe.createDefault())
                .build();
    }
    
    @Test
    void verifyAuth_missingAuthorizationHeader_returnsUnauthenticated() {
        HttpServerExchange exchange = createExchangeWithoutAuthHeader();
        
        Result<VerifiedAuthToken, HttpError> result = endpointRuntime.verifyAuth(exchange);

        assertThat(result.error()).hasValueSatisfying(error -> {
            assertThat(error.statusCode()).isEqualTo(401);
            assertThat(error.message()).isEqualTo("Unauthorized: Missing authorization authToken");
        });
    }
    
    @Test
    void verifyAuth_validAuthorizationHeader_returnsVerifiedToken() {
        HttpServerExchange exchange = createExchangeWithAuthHeader("Bearer valid-token");
        testAuthz.setShouldPass(true);
        
        Result<VerifiedAuthToken, HttpError> result = endpointRuntime.verifyAuth(exchange);
        
        assertThat(result.result()).isNotEmpty();
    }
    
    @Test
    void verifyAuth_invalidAuthorizationHeader_returnsUnauthorized() {
        HttpServerExchange exchange = createExchangeWithAuthHeader("Bearer invalid-token");
        testAuthz.setShouldPass(false);
        
        Result<VerifiedAuthToken, HttpError> result = endpointRuntime.verifyAuth(exchange);
        
        assertThat(result.error()).hasValueSatisfying(error -> {
            assertThat(error.statusCode()).isEqualTo(403);
            assertThat(error.message()).isEqualTo("Unauthorized: Invalid authorization authToken");
        });
    }
    
    private HttpServerExchange createExchangeWithoutAuthHeader() {
        return new HttpServerExchange(null, new HeaderMap(), new HeaderMap(), 0L);
    }
    
    private HttpServerExchange createExchangeWithAuthHeader(String authHeaderValue) {
        HeaderMap headers = new HeaderMap();
        headers.put(Headers.AUTHORIZATION, authHeaderValue);
        return new HttpServerExchange(null, headers, new HeaderMap(), 0L);
    }
    
    private static class TestAuthz implements Authz {
        private boolean shouldPass = false;
        
        void setShouldPass(boolean shouldPass) {
            this.shouldPass = shouldPass;
        }

        @Override
        public AuthToken newSession(String userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<VerifiedAuthToken> check(AuthToken authToken) {
            return shouldPass ? Optional.of(new VerifiedAuthToken(null, "fake-user-id")) : Optional.empty();
        }
    }
    
}