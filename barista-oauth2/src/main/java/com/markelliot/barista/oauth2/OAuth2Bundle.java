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

import com.markelliot.barista.Bundle;
import com.markelliot.barista.endpoints.Endpoints;
import com.markelliot.barista.handlers.DelegatingHandler;
import com.markelliot.barista.oauth2.objects.OAuth2Configuration;
import java.util.Optional;

public final class OAuth2Bundle implements Bundle {
    private final String contextPath;
    private final OAuth2Client client;
    private final OAuth2Configuration config;

    public OAuth2Bundle(String contextPath, OAuth2Client client, OAuth2Configuration config) {
        this.contextPath = contextPath;
        this.client = client;
        this.config = config;
    }

    @Override
    public String name() {
        return "OAuth2";
    }

    @Override
    public Optional<DelegatingHandler> handler() {
        return Optional.of(AuthDelegatingHandler.of(contextPath, client, () -> config));
    }

    @Override
    public Endpoints endpoints() {
        return new AuthRedirectResourceEndpoints(
                new AuthRedirectResource(
                        contextPath, client, () -> config, new OAuth2StateSerdeImpl()));
    }
}
