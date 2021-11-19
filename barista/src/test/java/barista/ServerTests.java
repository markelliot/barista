package barista;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.Test;

final class ServerTests {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @Test
    void smokeTest() throws IOException, InterruptedException {
        Server server =
                Server.builder()
                        .allowOrigin("localhost:8181")
                        .port(8080)
                        .disableTls()
                        .endpoint(new HelloWorldEndpoint())
                        .start();

        assertResponse("http://localhost:8080/hello-world", 200, "\"Hello World\"");
        assertResponse("http://localhost:8080/missing", 404, "Unknown API Endpoint");

        server.stop();
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
