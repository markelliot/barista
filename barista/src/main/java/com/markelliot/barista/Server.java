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

package com.markelliot.barista;

import com.google.common.base.Preconditions;
import com.markelliot.barista.authz.Authz;
import com.markelliot.barista.endpoints.EndpointHandler;
import com.markelliot.barista.endpoints.Endpoints;
import com.markelliot.barista.handlers.CorsHandler;
import com.markelliot.barista.handlers.DispatchFromIoThreadHandler;
import com.markelliot.barista.handlers.EndpointHandlerBuilder;
import com.markelliot.barista.handlers.HandlerChain;
import com.markelliot.barista.handlers.TracingHandler;
import com.markelliot.barista.tls.TransportLayerSecurity;
import com.markelliot.barista.tracing.Spans;
import io.undertow.Undertow;
import io.undertow.Undertow.ListenerBuilder;
import io.undertow.Undertow.ListenerType;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.net.ssl.SSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofMinutes(1);

    private final GracefulShutdownHandler shutdownHandler;
    private final Undertow undertow;

    private Server(GracefulShutdownHandler shutdownHandler, Undertow undertow) {
        this.shutdownHandler = shutdownHandler;
        this.undertow = undertow;

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    private void start() {
        undertow.start();
    }

    /**
     * Manually stop the server.
     *
     * <p>It's uncommon to invoke this method because the expected use of this framework is for the
     * server to run for the lifetime of the process, and a shutdown hook to stop the server is
     * included automatically.
     */
    public void stop() {
        shutdownHandler.shutdown();
        shutdownHandler.addShutdownListener(shutdownSuccessful -> undertow.stop());
        try {
            if (!shutdownHandler.awaitShutdown(SHUTDOWN_TIMEOUT.toMillis())) {
                undertow.stop();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int port = 8443;
        private final Set<EndpointHandler> endpointHandlers = new LinkedHashSet<>();
        private final Set<String> allowedOrigins = new LinkedHashSet<>();
        private SerDe serde = new SerDe.ObjectMapperSerDe();
        private Authz authz = Authz.denyAll();
        private boolean allowAllOrigins = false;
        private boolean tls = true;
        private Optional<Consumer<Request>> fallbackHandler = Optional.empty();
        private Optional<SSLContext> sslContext = Optional.empty();
        private double tracingRate = 0.2;
        private boolean enableTraceLogging = true;

        private Builder() {}

        public Builder port(int port) {
            Preconditions.checkArgument(0 < port && port < 65536, "Port must be in range [1, 65535]");
            this.port = port;
            return this;
        }

        public Builder fallback(Consumer<Request> fallback) {
            this.fallbackHandler = Optional.of(fallback);
            return this;
        }

        public Builder endpoints(Endpoints resource) {
            Objects.requireNonNull(resource);
            endpointHandlers.addAll(resource.endpoints());
            return this;
        }

        public Builder allowOrigin(String origin) {
            Objects.requireNonNull(origin);
            allowedOrigins.add(origin);
            return this;
        }

        public Builder allowAllOrigins() {
            allowAllOrigins = true;
            return this;
        }

        public Builder serde(SerDe serde) {
            Objects.requireNonNull(serde);
            this.serde = serde;
            return this;
        }

        public Builder authz(Authz authz) {
            Objects.requireNonNull(authz);
            this.authz = authz;
            return this;
        }

        public Builder disableTls() {
            this.tls = false;
            return this;
        }

        public Builder sslContext(SSLContext context) {
            Objects.requireNonNull(context);
            this.sslContext = Optional.of(context);
            return this;
        }

        /**
         * Sets the sample rate to run tracing for incoming requests without a traceId header.
         *
         * <p>A value of 1.0 means trace every request.
         *
         * <p>A value of 0.0 means trace no requests.
         */
        public Builder tracingRate(double rate) {
            if (rate < 0.0 || rate > 1.0) {
                throw new IllegalArgumentException("Cannot set a rate outside of range [0, 1]");
            }
            this.tracingRate = rate;
            return this;
        }

        public Builder disableTraceLogging() {
            this.enableTraceLogging = false;
            return this;
        }

        public Server start() {
            Preconditions.checkNotNull(authz);

            if (enableTraceLogging) {
                // TODO(markelliot): use a custom format, perhaps emit to a specific log file
                Logger tracing = LoggerFactory.getLogger("tracing");
                Spans.register("barista", span -> tracing.info("TRACING {}", span));
            }

            EndpointHandlerBuilder handler = new EndpointHandlerBuilder(fallbackHandler, serde, authz);
            HttpHandler handlerChain = HandlerChain.of(DispatchFromIoThreadHandler::new)
                    .then(h -> new CorsHandler(allowAllOrigins, allowedOrigins, h))
                    .then(h -> new TracingHandler(tracingRate, h))
                    .last(new EndpointHandlerBuilder(fallbackHandler, serde, authz).build(endpointHandlers));
            GracefulShutdownHandler shutdownHandler = new GracefulShutdownHandler(handlerChain);
            Undertow undertow = Undertow.builder()
                    .setHandler(new DispatchFromIoThreadHandler(shutdownHandler))
                    .addListener(listener())
                    .build();
            Server server = new Server(shutdownHandler, undertow);
            server.start();
            return server;
        }

        private ListenerBuilder listener() {
            ListenerBuilder lb = new ListenerBuilder().setPort(port).setHost("0.0.0.0");
            if (tls) {
                SSLContext context = sslContext.orElseGet(
                        () -> TransportLayerSecurity.createSslContext(Paths.get("var", "security")));
                lb.setType(ListenerType.HTTPS).setSslContext(context);
            } else {
                lb.setType(ListenerType.HTTP);
            }
            return lb;
        }
    }
}
