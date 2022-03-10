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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.palantir.dialogue.DialogueException;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeRuntimeException;
import com.palantir.tokens.auth.AuthHeader;
import com.palantir.tokens.auth.BearerToken;
import com.palantir.tokens.auth.UnverifiedJsonWebToken;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

final class OAuth2CookieFilter {

    private final Supplier<OAuth2Configuration> config;
    private final LoadingCache<String, Boolean> tokenValidityCache;

    OAuth2CookieFilter(OAuth2Client client, Supplier<OAuth2Configuration> config) {
        this.config = config;

        this.tokenValidityCache =
                Caffeine.newBuilder()
                        // this filter doesn't provide real security, just attempts to force a login
                        // if the token is absent
                        // or invalid, so this timeout just debounces how often we request validity
                        // information
                        // TODO(#107): ask Multipass for the liveness and expire the entry when
                        // liveness ends
                        .expireAfterWrite(Duration.ofSeconds(30))
                        // max size to prevent OOMs
                        .maximumSize(10_000L)
                        .build(
                                token -> {
                                    BearerToken bearerToken = BearerToken.valueOf(token);
                                    try {
                                        client.checkToken(AuthHeader.of(bearerToken));
                                        return true;
                                    } catch (DialogueException | UncheckedIOException e) {
                                        String userId =
                                                UnverifiedJsonWebToken.of(bearerToken)
                                                        .getUnverifiedUserId();
                                        throw new SafeRuntimeException(
                                                "checkToken did not succeed",
                                                e,
                                                SafeArg.of("userId", userId));
                                    } catch (RuntimeException e) {
                                        // this catch block is necessary because of the behavior of
                                        // com.palantir.remoting3.retrofit2.SerializableErrorInterceptor
                                        return false;
                                    }
                                });
    }

    /** Returns true when the caller should conduct an OAuth2 redirect flow. */
    public boolean shouldDoOauth2Flow(String requestPath, Optional<String> token) {
        if (AuthRedirectResource.REDIRECT_RESOURCE_PATH.equals(requestPath)) {
            // never intercept redirects for OAuth2
            return false;
        }

        if (token.isPresent()) {
            try {
                if (tokenValidityCache.get(token.get())) {
                    // have a valid token
                    return false;
                }
            } catch (UncheckedExecutionException e) {
                // suppress any issues to cause the login negotiation below to continue
            }
        }

        // the user doesn't have something that looks like a valid cookie, do the normal flow
        return true;
    }

    /** Returns the signed authorize redirect URI for OAuth2 flow. */
    public String getAuthorizeRedirectUri(
            Optional<String> externalHostHeader, String encodedState) {
        return OAuthRedirects.getAuthorizeRedirectUri(
                externalHostHeader, encodedState, config.get());
    }
}
