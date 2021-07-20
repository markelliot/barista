package barista;

import barista.authz.Authz;
import barista.handlers.CorsHandler;
import barista.handlers.DispatchFromIoThreadHandler;
import barista.handlers.EndpointHandlerBuilder;
import barista.handlers.HandlerChain;
import barista.tls.TransportLayerSecurity;
import com.google.common.base.Preconditions;
import io.undertow.Undertow;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Server {
    private final Undertow undertow;

    private Server(Undertow undertow) {
        this.undertow = undertow;

        Runtime.getRuntime().addShutdownHook(new Thread(undertow::stop));
    }

    private void start() {
        undertow.start();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int port = 8443;
        private Set<Endpoints.Open<?, ?>> openEndpoints = new LinkedHashSet<>();
        private Set<Endpoints.VerifiedAuth<?, ?>> authEndpoints = new LinkedHashSet<>();
        private Set<String> allowedOrigins = new LinkedHashSet<>();
        private SerDe serde = new SerDe.ObjectMapperSerDe();
        private Authz authz = null;
        private boolean tls = true;

        private Builder() {}

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public <Request, Response> Builder endpoint(Endpoints.Open<Request, Response> endpoint) {
            openEndpoints.add(endpoint);
            return this;
        }

        public <Request, Response> Builder endpoint(
                Endpoints.VerifiedAuth<Request, Response> endpoint) {
            authEndpoints.add(endpoint);
            return this;
        }

        public Builder allowOrigin(String origin) {
            allowedOrigins.add(origin);
            return this;
        }

        public Builder serde(SerDe serde) {
            this.serde = serde;
            return this;
        }

        public Builder authz(Authz authz) {
            this.authz = authz;
            return this;
        }

        public Builder disableTls() {
            this.tls = false;
            return this;
        }

        public Server start() {
            Preconditions.checkNotNull(authz);

            EndpointHandlerBuilder handler = new EndpointHandlerBuilder(serde, authz);
            Undertow.Builder builder =
                    Undertow.builder()
                            .setHandler(
                                    HandlerChain.of(DispatchFromIoThreadHandler::new)
                                            .then(h -> new CorsHandler(allowedOrigins, h))
                                            .last(handler.build(authEndpoints, openEndpoints)));
            if (tls) {
                builder.addHttpsListener(
                        port,
                        "0.0.0.0",
                        TransportLayerSecurity.createSslContext(Paths.get("var", "security")));
            } else {
                builder.addHttpListener(port, "0.0.0.0");
            }
            Server server = new Server(builder.build());
            server.start();
            return server;
        }
    }
}
