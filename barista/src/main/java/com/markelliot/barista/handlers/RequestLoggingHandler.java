/*
 * (c) Copyright 2025 Mark Elliot. All rights reserved.
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

import com.google.common.base.Stopwatch;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import java.util.concurrent.TimeUnit;

public final class RequestLoggingHandler implements HttpHandler {
    public static final LogConsumer NO_OP_LOG_CONSUMER = new NoOpLogConsumer();
    public static final ErrorConsumer NO_OP_ERROR_CONSUMER = new NoOpErrorConsumer();

    private final LogConsumer logConsumer;
    private final ErrorConsumer errorConsumer;
    private final HttpHandler delegate;

    public RequestLoggingHandler(LogConsumer logConsumer, ErrorConsumer errorConsumer, HttpHandler delegate) {
        this.logConsumer = logConsumer;
        this.errorConsumer = errorConsumer;
        this.delegate = delegate;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Stopwatch timer = Stopwatch.createStarted();
        exchange.addResponseCommitListener(exc -> logConsumer.accept(
                exc.getRequestMethod().toString(),
                exc.getRequestPath(),
                exc.getStatusCode(),
                exc.getRequestContentLength(),
                exc.getResponseContentLength(),
                timer.elapsed(TimeUnit.MILLISECONDS)));
        try {
            delegate.handleRequest(exchange);
        } catch (Exception e) {
            errorConsumer.accept(exchange.getRequestMethod().toString(), exchange.getRequestPath(), e);
        }
    }

    public interface LogConsumer {
        void accept(
                String method, String path, int status, long requestSize, long responseSize, long responseTimeMillis);
    }

    public interface ErrorConsumer {
        void accept(String method, String path, Exception e);
    }

    private static final class NoOpLogConsumer implements LogConsumer {
        @Override
        public void accept(
                String method, String path, int status, long requestSize, long responseSize, long responseTimeMillis) {}
    }

    private static final class NoOpErrorConsumer implements ErrorConsumer {
        @Override
        public void accept(String method, String path, Exception e) {}
    }
}
