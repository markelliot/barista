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

import com.markelliot.barista.SerDe;
import com.markelliot.barista.authz.Authz;
import com.markelliot.barista.endpoints.EndpointHandler;
import com.markelliot.barista.endpoints.EndpointRuntime;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import java.util.Set;

public final class EndpointHandlerBuilder {
    private final SerDe serde;
    private final Authz authz;

    public EndpointHandlerBuilder(SerDe serde, Authz authz) {
        this.serde = serde;
        this.authz = authz;
    }

    public HttpHandler build(Set<EndpointHandler> endpointHandlers) {
        EndpointRuntime runtime = new EndpointRuntime(serde, authz);
        RoutingHandler router = new RoutingHandler();
        endpointHandlers.forEach(
                e -> router.add(e.method().method(), e.route(), e.handler(runtime)));
        router.setFallbackHandler(
                exchange ->
                        exchange.setStatusCode(404)
                                .getResponseSender()
                                .send("Unknown API Endpoint"));
        return router;
    }
}
