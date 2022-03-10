package com.markelliot.barista.oauth2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.markelliot.barista.oauth2.objects.CreateTokenRequest;
import com.markelliot.barista.oauth2.objects.OAuth2Credentials;
import com.palantir.tokens.auth.AuthHeader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.HttpHeaders;

public final class DefaultOAuth2Client implements OAuth2Client {

    private static final String CREATE_TOKEN_PATH = "/oauth2/token";
    private static final String CHECK_TOKEN_PATH = "/oauth2/check_token";

    @Override
    public OAuth2Credentials createToken(CreateTokenRequest createTokenRequest) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CREATE_TOKEN_PATH))
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .POST(serialize(createTokenRequest))
                .build();
        return null;
    }

    @Override
    public void checkToken(AuthHeader authHeader) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHECK_TOKEN_PATH))
                .header(HttpHeaders.AUTHORIZATION, authHeader.toString())
                .GET()
                .build();
    }

    private static HttpRequest.BodyPublisher serialize(CreateTokenRequest createTokenRequest) {
        return HttpRequest.BodyPublishers.ofString(formEncodedBody(ImmutableMap.<String, String>builder()
                        .put("grant_type", createTokenRequest.grantType())
                        .put("code", createTokenRequest.authorizationCode())
                        .put("redirect_uri", createTokenRequest.callbackUrl())
                        .put("client_id", createTokenRequest.authorization().clientId())
                        .put("client_secret", createTokenRequest.authorization().clientSecret())
                        .build()),
                StandardCharsets.UTF_8);
    }

    @VisibleForTesting
    static String formEncodedBody(Map<String, String> parameters) {
        return parameters.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
