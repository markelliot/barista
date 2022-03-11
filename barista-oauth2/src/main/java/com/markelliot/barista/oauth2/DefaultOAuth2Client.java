package com.markelliot.barista.oauth2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.markelliot.barista.SerDe;
import com.markelliot.barista.oauth2.objects.CreateTokenRequest;
import com.markelliot.barista.oauth2.objects.OAuth2Credentials;
import com.palantir.tokens.auth.AuthHeader;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;

public final class DefaultOAuth2Client implements OAuth2Client {
    static final String CREATE_TOKEN_PATH = "/oauth2/token";
    static final String CHECK_TOKEN_PATH = "/oauth2/check_token";
    private static final SerDe responseSerDe = new SerDe.ObjectMapperSerDe();

    private final String baseUri;
    private final HttpClient httpClient;

    @VisibleForTesting
    DefaultOAuth2Client(String baseUri, HttpClient httpClient) {
        this.baseUri = StringUtils.removeEnd(baseUri, "/");
        this.httpClient = httpClient;
    }

    public static OAuth2Client create(String baseUri, SSLContext sslContext) {
        return new DefaultOAuth2Client(baseUri, HttpClient.newBuilder().sslContext(sslContext).build());
    }

    @Override
    public OAuth2Credentials createToken(CreateTokenRequest createTokenRequest) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + CREATE_TOKEN_PATH))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.ACCEPT, responseSerDe.contentType())
                // request must be form encoded
                .POST(formEncode(createTokenRequest))
                .build();
        return responseSerDe.deserialize(
                new SerDe.ByteRepr(call(request, HttpResponse.BodyHandlers.ofString())), OAuth2Credentials.class);
    }

    @Override
    public void checkToken(AuthHeader authHeader) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + CHECK_TOKEN_PATH))
                .header(HttpHeaders.AUTHORIZATION, authHeader.toString())
                .GET()
                .build();
        call(request, HttpResponse.BodyHandlers.discarding());
    }

    private <T> T call(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
        try {
            HttpResponse<T> response = this.httpClient.send(request, bodyHandler);
            if (300 <= response.statusCode() && response.statusCode() <= 599) {
                throw new RuntimeException("error in http request, status code: " + response.statusCode()
                        + ", body: " + response.body());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpRequest.BodyPublisher formEncode(CreateTokenRequest createTokenRequest) {
        return HttpRequest.BodyPublishers.ofString(formEncode(ImmutableMap.<String, String>builder()
                        .put("grant_type", createTokenRequest.grantType())
                        .put("code", createTokenRequest.authorizationCode())
                        .put("redirect_uri", createTokenRequest.callbackUrl())
                        .put("client_id", createTokenRequest.authorization().clientId())
                        .put("client_secret", createTokenRequest.authorization().clientSecret())
                        .build()),
                StandardCharsets.UTF_8);
    }

    @VisibleForTesting
    static String formEncode(Map<String, String> parameters) {
        return parameters.entrySet()
                .stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
