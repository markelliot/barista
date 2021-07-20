package barista;

import io.undertow.util.HttpString;
import io.undertow.util.Methods;

@SuppressWarnings("ImmutableEnumChecker")
public enum HttpMethod {
    GET(Methods.GET),
    PUT(Methods.PUT),
    POST(Methods.POST);

    private final HttpString method;

    HttpMethod(HttpString method) {
        this.method = method;
    }

    public HttpString method() {
        return method;
    }
}
