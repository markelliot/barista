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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.markelliot.barista.oauth2.objects.CreateTokenRequest;
import com.markelliot.barista.oauth2.objects.OAuth2Authorization;
import com.markelliot.barista.oauth2.objects.OAuth2Credentials;
import com.palantir.conjure.java.api.errors.RemoteException;
import com.palantir.tokens.auth.AuthHeader;
import com.palantir.tokens.auth.BearerToken;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class OAuth2ClientTest {

    private static final BearerToken VALID_TOKEN = BearerToken.valueOf("valid");
    private static final AuthHeader VALID_AUTH_HEADER = AuthHeader.of(VALID_TOKEN);
    private static final int EXPIRES_IN = 3600;
    private static final String BASE_URI = "https://auth.example.com";

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private OAuth2Client client;

    @BeforeEach
    public void createClient() {
        client = new DefaultOAuth2Client(BASE_URI, httpClient);
    }

    @Test
    public void testFormEncoding() {
        assertThat(DefaultOAuth2Client.formEncode(
                ImmutableMap.of("token", "foo", "TOKEN2", "bar", "", "", "empty", "")))
                .isEqualTo("token=foo&TOKEN2=bar&=&empty=");
    }

    @Test
    public void testFormEncodingSpecialCharacters() {
        assertThat(DefaultOAuth2Client.formEncode(
                ImmutableMap.of("key!@#$%^&*()_+-= ", "value!@#$%^&*()_+-= ")))
                .isEqualTo(
                        "key%21%40%23%24%25%5E%26*%28%29_%2B-%3D+=value%21%40%23%24%25%5E%26*%28%29_%2B-%3D+");
    }

    @Test
    public void testCheckTokenSuccessful() {
        assertThatCode(() -> client.checkToken(VALID_AUTH_HEADER)).doesNotThrowAnyException();
    }

    @Test
    public void testCheckTokenInvalid() {
        assertThatThrownBy(() -> client.checkToken(AuthHeader.valueOf("invalid")))
                .isInstanceOfSatisfying(
                        RemoteException.class, re -> assertThat(re.getStatus()).isEqualTo(401));
    }

    @Test
    public void testCreateToken() throws IOException, InterruptedException {
        HttpRequest expectedRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URI + DefaultOAuth2Client.CREATE_TOKEN_PATH))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.ACCEPT, "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=a&code=b&redirect_uri=c&client_id=d&client_secret=e"))
                .build();
        when(httpClient.<String>send(eq(expectedRequest), any())).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\n" +
                "  \"access_token\":\"valid\",\n" +
                "  \"token_type\":\"Bearer\",\n" +
                "  \"expires_in\":3600,\n" +
                "  \"refresh_token\":\"grant_type=a&code=b&redirect_uri=c&client_id=d&client_secret=e\",\n" +
                "  \"scope\":\"ignored\"\n" +
                "}");

        OAuth2Credentials creds =
                client.createToken(
                        CreateTokenRequest.builder()
                                .grantType("a")
                                .authorizationCode("b")
                                .callbackUrl("c")
                                .authorization(
                                        OAuth2Authorization.builder()
                                                .clientId("d")
                                                .clientSecret("e")
                                                .build())
                                .build());
        assertThat(creds)
                .isEqualTo(
                        OAuth2Credentials.builder()
                                .bearerToken(VALID_TOKEN)
                                .refreshToken(
                                        "grant_type=a&code=b&redirect_uri=c&client_id=d&client_secret=e")
                                .expiresIn(EXPIRES_IN)
                                .build());
    }
}
