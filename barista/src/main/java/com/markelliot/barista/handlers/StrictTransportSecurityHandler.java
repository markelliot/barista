/*
 * (c) Copyright 2023 Mark Elliot. All rights reserved.
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

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

public class StrictTransportSecurityHandler implements HttpHandler {

    private static final HttpString STRICT_TRANSPORT_SECURITY = new HttpString("Strict-Transport-Security");
    private static final String ONE_DAY_IN_SECONDS = "86400";

    private final HttpHandler delegate;

    public StrictTransportSecurityHandler(HttpHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        delegate.handleRequest(exchange);
        if (!exchange.getResponseHeaders().contains(STRICT_TRANSPORT_SECURITY)) {
            exchange.getResponseHeaders().add(STRICT_TRANSPORT_SECURITY, "max-age=" + ONE_DAY_IN_SECONDS);
        }
    }
}
