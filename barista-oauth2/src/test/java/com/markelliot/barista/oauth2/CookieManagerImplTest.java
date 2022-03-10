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
import static org.mockito.Mockito.when;

import com.palantir.tokens.auth.BearerToken;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.handlers.CookieSameSiteMode;
import io.undertow.util.Headers;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xnio.OptionMap;

public final class CookieManagerImplTest {

    private static final String SAME_SITE_NONE_COMPATIBLE_USER_AGENT = "userAgent";
    private static final String SAME_SITE_NONE_INCOMPATIBLE_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36";

    private static final String CONTEXT_PATH = "/context-path";
    private static final int MAX_AGE = 3600;
    private static final String STATE = "state";
    private static final BearerToken TOKEN = BearerToken.valueOf("token");
    private final CookieManager cookieManager = CookieManagerImpl.INSTANCE;
    @Mock private ServerConnection serverConnection;
    private HttpServerExchange exchange;

    private static Cookie getResponseCookie(HttpServerExchange exchange, String name) {
        for (Cookie cookie : exchange.responseCookies()) {
            if (Objects.equals(name, cookie.getName())) {
                return cookie;
            }
        }
        throw new IllegalStateException("Failed to find cookie with name: " + name);
    }

    @BeforeEach
    public void before() {
        when(serverConnection.getUndertowOptions()).thenReturn(OptionMap.EMPTY);

        exchange = new HttpServerExchange(serverConnection);
    }

    @Test
    public void setStateCookie() {
        exchange.getRequestHeaders().put(Headers.USER_AGENT, SAME_SITE_NONE_COMPATIBLE_USER_AGENT);

        cookieManager.setStateCookie(exchange, CONTEXT_PATH, STATE);

        Cookie cookie = getResponseCookie(exchange, STATE);
        assertThat(cookie.getName()).isEqualTo(STATE);
        assertThat(cookie.getValue()).isEqualTo(Cookies.OAUTH_STATE);
        assertThat(cookie.getPath()).isEqualTo(CONTEXT_PATH);
        assertThat(cookie.getMaxAge()).isEqualTo(3600);
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSiteMode()).isEqualTo(CookieSameSiteMode.NONE.toString());
    }

    @Test
    public void setStateCookie_sameSiteNoneIncompatible() {
        exchange.getRequestHeaders()
                .put(Headers.USER_AGENT, SAME_SITE_NONE_INCOMPATIBLE_USER_AGENT);

        cookieManager.setStateCookie(exchange, CONTEXT_PATH, STATE);

        Cookie cookie = getResponseCookie(exchange, STATE);
        assertThat(cookie.getName()).isEqualTo(STATE);
        assertThat(cookie.getValue()).isEqualTo(Cookies.OAUTH_STATE);
        assertThat(cookie.getPath()).isEqualTo(CONTEXT_PATH);
        assertThat(cookie.getMaxAge()).isEqualTo(3600);
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSiteMode()).isNull();
    }

    @Test
    public void deleteStateCookie() {
        exchange.getRequestHeaders().put(Headers.USER_AGENT, SAME_SITE_NONE_COMPATIBLE_USER_AGENT);

        cookieManager.deleteStateCookie(exchange, CONTEXT_PATH, STATE);

        Cookie cookie = getResponseCookie(exchange, STATE);
        assertThat(cookie.getName()).isEqualTo(STATE);
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getPath()).isEqualTo(CONTEXT_PATH);
        assertThat(cookie.getMaxAge()).isEqualTo(0);
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSiteMode()).isEqualTo(CookieSameSiteMode.NONE.toString());
    }

    @Test
    public void deleteStateCookie_sameSiteNoneIncompatible() {
        exchange.getRequestHeaders()
                .put(Headers.USER_AGENT, SAME_SITE_NONE_INCOMPATIBLE_USER_AGENT);

        cookieManager.deleteStateCookie(exchange, CONTEXT_PATH, STATE);

        Cookie cookie = getResponseCookie(exchange, STATE);
        assertThat(cookie.getName()).isEqualTo(STATE);
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getPath()).isEqualTo(CONTEXT_PATH);
        assertThat(cookie.getMaxAge()).isEqualTo(0);
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSiteMode()).isNull();
    }

    @Test
    public void setTokenCookie() {
        exchange.getRequestHeaders().put(Headers.USER_AGENT, SAME_SITE_NONE_COMPATIBLE_USER_AGENT);

        cookieManager.setTokenCookie(exchange, CONTEXT_PATH, TOKEN, MAX_AGE);

        Cookie cookie = getResponseCookie(exchange, Cookies.TOKEN_COOKIE_NAME);
        assertThat(cookie.getName()).isEqualTo(Cookies.TOKEN_COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo(TOKEN.getToken());
        assertThat(cookie.getPath()).isEqualTo(CONTEXT_PATH);
        assertThat(cookie.getMaxAge()).isEqualTo(MAX_AGE);
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isFalse();
        assertThat(cookie.getSameSiteMode()).isEqualTo(CookieSameSiteMode.NONE.toString());
    }

    @Test
    public void setTokenCookie_sameSiteNoneIncompatible() {
        exchange.getRequestHeaders()
                .put(Headers.USER_AGENT, SAME_SITE_NONE_INCOMPATIBLE_USER_AGENT);

        cookieManager.setTokenCookie(exchange, CONTEXT_PATH, TOKEN, MAX_AGE);

        Cookie cookie = getResponseCookie(exchange, Cookies.TOKEN_COOKIE_NAME);
        assertThat(cookie.getName()).isEqualTo(Cookies.TOKEN_COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo(TOKEN.getToken());
        assertThat(cookie.getPath()).isEqualTo(CONTEXT_PATH);
        assertThat(cookie.getMaxAge()).isEqualTo(MAX_AGE);
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isFalse();
        assertThat(cookie.getSameSiteMode()).isNull();
    }

    @Test
    public void getTokenCookie_present() {
        Cookie cookie = new CookieImpl(Cookies.TOKEN_COOKIE_NAME, TOKEN.getToken());
        exchange.setRequestCookie(cookie);

        Optional<String> token = cookieManager.getTokenCookie(exchange);

        assertThat(token).contains(TOKEN.getToken());
    }

    @Test
    public void getTokenCookie_absent() {
        Optional<String> token = cookieManager.getTokenCookie(exchange);

        assertThat(token).isEmpty();
    }

    @Test
    public void hasStateCookie_present() {
        Cookie cookie = new CookieImpl(STATE, Cookies.OAUTH_STATE);
        exchange.setRequestCookie(cookie);

        boolean hasStateCookie = cookieManager.hasStateCookie(exchange, STATE);

        assertThat(hasStateCookie).isTrue();
    }

    @Test
    public void hasStateCookie_absent() {
        boolean hasStateCookie = cookieManager.hasStateCookie(exchange, STATE);

        assertThat(hasStateCookie).isFalse();
    }

    @Test
    public void hasStateCookie_incorrectValue() {
        Cookie cookie = new CookieImpl(STATE, "incorrectValue");
        exchange.setRequestCookie(cookie);

        boolean hasStateCookie = cookieManager.hasStateCookie(exchange, STATE);

        assertThat(hasStateCookie).isFalse();
    }
}
