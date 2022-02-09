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

import com.markelliot.barista.Endpoints;
import com.markelliot.barista.SerDe;
import com.markelliot.barista.authz.AuthToken;
import com.markelliot.barista.authz.AuthTokens;
import com.markelliot.barista.authz.Authz;
import com.markelliot.barista.authz.VerifiedAuthToken;
import com.markelliot.barista.endpoints.EndpointHandler;
import com.markelliot.barista.endpoints.EndpointRuntime;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/** A builder to make an {@link HttpHandler} for specified {@link Endpoints}. */
public final class EndpointHandlerBuilder {
    private final SerDe serde;
    private final Authz authz;

    public EndpointHandlerBuilder(SerDe serde, Authz authz) {
        this.serde = serde;
        this.authz = authz;
    }

    public HttpHandler build(
            Set<Endpoints.VerifiedAuth<?, ?>> authEndpoints,
            Set<Endpoints.Open<?, ?>> openEndpoints,
            Set<EndpointHandler> endpointHandlers) {
        EndpointRuntime runtime = new EndpointRuntime(serde, authz);
        RoutingHandler router = new RoutingHandler();
        // TODO(markelliot): remove authEndpoints and openEndpoints in a future version
        authEndpoints.forEach(e -> router.add(e.method().method(), e.path(), authEndpoint(e)));
        openEndpoints.forEach(e -> router.add(e.method().method(), e.path(), openEndpoint(e)));
        endpointHandlers.forEach(
                e -> router.add(e.method().method(), e.route(), e.handler(runtime)));
        router.setFallbackHandler(
                exchange ->
                        exchange.setStatusCode(404)
                                .getResponseSender()
                                .send("Unknown API Endpoint"));
        return router;
    }

    private <Request, Response> HttpHandler authEndpoint(
            Endpoints.VerifiedAuth<Request, Response> endpoint) {
        return switch (endpoint.method()) {
            case GET -> exchange -> authNoBody(endpoint, exchange);
            case PUT, POST -> withBody((exchange, body) -> authWithBody(endpoint, exchange, body));
        };
    }

    public <Request, Response> HttpHandler openEndpoint(
            Endpoints.Open<Request, Response> endpoint) {
        return switch (endpoint.method()) {
            case GET -> exchange -> openNoBody(endpoint, exchange);
            case PUT, POST -> withBody((exchange, body) -> openWithBody(endpoint, exchange, body));
        };
    }

    /** Returns an {@link HttpHandler} with body content. */
    private HttpHandler withBody(BiConsumer<HttpServerExchange, SerDe.ByteRepr> fn) {
        return exchange ->
                exchange.getRequestReceiver()
                        .receiveFullString((ex, str) -> fn.accept(ex, new SerDe.ByteRepr(str)));
    }

    private <Request, Response> void openNoBody(
            Endpoints.Open<Request, Response> endpoint, HttpServerExchange exchange) {
        try {
            Response responseObj = endpoint.call(null);
            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, serde.contentType());
            exchange.getResponseSender().send(serde.serialize(responseObj).raw());
        } catch (Exception e) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("Server Error");
        }
    }

    private <Request, Response> void openWithBody(
            Endpoints.Open<Request, Response> endpoint,
            HttpServerExchange exchange,
            SerDe.ByteRepr body) {
        try {
            Request requestObj = serde.deserialize(body, endpoint.requestClass());
            Response responseObj = endpoint.call(requestObj);
            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, serde.contentType());
            exchange.getResponseSender().send(serde.serialize(responseObj).raw());
        } catch (Exception e) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("Server Error");
        }
    }

    private <Request, Response> void authNoBody(
            Endpoints.VerifiedAuth<Request, Response> endpoint, HttpServerExchange exchange) {
        HeaderValues authzHeader = exchange.getRequestHeaders().get(Headers.AUTHORIZATION);
        if (authzHeader.size() != 1) {
            exchange.setStatusCode(401);
            exchange.getResponseSender().send("Unauthorized: Missing authorization authToken");
            return;
        }

        AuthToken authToken = AuthTokens.fromAuthorizationHeader(authzHeader.getFirst());
        Optional<VerifiedAuthToken> verifiedAuthToken = authz.check(authToken);

        if (verifiedAuthToken.isEmpty()) {
            exchange.setStatusCode(403);
            exchange.getResponseSender()
                    .send("Forbidden: Invalid or expired authorization authToken");
            return;
        }

        try {
            Response responseObj = endpoint.call(verifiedAuthToken.get(), null);
            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, serde.contentType());
            exchange.getResponseSender().send(serde.serialize(responseObj).raw());
        } catch (Exception e) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("Server Error");
        }
    }

    private <Request, Response> void authWithBody(
            Endpoints.VerifiedAuth<Request, Response> endpoint,
            HttpServerExchange exchange,
            SerDe.ByteRepr body) {
        HeaderValues authzHeader = exchange.getRequestHeaders().get(Headers.AUTHORIZATION);
        if (authzHeader.size() != 1) {
            exchange.setStatusCode(401);
            exchange.getResponseSender().send("Unauthorized: Missing authorization authToken");
            return;
        }

        AuthToken authToken = AuthTokens.fromAuthorizationHeader(authzHeader.getFirst());
        Optional<VerifiedAuthToken> verifiedAuthToken = authz.check(authToken);

        if (verifiedAuthToken.isEmpty()) {
            exchange.setStatusCode(403);
            exchange.getResponseSender()
                    .send("Forbidden: Invalid or expired authorization authToken");
            return;
        }

        try {
            Request requestObj = serde.deserialize(body, endpoint.requestClass());
            Response responseObj = endpoint.call(verifiedAuthToken.get(), requestObj);
            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, serde.contentType());
            exchange.getResponseSender().send(serde.serialize(responseObj).raw());
        } catch (Exception e) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("Server Error");
        }
    }
}
