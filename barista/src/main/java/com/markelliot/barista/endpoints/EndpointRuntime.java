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

import com.google.common.net.MediaType;
import com.markelliot.barista.Bytes;
import com.markelliot.barista.SerDe;
import com.markelliot.barista.authz.AuthToken;
import com.markelliot.barista.authz.AuthTokens;
import com.markelliot.barista.authz.Authz;
import com.markelliot.barista.authz.VerifiedAuthToken;
import com.markelliot.barista.serde.DispatchingSerDe;
import com.markelliot.result.Result;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

public final class EndpointRuntime<E> {
    private final SerDe<E> serde;
    private final Authz authz;

    private EndpointRuntime(SerDe<E> serde, Authz authz) {
        this.serde = serde;
        this.authz = authz;
    }

    private SerDe.MimeTypeSerDe<E> requestSerDe(HttpServerExchange exchange) {
        HeaderValues headers = exchange.getRequestHeaders().get(Headers.CONTENT_TYPE);
        if (headers == null || headers.isEmpty()) {
            return serde.select(MediaType.ANY_TYPE);
        }
        return serde.select(MediaType.parse(headers.getFirst()));
    }

    private SerDe.MimeTypeSerDe<E> responseSerDe(HttpServerExchange exchange) {
        HeaderValues headers = exchange.getRequestHeaders().get(Headers.ACCEPT);
        if (headers != null) {
            for (String accept : headers) {
                SerDe.MimeTypeSerDe<E> candidate = serde.select(MediaType.parse(accept));
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return serde.select(MediaType.ANY_TYPE);
    }

    public <T> Result<T, E> readBodyAsResult(HttpServerExchange exchange, byte[] body, Class<T> aClass) {
        return requestSerDe(exchange).deserialize(Bytes.from(body), aClass);
    }

    public <T> T readBody(HttpServerExchange exchange, byte[] body, Class<T> aClass) {
        Result<T, E> result = readBodyAsResult(exchange, body, aClass);
        return result.orElseThrow(err -> {
            if (err instanceof RuntimeException re) {
                return re;
            } else if (err instanceof Exception e) {
                return new RuntimeException(e);
            }
            return new RuntimeException(String.valueOf(err));
        });
    }

    public Result<VerifiedAuthToken, HttpError> verifyAuth(HttpServerExchange exchange) {
        HeaderValues authzHeader = exchange.getRequestHeaders().get(Headers.AUTHORIZATION);
        if (authzHeader == null || authzHeader.size() != 1) {
            return HttpError.unauthenticated("Unauthorized: Missing authorization authToken");
        }

        AuthToken authToken = AuthTokens.fromAuthorizationHeader(authzHeader.getFirst());
        return authz.check(authToken)
                .map(Result::<VerifiedAuthToken, HttpError>ok)
                .orElseGet(() -> HttpError.unauthorized("Unauthorized: Invalid authorization authToken"));
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
        SerDe.MimeTypeSerDe<E> responseSerDe = responseSerDe(exchange);
        exchange.setStatusCode(error.statusCode());
        exchange.getResponseHeaders()
                .add(Headers.CONTENT_TYPE, responseSerDe.mimeType().toString());
        exchange.getResponseSender()
                .send(responseSerDe
                        .serialize(new ServerError(UUID.randomUUID().toString(), error.message()))
                        .unwrap()
                        .asReadOnlyByteBuffer());
    }

    private void writeBody(Object body, HttpServerExchange exchange) {
        SerDe.MimeTypeSerDe<E> responseSerDe = responseSerDe(exchange);
        exchange.getResponseHeaders()
                .add(Headers.CONTENT_TYPE, responseSerDe.mimeType().toString());
        exchange.getResponseSender().send(responseSerDe.serialize(body).unwrap().asReadOnlyByteBuffer());
    }

    private void writeEmpty(HttpServerExchange exchange) {
        SerDe.MimeTypeSerDe<E> responseSerDe = responseSerDe(exchange);
        exchange.setStatusCode(201);
        exchange.getResponseHeaders()
                .add(Headers.CONTENT_TYPE, responseSerDe.mimeType().toString());
    }

    private void writeError(Exception exception, HttpServerExchange exchange) {
        writeError(new ServerError(UUID.randomUUID().toString(), exception.getMessage()), exchange);
    }

    private void writeError(ServerError error, HttpServerExchange exchange) {
        SerDe.MimeTypeSerDe<E> responseSerDe = responseSerDe(exchange);
        exchange.setStatusCode(500);
        exchange.getResponseHeaders()
                .add(Headers.CONTENT_TYPE, responseSerDe.mimeType().toString());
        exchange.getResponseSender()
                .send(responseSerDe.serialize(error).unwrap().asReadOnlyByteBuffer());
    }

    private static void redirect(HttpRedirect redirect, HttpServerExchange exchange) {
        exchange.setStatusCode(redirect.type().statusCode());
        exchange.getResponseHeaders().add(Headers.LOCATION, redirect.location().toString());
    }

    public static Optional<String> pathParameter(String parameter, HttpServerExchange exchange) {
        Map<String, String> pathParams =
                exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY).getParameters();
        return Optional.ofNullable(pathParams.get(parameter));
    }

    public static Optional<String> headerParameter(String parameter, HttpServerExchange exchange) {
        return Optional.ofNullable(exchange.getRequestHeaders().getFirst(parameter));
    }

    public static Optional<String> cookieParameter(String parameter, HttpServerExchange exchange) {
        return Optional.ofNullable(exchange.getRequestCookie(parameter).getValue());
    }

    public static Optional<String> queryParameter(String parameter, HttpServerExchange exchange) {
        return Optional.ofNullable(exchange.getQueryParameters().get(parameter)).map(Deque::getFirst);
    }

    record ServerError(String errorId, String message) {}

    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    public static EndpointRuntime<Exception> createDefault() {
        return EndpointRuntime.<Exception>builder()
                .serde(DispatchingSerDe.createDefault())
                .build();
    }

    public static final class Builder<E> {
        private SerDe<E> serde;
        private Authz authz = Authz.denyAll();

        private Builder() {}

        public Builder<E> serde(SerDe<E> serde) {
            this.serde = serde;
            return this;
        }

        public Builder<E> authz(Authz authz) {
            this.authz = authz;
            return this;
        }

        public EndpointRuntime<E> build() {
            Objects.requireNonNull(serde);
            Objects.requireNonNull(authz);
            return new EndpointRuntime<>(serde, authz);
        }
    }
}
