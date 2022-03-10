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

package com.markelliot.barista.oauth2.tokens;

import com.markelliot.barista.authz.AuthToken;
import com.palantir.tokens.auth.BearerToken;

public record WrappedBearerToken(BearerToken delegate) implements AuthToken {
    @Override
    public String token() {
        return delegate().getToken();
    }

    public static AuthToken of(BearerToken delegate) {
        return new WrappedBearerToken(delegate);
    }

    public static AuthToken of(String token) {
        return new WrappedBearerToken(BearerToken.valueOf(token));
    }

    public static AuthToken fromAuthorizationHeader(String headerValue) {
        return new WrappedBearerToken(
                BearerToken.valueOf(
                        headerValue.startsWith("Bearer ")
                                ? headerValue.substring(7)
                                : headerValue));
    }
}
