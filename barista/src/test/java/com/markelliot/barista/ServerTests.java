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

import static org.assertj.core.api.Assertions.assertThat;

import com.markelliot.barista.endpoints.EndpointHandler;
import com.markelliot.barista.endpoints.EndpointRuntime;
import io.undertow.server.HttpHandler;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ServerTests {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static Server server;

    @BeforeAll
    static void beforeAll() {
        server =
                Server.builder()
                        .allowOrigin("localhost:8181")
                        .port(8080)
                        .disableTls()
                        .endpoints(
                                () ->
                                        Set.of(
                                                new EndpointHandler() {
                                                    @Override
                                                    public HttpMethod method() {
                                                        return HttpMethod.GET;
                                                    }

                                                    @Override
                                                    public String route() {
                                                        return "/hello-world";
                                                    }

                                                    @Override
                                                    public HttpHandler handler(
                                                            EndpointRuntime runtime) {
                                                        return exchange ->
                                                                runtime.handle(
                                                                        () -> "Hello World",
                                                                        exchange);
                                                    }
                                                }))
                        .start();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void smokeTest() throws IOException, InterruptedException {
        assertResponse("http://localhost:8080/hello-world", 200, "\"Hello World\"");
        assertResponse("http://localhost:8080/missing", 404, "Unknown API Endpoint");
    }

    @Test
    void testCorsRejectsRequestsWithoutOrigin() throws IOException, InterruptedException {
        assertCorsFailure("http://localhost:8080/hello-world");
        assertCorsFailure("http://localhost:8080/missing");
    }

    private void assertResponse(String uri, int statusCode, String expectedResponseText)
            throws IOException, InterruptedException {
        HttpResponse<String> helloWorldResult =
                CLIENT.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(uri))
                                .header("origin", "localhost:8181")
                                .GET()
                                .build(),
                        BodyHandlers.ofString());
        assertThat(helloWorldResult.statusCode()).isEqualTo(statusCode);
        assertThat(helloWorldResult.body()).isEqualTo(expectedResponseText);
    }

    private void assertCorsFailure(String uri) throws IOException, InterruptedException {
        HttpResponse<String> helloWorldResult =
                CLIENT.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(uri))
                                .header("origin", "foo.com")
                                .GET()
                                .build(),
                        BodyHandlers.ofString());
        assertThat(helloWorldResult.statusCode()).isEqualTo(403);
        assertThat(helloWorldResult.body()).isEqualTo("Origin 'foo.com' not allowed.");
    }
}
