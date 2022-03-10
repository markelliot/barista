/*
 * (c) Copyright 2022 Mark Elliot. All rights reserved.
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

package com.markelliot.barista.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.conjure.java.api.errors.RemoteException;
import com.palantir.tokens.auth.AuthHeader;
import com.palantir.tokens.auth.BearerToken;
import org.junit.jupiter.api.Test;

public final class OAuth2ClientTest {

    private static final int PORT = 8034;
    private static final String SERVICE = "oauth2";

    private static final BearerToken VALID_TOKEN = BearerToken.valueOf("valid");
    private static final AuthHeader VALID_AUTH_HEADER = AuthHeader.of(VALID_TOKEN);
    private static final int EXPIRES_IN = 3600;

    private static OAuth2Client createClient() {
        return null;
        //        Endpoint endpoint =
        //                Endpoint.builder()
        //                        .method(Methods.POST)
        //                        .template("/oauth2/token")
        //                        .name("createToken")
        //                        .serviceName("OAuth2Service")
        //                        .handler(
        //                                exchange -> {
        //                                    // Return the form encoded
        //                                    // request body as the
        //                                    // 'refreshToken' so the client
        //                                    // may validate.
        //                                    byte[] contents =
        //
        // ByteStreams.toByteArray(exchange.getInputStream());
        //                                    conjureRuntime
        //                                            .bodySerDe()
        //                                            .serializer(new
        // TypeMarker<OAuth2Credentials>() {})
        //                                            .serialize(
        //                                                    OAuth2Credentials.builder()
        //                                                            .bearerToken(
        //                                                                    VALID_AUTH_HEADER
        //
        // .getBearerToken())
        //                                                            .refreshToken(
        //                                                                    new String(
        //                                                                            contents,
        //
        // StandardCharsets.UTF_8))
        //                                                            .expiresIn(EXPIRES_IN)
        //                                                            .build(),
        //                                                    exchange);
        //                                });
    }

    @Test
    public void testCheckTokenSuccessful() {
        OAuth2Client client = createClient();
        assertThatCode(() -> client.checkToken(VALID_AUTH_HEADER)).doesNotThrowAnyException();
    }

    @Test
    public void testCheckTokenInvalid() {
        OAuth2Client client = createClient();
        assertThatThrownBy(() -> client.checkToken(AuthHeader.valueOf("invalid")))
                .isInstanceOfSatisfying(
                        RemoteException.class, re -> assertThat(re.getStatus()).isEqualTo(401));
    }

    @Test
    public void testCreateToken() {
        OAuth2Client client = createClient();
        OAuth2Credentials creds =
                client.createToken(
                        CreateTokenRequest.builder()
                                .grantType("a")
                                .authorizationCode("b")
                                .callbackUrl("c")
                                .authorization(
                                        OAuth2Authorization.builder()
                                                .clientId("d")
                                                .clientSecret("e")
                                                .build())
                                .build());
        assertThat(creds)
                .isEqualTo(
                        OAuth2Credentials.builder()
                                .bearerToken(VALID_TOKEN)
                                .refreshToken(
                                        "grant_type=a&code=b&redirect_uri=c&client_id=d&client_secret=e")
                                .expiresIn(EXPIRES_IN)
                                .build());
    }
}
