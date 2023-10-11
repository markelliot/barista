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

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.markelliot.barista.Request;
import com.markelliot.barista.SerDe;
import com.markelliot.barista.authz.Authz;
import com.markelliot.barista.endpoints.EndpointHandler;
import com.markelliot.barista.endpoints.EndpointRuntime;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

public final class EndpointHandlerBuilder {
    private final SerDe serde;
    private final Authz authz;
    private final Optional<Consumer<Request>> fallbackHandler;

    public EndpointHandlerBuilder(SerDe serde, Authz authz, Optional<Consumer<Request>> fallbackHandler) {
        this.fallbackHandler = fallbackHandler;
        this.serde = serde;
        this.authz = authz;
    }

    public HttpHandler build(Set<EndpointHandler> endpointHandlers) {
        EndpointRuntime runtime = new EndpointRuntime(serde, authz);
        RoutingHandler router = new RoutingHandler(false);
        endpointHandlers.forEach(e -> router.add(e.method().method(), e.route(), e.handler(runtime)));
        router.setFallbackHandler(exchange -> {
            fallbackHandler.ifPresent(requestConsumer -> requestConsumer.accept(toRequest(exchange)));
            exchange.setStatusCode(404).getResponseSender().send("Unknown API Endpoint");
        });
        return router;
    }

    private static Request toRequest(HttpServerExchange exchange) {
        ListMultimap<String, String> headers =
                Multimaps.newListMultimap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER), ArrayList::new);
        ListMultimap<String, String> queryParams =
                Multimaps.newListMultimap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER), ArrayList::new);

        exchange.getQueryParameters().forEach(queryParams::putAll);
        exchange.getRequestHeaders().forEach(headerValue -> {
            headers.putAll(headerValue.getHeaderName().toString(), headerValue);
        });

        return new Request(exchange.getRequestMethod(), exchange.getRequestPath(), headers, queryParams);
    }
}
