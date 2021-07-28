package barista.handlers;

import barista.tracing.Ids;
import barista.tracing.Span;
import barista.tracing.Trace;
import barista.tracing.Traces;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public record TracingHandler(double rate, HttpHandler delegate) implements HttpHandler {
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Trace trace = getTraceForRequest(exchange);
        Span span = getSpanForRequest(exchange, trace);
        exchange.addExchangeCompleteListener(
                (ignored, next) -> {
                    span.close();
                    next.proceed();
                });
        delegate.handleRequest(exchange);
    }

    private Trace getTraceForRequest(HttpServerExchange exchange) {
        String traceId = getId(exchange, "X-B3-TraceId");
        if (traceId != null) {
            return Traces.create(traceId, isSampled(exchange));
        }
        return Traces.create(Ids.randomId(), ThreadLocalRandom.current().nextDouble() < rate);
    }

    private static boolean isSampled(HttpServerExchange exchange) {
        String val = exchange.getRequestHeaders().getFirst("X-B3-Sampled");
        return val != null && (val.equals("1") || val.equalsIgnoreCase("true"));
    }

    private Span getSpanForRequest(HttpServerExchange exchange, Trace trace) {
        Supplier<String> opName = opName(exchange);
        String spanId = getId(exchange, "X-B3-SpanId");
        if (spanId != null) {
            return trace.withParent(spanId, opName);
        }
        return trace.rootSpan(opName);
    }

    private static Supplier<String> opName(HttpServerExchange exchange) {
        return () -> exchange.getRequestMethod() + " " + exchange.getRelativePath();
    }

    /** Gets the value of the named header the request and returns it if safe to handle. */
    private static String getId(HttpServerExchange exchange, String header) {
        String val = exchange.getRequestHeaders().getFirst(header);
        if (val != null && val.length() < 32) {
            return val;
        }
        return null;
    }
}
