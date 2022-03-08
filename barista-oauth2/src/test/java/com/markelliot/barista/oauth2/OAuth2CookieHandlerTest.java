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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.palantir.tokens.auth.BearerToken;
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
public final class OAuth2CookieHandlerTest {

    private static final BearerToken BEARER_TOKEN = BearerToken.valueOf("test-token");
    private static final String REQUEST_URI = "https://test-uri";
    private static final String REQUEST_PATH_INFO = "test-uri";
    private static final String QUERY_STRING = "foo=bar";
    private static final String SIGNED_REDIRECT_URI = "signed-redirect-uri";
    private static final String SIGNED_STATE = "signed-state";
    private static final String COOKIE_PATH = "/cookie-path";

    @Mock private HttpServletRequest request;

    @Mock private HttpServletResponse response;

    @Mock private FilterChain chain;

    @Mock private OAuth2CookieFilter cookieAuthFilter;

    @Mock private OAuth2StateSerde oauth2StateSerde;

    @Mock private CookieManager cookieManager;

    @Mock private Supplier<HttpServerExchange> exchangeSupplier;

    private OAuth2CookieHandler handler;

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

        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        when(request.getPathInfo()).thenReturn(REQUEST_PATH_INFO);
        when(request.getQueryString()).thenReturn(QUERY_STRING);

        handler =
                OAuth2CookieHandler.of(
                        cookieAuthFilter,
                        COOKIE_PATH,
                        oauth2StateSerde,
                        cookieManager,
                        exchangeSupplier);
    }

    @Test
    public void test_redirect_filterChainStopped() throws Exception {
        HttpServerExchange exchange = new HttpServerExchange(null);
        when(exchangeSupplier.get()).thenReturn(exchange);
        when(cookieManager.getTokenCookie(exchange)).thenReturn(Optional.empty());
        handler.handleRequest(request, response, chain);
        verify(chain, never()).doFilter(request, response);
        verify(response).setHeader(HttpHeaders.LOCATION, SIGNED_REDIRECT_URI);
        verify(cookieManager).setStateCookie(exchange, COOKIE_PATH, SIGNED_STATE);
    }

    @Test
    public void test_goodCookie_filterChainContinued() throws Exception {
        HttpServerExchange exchange = new HttpServerExchange(null);
        when(exchangeSupplier.get()).thenReturn(exchange);
        when(cookieManager.getTokenCookie(exchange))
                .thenReturn(Optional.of(BEARER_TOKEN.getToken()));
        handler.handleRequest(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
