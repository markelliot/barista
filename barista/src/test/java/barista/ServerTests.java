package barista;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
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
                        .endpoint(new HelloWorldEndpoint())
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

    private static final class HelloWorldEndpoint implements Endpoints.Open<Void, String> {
        @Override
        public String call(Void unused) {
            return "Hello World";
        }

        @Override
        public Class<Void> requestClass() {
            return Void.class;
        }

        @Override
        public String path() {
            return "/hello-world";
        }

        @Override
        public HttpMethod method() {
            return HttpMethod.GET;
        }
    }
}
