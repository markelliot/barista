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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.palantir.tokens.auth.BearerToken;
import io.undertow.server.HttpServerExchange;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AuthRedirectResourceTest {

    private static final String CODE = "code";
    private static final BearerToken TOKEN =
            BearerToken.valueOf(
                    "eyJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJZbFRLU0cxU1QxVzJDdlEw"
                            + "UmJCMUN3PT0iLCJqdGkiOiJRbzlHMVBlTVNuK1BKTjZ5QlJXUEh3PT0ifQ.-EH6N7TmgTWFPdi4CFmJX7TC1G3dB8PlrWPm3FQUaamTR"
                            + "BuVHgbcvuNwnegZ8iXmOkC3kIuFmCJ7l03DoesuoQ");
    private static final int EXPIRES_IN = 3600;
    private static final String COOKIE_PATH = "/cookie-path";
    private static final OAuth2Configuration CONFIG =
            OAuth2Configuration.builder()
                    .clientId("clientId")
                    .clientSecret("clientSecret")
                    .externalUri("https://authorizeuri.biz")
                    .redirectUri("https://clientapp.biz/cookie-path/redirect")
                    .build();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private OAuth2ClientBlocking oauth2Client;

    @Mock private CookieManager cookieManager;

    @Mock private UriInfo uriInfo;

    @Mock private ContainerRequestContext containerRequestContext;

    @Mock private Supplier<HttpServerExchange> exchangeSupplier;

    private AuthRedirectResource authRedirectResource;

    private UUID uuidForTesting;
    private OAuth2StateSerde oAuth2StateSerde;
    private String redirectBackToWithHost;
    private String redirectBackToWithoutHost;
    private String stateWithHost;
    private String stateWithoutHost;

    @Before
    public void before() {
        uuidForTesting = UUID.randomUUID();
        oAuth2StateSerde = new OAuth2StateSerdeImpl(() -> this.uuidForTesting);
        redirectBackToWithHost = "https://clientapp.biz/cookie-path/place";
        redirectBackToWithoutHost = "/cookie-path/place";
        stateWithHost =
                oAuth2StateSerde.encodeRedirectUrlToState(URI.create(redirectBackToWithHost));
        stateWithoutHost =
                oAuth2StateSerde.encodeRedirectUrlToState(URI.create(redirectBackToWithoutHost));
        authRedirectResource =
                new AuthRedirectResource(
                        Optional.of(COOKIE_PATH),
                        OAuth2Client.of(oauth2Client),
                        () -> CONFIG,
                        oAuth2StateSerde,
                        cookieManager,
                        exchangeSupplier);
    }

    @Test
    public void badState_fullyQualifiedRedirect() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        when(exchangeSupplier.get()).thenReturn(exchange);
        when(cookieManager.hasStateCookie(exchange, stateWithHost)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                authRedirectResource.handle(
                                        "", stateWithHost, CODE, uriInfo, containerRequestContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Login state is invalid, try logging in again");

        verify(cookieManager).hasStateCookie(exchange, stateWithHost);
        verifyNoInteractions(oauth2Client);
        verifyNoMoreInteractions(cookieManager);
    }

    @Test
    public void badState_fullyQualifiedRedirectToThisHost() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        when(exchangeSupplier.get()).thenReturn(exchange);
        when(cookieManager.hasStateCookie(exchange, stateWithHost)).thenReturn(false);
        when(uriInfo.getRequestUri()).thenReturn(URI.create(CONFIG.redirectUri()));

        Response response =
                authRedirectResource.handle(
                        "", stateWithHost, CODE, uriInfo, containerRequestContext);

        assertThat(response.getHeaderString(HttpHeaders.LOCATION))
                .isEqualTo(
                        OAuthRedirects.getAuthorizeRedirectUri(
                                Optional.empty(), stateWithHost, CONFIG));
        verify(cookieManager).setStateCookie(exchange, COOKIE_PATH, stateWithHost);
        verifyNoInteractions(oauth2Client);
    }

    @Test
    public void badState_redirectDoesNotIncludeHost() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        when(exchangeSupplier.get()).thenReturn(exchange);
        when(cookieManager.hasStateCookie(exchange, stateWithoutHost)).thenReturn(false);

        Response response =
                authRedirectResource.handle(
                        "", stateWithoutHost, CODE, uriInfo, containerRequestContext);

        assertThat(response.getHeaderString(HttpHeaders.LOCATION))
                .isEqualTo(
                        OAuthRedirects.getAuthorizeRedirectUri(
                                Optional.empty(), stateWithoutHost, CONFIG));
        verify(cookieManager).setStateCookie(exchange, COOKIE_PATH, stateWithoutHost);
        verifyNoInteractions(oauth2Client);
    }

    @Test
    public void goodState_redirectWithHost() throws URISyntaxException {
        HttpServerExchange exchange = new HttpServerExchange(null);
        expectCreateTokenCall();
        when(exchangeSupplier.get()).thenReturn(exchange);
        when(cookieManager.hasStateCookie(exchange, stateWithHost)).thenReturn(true);

        Response response =
                authRedirectResource.handle(
                        "", stateWithHost, CODE, uriInfo, containerRequestContext);
        assertThat(response.getLocation()).isEqualTo(new URI(redirectBackToWithHost));
        verify(cookieManager).setTokenCookie(exchange, COOKIE_PATH, TOKEN, EXPIRES_IN);
        verify(cookieManager).deleteStateCookie(exchange, COOKIE_PATH, stateWithHost);
    }

    @Test
    public void goodState_redirectWithoutHost() throws URISyntaxException {
        HttpServerExchange exchange = new HttpServerExchange(null);
        expectCreateTokenCall();
        when(exchangeSupplier.get()).thenReturn(exchange);
        when(cookieManager.hasStateCookie(exchange, stateWithoutHost)).thenReturn(true);

        Response response =
                authRedirectResource.handle(
                        "", stateWithoutHost, CODE, uriInfo, containerRequestContext);
        assertThat(response.getLocation()).isEqualTo(new URI(redirectBackToWithoutHost));
        verify(cookieManager).setTokenCookie(exchange, COOKIE_PATH, TOKEN, EXPIRES_IN);
        verify(cookieManager).deleteStateCookie(exchange, COOKIE_PATH, stateWithoutHost);
    }

    private void expectCreateTokenCall() {
        when(oauth2Client.createToken(
                        CreateTokenRequest.builder()
                                .grantType("authorization_code")
                                .authorizationCode(CODE)
                                .callbackUrl(CONFIG.redirectUri().toString())
                                .authorization(CONFIG.oauth2Authorization())
                                .build()))
                .thenReturn(
                        new OAuth2Credentials.Builder()
                                .bearerToken(TOKEN)
                                .expiresIn(EXPIRES_IN)
                                .build());
    }
}
