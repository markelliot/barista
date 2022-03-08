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

import com.google.common.collect.ImmutableMap;
import com.palantir.dialogue.RequestBody;
import com.palantir.dialogue.annotations.StdSerializer;

final class CreateTokenRequestSerializerFactory extends StdSerializer<CreateTokenRequest> {
    @Override
    public RequestBody serialize(CreateTokenRequest value) {
        return new FormUrlEncodedRequestBody(
                ImmutableMap.<String, String>builder()
                        .put("grant_type", value.grantType())
                        .put("code", value.authorizationCode())
                        .put("redirect_uri", value.callbackUrl())
                        .put("client_id", value.authorization().clientId())
                        .put("client_secret", value.authorization().clientSecret())
                        .build());
    }
}
