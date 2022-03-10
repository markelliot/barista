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

package com.markelliot.barista;

import com.markelliot.barista.endpoints.Endpoints;
import com.markelliot.barista.handlers.DelegatingHandler;
import io.undertow.server.HttpHandler;
import java.util.Optional;

/**
 * A Bundle encapsulates a global {@link HttpHandler} and related {@link Endpoints} to aid with
 * shipping Barista extensions. Genereally, {@code Bundle}s should be used strictly as a library
 * build target and not within an application. At the time of authoring, the only intended library
 * is an OAuth2 filter handler and redirect endpoint.
 */
public interface Bundle {
    /** A human-readable and safe-to-log name for this bundle. */
    String name();

    /**
     * An optional, global, {@link DelegatingHandler}. {@link DelegatingHandler}s will generally
     * perform work and then invoke the provided delegate. Some implementations may choose to
     * selectively invoke the delegate as a form of filtering.
     *
     * <p>To stack handlers, implementors might consider using a {@link
     * com.markelliot.barista.handlers.HandlerChain}.
     */
    Optional<DelegatingHandler> handler();

    /** Endpoints to install as part of this bundle. */
    Endpoints endpoints();
}
