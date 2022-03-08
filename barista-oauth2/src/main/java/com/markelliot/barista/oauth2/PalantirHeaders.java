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

package com.markelliot.barista.oauth2;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import java.util.Optional;

public final class PalantirHeaders {
    public static final String EXTERNAL_HOST_HEADER = "Palantir-External-Host";

    static Optional<String> getExternalHostHeader(HttpServerExchange exchange) {
        HeaderValues header =
                exchange.getRequestHeaders().get(PalantirHeaders.EXTERNAL_HOST_HEADER);
        if (header == null || header.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(header.getFirst());
    }

    private PalantirHeaders() {}
}