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

import com.palantir.tokens.auth.BearerToken;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.handlers.CookieSameSiteMode;
import io.undertow.util.Headers;
import io.undertow.util.SameSiteNoneIncompatibleClientChecker;
import java.util.Objects;
import java.util.Optional;

enum CookieManagerImpl implements CookieManager {
    INSTANCE;

    private static final int MAX_AGE_FOR_COOKIE_DELETION = 0;

    @Override
    public void setStateCookie(HttpServerExchange exchange, String path, String state) {
        Cookie cookie = new CookieImpl(state, Cookies.OAUTH_STATE);
        cookie.setPath(Cookies.getSafeCookiePath(path));
        cookie.setMaxAge(Cookies.OAUTH_STATE_MAX_AGE);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        setSameSiteNoneIfCompatible(exchange, cookie);

        exchange.setResponseCookie(cookie);
    }

    @Override
    public void deleteStateCookie(HttpServerExchange exchange, String path, String state) {
        Cookie cookie = new CookieImpl(state, "");
        cookie.setPath(Cookies.getSafeCookiePath(path));
        cookie.setMaxAge(MAX_AGE_FOR_COOKIE_DELETION);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        setSameSiteNoneIfCompatible(exchange, cookie);

        exchange.setResponseCookie(cookie);
    }

    @Override
    public void setTokenCookie(
            HttpServerExchange exchange, String path, BearerToken token, int maxAge) {
        Cookie cookie = new CookieImpl(Cookies.TOKEN_COOKIE_NAME, token.getToken());
        cookie.setPath(Cookies.getSafeCookiePath(path));
        cookie.setMaxAge(maxAge);
        cookie.setSecure(true);
        cookie.setHttpOnly(false);
        setSameSiteNoneIfCompatible(exchange, cookie);

        exchange.setResponseCookie(cookie);
    }

    @Override
    public Optional<String> getTokenCookie(HttpServerExchange exchange) {
        return Optional.ofNullable(exchange.getRequestCookie(Cookies.TOKEN_COOKIE_NAME))
                .map(Cookie::getValue);
    }

    @Override
    public boolean hasStateCookie(HttpServerExchange exchange, String state) {
        return Optional.ofNullable(exchange.getRequestCookie(state))
                .filter(cookie -> Objects.equals(cookie.getValue(), Cookies.OAUTH_STATE))
                .isPresent();
    }

    private static void setSameSiteNoneIfCompatible(HttpServerExchange exchange, Cookie cookie) {
        String userAgent = exchange.getRequestHeaders().getFirst(Headers.USER_AGENT);
        if (userAgent != null
                && SameSiteNoneIncompatibleClientChecker.shouldSendSameSiteNone(userAgent)) {
            cookie.setSameSiteMode(CookieSameSiteMode.NONE.toString());
        }
    }
}
