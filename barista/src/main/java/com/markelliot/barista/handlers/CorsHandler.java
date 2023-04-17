/*
 * (c) Copyright 2021 Mark Elliot. All rights reserved.
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

package com.markelliot.barista.handlers;

import com.google.common.base.Joiner;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import java.util.Set;
import java.util.function.Predicate;

/** An {@link HttpHandler} that returns reasonable CORS allow headers for {@code allowedOrigins}. */
public final class CorsHandler implements HttpHandler {
    private static final HttpString ACCESS_CONTROL_ALLOW_ORIGIN = new HttpString("Access-Control-Allow-Origin");
    private static final String ORIGIN_ALL = "*";

    private static final HttpString ACCESS_CONTROL_ALLOW_METHODS = new HttpString("Access-Control-Allow-Methods");
    private static final String ALLOWED_METHODS = "GET, PUT, POST, DELETE";

    private static final HttpString ACCESS_CONTROL_MAX_AGE = new HttpString("Access-Control-Max-Age");
    private static final String ONE_DAY_IN_SECONDS = "86400";

    private static final HttpString ACCESS_CONTROL_REQUEST_HEADERS = new HttpString("Access-Control-Request-Headers");
    private static final HttpString ACCESS_CONTROL_ALLOW_HEADERS = new HttpString("Access-Control-Allow-Headers");

    private final Set<String> allowedOrigins;
    private final HttpHandler delegate;
    private final Predicate<String> originCheck;

    public CorsHandler(boolean allowAllOrigins, Set<String> allowedOrigins, HttpHandler delegate) {
        this.originCheck = allowAllOrigins ? origin -> true : this::checkAllowedOrigin;
        this.allowedOrigins = allowedOrigins;
        this.delegate = delegate;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String origin = exchange.getRequestHeaders().getFirst(Headers.ORIGIN);
        if (!originCheck.test(origin)) {
            // not an allowed origin, hard deny
            exchange.setStatusCode(403).getResponseSender().send("Origin '" + origin + "' not allowed.");
            return;
        }

        exchange.getResponseHeaders()
                .add(ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN_ALL)
                .add(ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS)
                .add(ACCESS_CONTROL_MAX_AGE, ONE_DAY_IN_SECONDS);

        if (exchange.getRequestHeaders().contains(ACCESS_CONTROL_REQUEST_HEADERS)) {
            String allowedHeaders =
                    Joiner.on(',').join(exchange.getRequestHeaders().get(ACCESS_CONTROL_REQUEST_HEADERS));
            exchange.getResponseHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders);
        }

        // swallow the request if it's an OPTIONS request
        if (!exchange.getRequestMethod().equals(Methods.OPTIONS)) {
            delegate.handleRequest(exchange);
        }
    }

    /** Returns true if origin is null or if origin is in the allowedOrigins set. */
    private boolean checkAllowedOrigin(String origin) {
        return origin == null || allowedOrigins.contains(origin);
    }
}
