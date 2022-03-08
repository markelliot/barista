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

import com.palantir.dialogue.DialogueService;
import com.palantir.dialogue.HttpMethod;
import com.palantir.dialogue.annotations.Request;
import com.palantir.tokens.auth.AuthHeader;

@DialogueService(OAuth2ClientBlockingDialogueServiceFactory.class)
public interface OAuth2ClientBlocking {

    @Request(method = HttpMethod.POST, path = "/oauth2/token")
    OAuth2Credentials createToken(
            @Request.Body(CreateTokenRequestSerializerFactory.class) CreateTokenRequest request);

    @Request(method = HttpMethod.GET, path = "/oauth2/check_token")
    void checkToken(AuthHeader authHeader);
}
