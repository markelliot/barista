package barista.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/** An {@link HttpHandler} that ensures work is done off of IO threads. */
public record DispatchFromIoThreadHandler(HttpHandler delegate) implements HttpHandler {
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        delegate.handleRequest(exchange);
    }
}
