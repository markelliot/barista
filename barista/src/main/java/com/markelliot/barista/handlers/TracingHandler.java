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

import com.markelliot.barista.tracing.Ids;
import com.markelliot.barista.tracing.Span;
import com.markelliot.barista.tracing.Trace;
import com.markelliot.barista.tracing.Traces;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public record TracingHandler(double rate, HttpHandler delegate) implements HttpHandler {
    private static final HttpString TRACE_ID = HttpString.tryFromString("X-B3-TraceId");
    private static final HttpString SAMPLED = HttpString.tryFromString("X-B3-Sampled");
    private static final HttpString SPAN_ID = HttpString.tryFromString("X-B3-SpanId");

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Trace trace = getTraceForRequest(exchange);
        Span span = getSpanForRequest(exchange, trace);
        exchange.addExchangeCompleteListener((ignored, next) -> {
            span.close();
            next.proceed();
        });
        // Populate response before proceeding since later operations might commit the response.
        setExchangeState(exchange, span);
        delegate.handleRequest(exchange);
    }

    private Trace getTraceForRequest(HttpServerExchange exchange) {
        String traceId = getId(exchange, TRACE_ID);
        if (traceId != null) {
            return Traces.create(traceId, isSampled(exchange));
        }
        return Traces.create(Ids.randomId(), ThreadLocalRandom.current().nextDouble() < rate);
    }

    private static boolean isSampled(HttpServerExchange exchange) {
        String val = exchange.getRequestHeaders().getFirst(SAMPLED);
        return val != null && (val.equals("1") || val.equalsIgnoreCase("true"));
    }

    private Span getSpanForRequest(HttpServerExchange exchange, Trace trace) {
        Supplier<String> opName = opName(exchange);
        String spanId = getId(exchange, SPAN_ID);
        if (spanId != null) {
            return trace.withParent(spanId, opName);
        }
        return trace.rootSpan(opName);
    }

    private static Supplier<String> opName(HttpServerExchange exchange) {
        return () -> exchange.getRequestMethod() + " " + exchange.getRelativePath();
    }

    /** Gets the value of the named header the request and returns it if safe to handle. */
    private static String getId(HttpServerExchange exchange, HttpString header) {
        String val = exchange.getRequestHeaders().getFirst(header);
        if (val != null && val.length() <= 32) {
            return val;
        }
        return null;
    }

    private static void setExchangeState(HttpServerExchange exchange, Span span) {
        exchange.getResponseHeaders().put(TRACE_ID, span.traceId());
    }
}
