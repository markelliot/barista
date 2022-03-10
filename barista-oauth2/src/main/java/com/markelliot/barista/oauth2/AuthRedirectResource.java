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

import com.google.common.annotations.VisibleForTesting;
import com.markelliot.barista.annotations.Http;
import com.markelliot.barista.annotations.Param;
import com.markelliot.barista.endpoints.HttpRedirect;
import com.markelliot.barista.oauth2.objects.CreateTokenRequest;
import com.markelliot.barista.oauth2.objects.OAuth2Configuration;
import com.markelliot.barista.oauth2.objects.OAuth2Credentials;
import io.undertow.server.HttpServerExchange;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuthRedirectResource {
    public static final String REDIRECT_RESOURCE_PATH = "/redirect";

    private static final Logger log = LoggerFactory.getLogger(AuthRedirectResource.class);

    private final OAuth2Client oauth2Client;
    private final Supplier<OAuth2Configuration> config;
    private final String cookiePath;
    private final OAuth2StateSerde oauth2StateSerde;
    private final CookieManager cookieManager;
    private final Supplier<HttpServerExchange> exchangeSupplier;

    /**
     * Preferred constructor.
     *
     * @param cookiePath cookie path, usually install.contextPath()
     * @param oauth2Client client
     * @param config config
     * @param oauth2StateSerde serde
     */
    public AuthRedirectResource(
            String cookiePath,
            OAuth2Client oauth2Client,
            Supplier<OAuth2Configuration> config,
            OAuth2StateSerde oauth2StateSerde) {
        this(
                cookiePath,
                oauth2Client,
                config,
                oauth2StateSerde,
                CookieManager.buildDefault(),
                ServletExchangeSupplier.INSTANCE);
    }

    @VisibleForTesting
    AuthRedirectResource(
            String cookiePath,
            OAuth2Client oauth2Client,
            Supplier<OAuth2Configuration> config,
            OAuth2StateSerde oauth2StateSerde,
            CookieManager cookieManager,
            Supplier<HttpServerExchange> exchangeSupplier) {
        this.cookiePath = cookiePath;
        this.oauth2Client = oauth2Client;
        this.config = config;
        this.oauth2StateSerde = oauth2StateSerde;
        this.cookieManager = cookieManager;
        this.exchangeSupplier = exchangeSupplier;
    }

    @Http.Get(REDIRECT_RESOURCE_PATH)
    public HttpRedirect handle(
            @Param.Query("error") Optional<String> error,
            @Param.Query("state") String urlState,
            @Param.Query("code") String code,
            @Param.Header(PalantirHeaders.EXTERNAL_HOST_HEADER) Optional<String> externalHostHeader) {
        if (error.isPresent()) {
            throw new WebApplicationException(
                    "An error occurred during login: " + error, Status.FORBIDDEN);
        }

        URI redirectUri =
                oauth2StateSerde
                        .decodeRedirectUrlFromState(urlState)
                        .orElseThrow(
                                () -> new BadRequestException("Unable to decode login response"));

        HttpServerExchange exchange = exchangeSupplier.get();
        if (!cookieManager.hasStateCookie(exchange, urlState)) {
            if (!isLocalRedirectUri(redirectUri)) {
                throw throwForBadCookieState();
            }
            return retryOauthAuthorizeResponse(exchange, externalHostHeader, redirectUri);
        }

        return successfulRedirectResponse(
                exchange, code, externalHostHeader, urlState, redirectUri);
    }

    private static ForbiddenException throwForBadCookieState() {
        log.warn("URL state parameter didn't match state from cookie.");
        throw new ForbiddenException("Login state is invalid, try logging in again");
    }

    private boolean isLocalRedirectUri(URI uri) {
        return uri.getScheme() == null
                && uri.getHost() == null
                && uri.getPort() == -1
                && uri.getPath() != null
                // Cookie path check helps us avoid traversing to other services
                // behind the same proxy. We may eventually want to validate
                // against '..' patterns which may interact poorly with some
                // reverse proxies for request smuggling, however for now
                // this should be sufficient -- the host/scheme/port check
                // is the most important piece.
                && uri.getPath().startsWith(cookiePath);
    }

    private OAuth2Credentials exchangeCode(String code, Optional<String> externalHostHeader) {
        OAuth2Configuration currentConfig = config.get();
        return oauth2Client.createToken(
                CreateTokenRequest.builder()
                        .grantType("authorization_code")
                        .authorizationCode(code)
                        .callbackUrl(
                                OAuthRedirects.getRedirectUri(currentConfig, externalHostHeader))
                        .authorization(currentConfig.oauth2Authorization())
                        .build());
    }

    /**
     * When the state in the query parameters has a matching cookie, exchange the code for a bearer
     * token, set it as a cookie, and redirect the user back to the application uri that was encoded
     * in the oauth state.
     */
    private HttpRedirect successfulRedirectResponse(
            HttpServerExchange exchange,
            String code,
            Optional<String> externalHostHeader,
            String state,
            URI redirectUri) {
        // success, issue a redirect and set the received token into a cookie
        OAuth2Credentials credentials = exchangeCode(code, externalHostHeader);
        cookieManager.setTokenCookie(
                exchange, cookiePath, credentials.bearerToken(), credentials.expiresIn());
        cookieManager.deleteStateCookie(exchange, cookiePath, state);
        return HttpRedirect.temporary(redirectUri);
    }

    /**
     * There was no state cookie matching the url state query parameter. This could mean that the
     * oauth state cookie has expired. Force the user to redo the auth flow with the specified
     * redirectUri.
     */
    private HttpRedirect retryOauthAuthorizeResponse(
            HttpServerExchange exchange, Optional<String> externalHostHeader, URI redirectUri) {
        log.info(
                "URL state parameter didn't match state from cookie. Redirecting user back to authorize uri");

        String newEncodedState = oauth2StateSerde.encodeRedirectUrlToState(redirectUri);
        cookieManager.setStateCookie(exchange, cookiePath, newEncodedState);
        return HttpRedirect.temporary(
                OAuthRedirects.getAuthorizeRedirectUri(
                        externalHostHeader, newEncodedState, config.get()));
    }
}
