/*
 * (c) Copyright 2025 Mark Elliot. All rights reserved.
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

package com.markelliot.barista.prometheus;

import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class PrometheusHandler implements HttpHandler {
    private final PrometheusRegistry registry;

    public PrometheusHandler(PrometheusRegistry registry) {
        this.registry = registry;
    }

    public void handleRequest(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, PrometheusTextFormatWriter.CONTENT_TYPE);
        exchange.getResponseSender().send(toExpositionFormat(registry.scrape()));
    }

    private static String toExpositionFormat(MetricSnapshots snapshots) {
        var writer = PrometheusTextFormatWriter.builder()
                .setIncludeCreatedTimestamps(true)
                .build();
        var outputStream = new ByteArrayOutputStream();
        try {
            writer.write(outputStream, snapshots);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }
}
