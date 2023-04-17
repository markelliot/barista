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

package com.markelliot.barista.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.markelliot.barista.Server;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public final class TestFooServer {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static Server server;

    @BeforeAll
    static void beforeAll() {
        server = Server.builder()
                .allowOrigin("localhost:8181")
                .port(8080)
                .disableTls()
                .endpoints(new FooResourceEndpoints(new FooResource()))
                .start();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void smokeTest() throws IOException, InterruptedException {
        assertResponse("http://localhost:8080/foo/open/get", 200, "\"Hello, World!\"");
    }

    private void assertResponse(String uri, int statusCode, String expectedResponseText)
            throws IOException, InterruptedException {
        HttpResponse<String> helloWorldResult = CLIENT.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(uri))
                        .header("origin", "localhost:8181")
                        .GET()
                        .build(),
                BodyHandlers.ofString());
        assertThat(helloWorldResult.statusCode()).isEqualTo(statusCode);
        assertThat(helloWorldResult.body()).isEqualTo(expectedResponseText);
    }
}
