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

package com.markelliot.barista.endpoints;

import com.markelliot.barista.SerDe;
import com.markelliot.barista.authz.AuthToken;
import com.markelliot.barista.authz.AuthTokens;
import com.markelliot.barista.authz.Authz;
import com.markelliot.barista.authz.VerifiedAuthToken;
import com.markelliot.result.Result;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

public final class EndpointRuntime {
    private final SerDe serde;
    private final Authz authz;

    public EndpointRuntime(SerDe serde, Authz authz) {
        this.serde = serde;
        this.authz = authz;
    }

    public SerDe serde() {
        return serde;
    }

    public Result<VerifiedAuthToken, HttpError> verifyAuth(HttpServerExchange exchange) {
        HeaderValues authzHeader = exchange.getRequestHeaders().get(Headers.AUTHORIZATION);
        if (authzHeader.size() != 1) {
            return HttpError.unauthenticated("Unauthorized: Missing authorization authToken");
        }

        AuthToken authToken = AuthTokens.fromAuthorizationHeader(authzHeader.getFirst());
        return authz.check(authToken)
                .map(Result::<VerifiedAuthToken, HttpError>ok)
                .orElseGet(
                        () ->
                                HttpError.unauthorized(
                                        "Unauthorized: Invalid authorization authToken"));
    }

    public void handle(Runnable runnable, HttpServerExchange exchange) {
        try {
            runnable.run();
        } catch (Exception e) {
            writeError(e, exchange);
            // TODO(markelliot): some smarter logging
            return;
        }

        writeEmpty(exchange);
    }

    public void handle(Callable<?> callable, HttpServerExchange exchange) {
        Object response;
        try {
            response = callable.call();
        } catch (Exception e) {
            writeError(e, exchange);
            // TODO(markelliot): some smarter logging
            return;
        }

        writeBody(response, exchange);
    }

    public void redirect(Callable<HttpRedirect> callable, HttpServerExchange exchange) {
        HttpRedirect redirect;
        try {
            redirect = callable.call();
        } catch (Exception e) {
            writeError(e, exchange);
            return;
        }

        redirect(redirect, exchange);
    }

    public void error(HttpError error, HttpServerExchange exchange) {
        exchange.setStatusCode(error.statusCode());
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, serde.contentType());
        exchange.getResponseSender()
                .send(
                        serde.serialize(
                                        new ServerError(
                                                UUID.randomUUID().toString(), error.message()))
                                .raw());
    }

    private void writeBody(Object body, HttpServerExchange exchange) {
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, serde.contentType());
        exchange.getResponseSender().send(serde.serialize(body).raw());
    }

    private void writeEmpty(HttpServerExchange exchange) {
        exchange.setStatusCode(201);
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, serde.contentType());
    }

    private void writeError(Exception exception, HttpServerExchange exchange) {
        writeError(new ServerError(UUID.randomUUID().toString(), exception.getMessage()), exchange);
    }

    private void writeError(ServerError error, HttpServerExchange exchange) {
        exchange.setStatusCode(500);
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, serde.contentType());
        exchange.getResponseSender().send(serde.serialize(error).raw());
    }

    private static void redirect(HttpRedirect redirect, HttpServerExchange exchange) {
        exchange.setStatusCode(redirect.type().statusCode());
        exchange.getResponseHeaders().add(Headers.LOCATION, redirect.location().toString());
    }

    public static Optional<String> pathParameter(String parameter, HttpServerExchange exchange) {
        return Optional.ofNullable(exchange.getPathParameters().get(parameter))
                .map(Deque::getFirst);
    }

    public static Optional<String> headerParameter(String parameter, HttpServerExchange exchange) {
        return Optional.ofNullable(exchange.getRequestHeaders().getFirst(parameter));
    }

    public static Optional<String> cookieParameter(String parameter, HttpServerExchange exchange) {
        return Optional.ofNullable(exchange.getRequestCookie(parameter).getValue());
    }

    public static Optional<String> queryParameter(String parameter, HttpServerExchange exchange) {
        return Optional.ofNullable(exchange.getQueryParameters().get(parameter))
                .map(Deque::getFirst);
    }

    record ServerError(String errorId, String message) {}
}
