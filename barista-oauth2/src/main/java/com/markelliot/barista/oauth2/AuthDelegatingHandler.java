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

import com.google.common.annotations.VisibleForTesting;
import com.markelliot.barista.handlers.DelegatingHandler;
import com.markelliot.barista.oauth2.objects.OAuth2Configuration;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.StatusCodes;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A standard {@link HttpHandler} that enforces cookie auth for OAuth2 workflows.
 *
 * <p>Must be used in concert with {@link AuthRedirectResource}.
 */
public final class AuthDelegatingHandler implements DelegatingHandler {
    private final String cookiePath;
    private final OAuth2CookieFilter auth;
    private final OAuth2StateSerde oauth2StateSerde;
    private final CookieManager cookieManager;

    @VisibleForTesting
    AuthDelegatingHandler(
            String cookiePath,
            OAuth2CookieFilter cookieAuthFilter,
            OAuth2StateSerde oauth2StateSerde,
            CookieManager cookieManager) {
        this.cookiePath = cookiePath;
        this.auth = cookieAuthFilter;
        this.oauth2StateSerde = oauth2StateSerde;
        this.cookieManager = cookieManager;
    }

    public static DelegatingHandler of(
            String cookiePath, OAuth2Client client, Supplier<OAuth2Configuration> config) {
        return new AuthDelegatingHandler(
                cookiePath,
                new OAuth2CookieFilter(client, config),
                new OAuth2StateSerdeImpl(),
                CookieManager.buildDefault());
    }

    @Override
    public HttpHandler handler(HttpHandler next) {
        // Must run on the task pool, authentication client blocks until completion.
        return new BlockingHandler(
                exchange -> {
                    String requestPath = exchange.getRequestPath().substring(cookiePath.length());

                    Optional<String> token = cookieManager.getTokenCookie(exchange);
                    if (auth.shouldDoOauth2Flow(requestPath, token)) {
                        String encodedState =
                                oauth2StateSerde.encodeRedirectUrlToState(
                                        getUriWithQueryParameters(exchange));
                        cookieManager.setStateCookie(exchange, cookiePath, encodedState);

                        Optional<String> host = Headers.getExternalHostHeader(exchange);
                        String authorizeRedirectUri =
                                auth.getAuthorizeRedirectUri(host, encodedState);

                        exchange.setStatusCode(StatusCodes.TEMPORARY_REDIRECT);
                        exchange.getResponseHeaders().put(io.undertow.util.Headers.LOCATION, authorizeRedirectUri);

                        // do not continue to process the handler chain
                        return;
                    }
                    next.handleRequest(exchange);
                });
    }

    private static URI getUriWithQueryParameters(HttpServerExchange exchange) {
        StringBuilder fullUrl = new StringBuilder(exchange.getRequestURI());
        String queryString = exchange.getQueryString();

        if (queryString != null) {
            fullUrl.append("?");
            fullUrl.append(queryString);
        }

        return OAuthRedirects.toLocalUri(URI.create(fullUrl.toString()));
    }
}
