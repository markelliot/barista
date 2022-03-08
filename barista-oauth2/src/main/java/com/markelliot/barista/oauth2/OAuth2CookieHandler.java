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
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A standard {@link HttpHandler} that enforces cookie auth for OAuth2 workflows.
 *
 * <p>Must be used in concert with {@link AuthRedirectResource}.
 */
public final class OAuth2CookieHandler implements HttpHandler {
    private final HttpHandler next;
    private final String cookiePath;
    private final OAuth2CookieFilter auth;
    private final OAuth2StateSerde oauth2StateSerde;
    private final CookieManager cookieManager;

    private OAuth2CookieHandler(
            HttpHandler next,
            String cookiePath,
            OAuth2CookieFilter cookieAuthFilter,
            OAuth2StateSerde oauth2StateSerde,
            CookieManager cookieManager) {
        this.next = next;
        this.cookiePath = cookiePath;
        this.auth = cookieAuthFilter;
        this.oauth2StateSerde = oauth2StateSerde;
        this.cookieManager = cookieManager;
    }

    public static HttpHandler of(
            HttpHandler next,
            OAuth2ClientBlocking client,
            Supplier<OAuth2Configuration> config) {
        return of(next, "", client, config, new OAuth2StateSerdeImpl());
    }

    public static HttpHandler of(
            HttpHandler next,
            String cookiePath,
            OAuth2ClientBlocking client,
            Supplier<OAuth2Configuration> config,
            OAuth2StateSerde oauth2StateSerde) {
        return of(
                next,
                cookiePath,
                new OAuth2CookieFilter(OAuth2Client.of(client), config),
                oauth2StateSerde,
                CookieManagerImpl.INSTANCE);
    }

    @VisibleForTesting
    static HttpHandler of(
            HttpHandler next,
            String cookiePath,
            OAuth2CookieFilter cookieAuthFilter,
            OAuth2StateSerde oauth2StateSerde,
            CookieManager cookieManager) {
        // Must run on the task pool, authentication client blocks until completion.
        return new BlockingHandler(
                new OAuth2CookieHandler(
                        next, cookiePath, cookieAuthFilter, oauth2StateSerde, cookieManager));
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String requestPath = exchange.getRequestPath().substring(cookiePath.length());

        Optional<String> token = cookieManager.getTokenCookie(exchange);
        if (auth.shouldDoOauth2Flow(requestPath, token)) {
            String encodedState =
                    oauth2StateSerde.encodeRedirectUrlToState(getUriWithQueryParameters(exchange));
            cookieManager.setStateCookie(exchange, cookiePath, encodedState);

            Optional<String> host = PalantirHeaders.getExternalHostHeader(exchange);
            String authorizeRedirectUri = auth.getAuthorizeRedirectUri(host, encodedState);

            exchange.setStatusCode(StatusCodes.TEMPORARY_REDIRECT);
            exchange.getResponseHeaders().put(Headers.LOCATION, authorizeRedirectUri);

            // do not continue to process the handler chain
            return;
        }
        next.handleRequest(exchange);
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
