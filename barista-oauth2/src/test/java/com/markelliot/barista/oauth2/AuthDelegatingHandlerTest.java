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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.markelliot.barista.handlers.DelegatingHandler;
import com.palantir.tokens.auth.BearerToken;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class AuthDelegatingHandlerTest {

    private static final BearerToken BEARER_TOKEN = BearerToken.valueOf("test-token");
    private static final String REQUEST_URI = "https://test-uri";
    private static final String REQUEST_PATH_INFO = "test-uri";
    private static final String QUERY_STRING = "foo=bar";
    private static final String SIGNED_REDIRECT_URI = "signed-redirect-uri";
    private static final String SIGNED_STATE = "signed-state";
    private static final String COOKIE_PATH = "/cookie-path";

    @Mock private HttpHandler next;

    @Mock private OAuth2CookieFilter cookieAuthFilter;

    @Mock private OAuth2StateSerde oauth2StateSerde;

    @Mock private CookieManager cookieManager;

    private HttpServerExchange exchange;
    private HttpHandler handler;

    @Before
    public void before() {
        when(cookieAuthFilter.shouldDoOauth2Flow(
                        REQUEST_PATH_INFO, Optional.of(BEARER_TOKEN.getToken())))
                .thenReturn(false);
        when(cookieAuthFilter.shouldDoOauth2Flow(REQUEST_PATH_INFO, Optional.empty()))
                .thenReturn(true);
        when(oauth2StateSerde.encodeRedirectUrlToState(
                        URI.create(String.format("/?%s", QUERY_STRING))))
                .thenReturn(SIGNED_STATE);
        when(cookieAuthFilter.getAuthorizeRedirectUri(Optional.empty(), SIGNED_STATE))
                .thenReturn(SIGNED_REDIRECT_URI);

        exchange = new HttpServerExchange(null);
        exchange.setRequestURI(REQUEST_URI);
        exchange.setRequestPath(REQUEST_PATH_INFO);
        exchange.setQueryString(QUERY_STRING);

        handler = new AuthDelegatingHandler(
                        COOKIE_PATH,
                        cookieAuthFilter,
                        oauth2StateSerde,
                        cookieManager).innerHandler(next);
    }

    @Test
    public void test_redirect_filterChainStopped() throws Exception {
        when(cookieManager.getTokenCookie(exchange)).thenReturn(Optional.empty());
        handler.handleRequest(exchange);
        verify(next, never()).handleRequest(exchange);
        assertThat(exchange.getResponseHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo(SIGNED_REDIRECT_URI);
        verify(cookieManager).setStateCookie(exchange, COOKIE_PATH, SIGNED_STATE);
    }

    @Test
    public void test_goodCookie_filterChainContinued() throws Exception {
        when(cookieManager.getTokenCookie(exchange))
                .thenReturn(Optional.of(BEARER_TOKEN.getToken()));
        handler.handleRequest(exchange);
        verify(next).handleRequest(exchange);
    }
}
