package barista.handlers;

import com.google.common.base.Joiner;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import java.util.Set;

/** An {@link HttpHandler} that returns reasonable CORS allow headers for {@code allowedOrigins}. */
public record CorsHandler(Set<String> allowedOrigins, HttpHandler delegate) implements HttpHandler {
    private static final HttpString ACCESS_CONTROL_ALLOW_ORIGIN =
            new HttpString("Access-Control-Allow-Origin");
    private static final String ORIGIN_ALL = "*";

    private static final HttpString ACCESS_CONTROL_ALLOW_METHODS =
            new HttpString("Access-Control-Allow-Methods");
    private static final String ALLOWED_METHODS = "GET, PUT, POST, DELETE";

    private static final HttpString ACCESS_CONTROL_MAX_AGE =
            new HttpString("Access-Control-Max-Age");
    private static final String ONE_DAY_IN_SECONDS = "86400";

    private static final HttpString ACCESS_CONTROL_REQUEST_HEADERS =
            new HttpString("Access-Control-Request-Headers");
    private static final HttpString ACCESS_CONTROL_ALLOW_HEADERS =
            new HttpString("Access-Control-Allow-Headers");

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (allowedOrigins.contains(exchange.getRequestHeaders().getFirst(Headers.ORIGIN))) {
            exchange.getResponseHeaders()
                    .add(ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN_ALL)
                    .add(ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS)
                    .add(ACCESS_CONTROL_MAX_AGE, ONE_DAY_IN_SECONDS);

            if (exchange.getRequestHeaders().contains(ACCESS_CONTROL_REQUEST_HEADERS)) {
                String allowedHeaders =
                        Joiner.on(',')
                                .join(
                                        exchange.getRequestHeaders()
                                                .get(ACCESS_CONTROL_REQUEST_HEADERS));
                exchange.getResponseHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders);
            }
        }

        // swallow the request if it's an OPTIONS request
        if (!exchange.getRequestMethod().equals(Methods.OPTIONS)) {
            delegate.handleRequest(exchange);
        }
    }
}
