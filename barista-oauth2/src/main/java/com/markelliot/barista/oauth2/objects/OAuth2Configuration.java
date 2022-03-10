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

package com.markelliot.barista.oauth2.objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@ImmutablesStyle
@JsonDeserialize(as = ImmutableOAuth2Configuration.class)
@JsonSerialize(as = ImmutableOAuth2Configuration.class)
public interface OAuth2Configuration {

    @Value.Redacted
    @JsonProperty("id")
    String clientId();

    @Value.Redacted
    @JsonProperty("secret")
    String clientSecret();

    @JsonProperty("auth-uri")
    String externalUri();

    @JsonProperty("redirect-uri")
    String redirectUri();

    @JsonProperty("allow-spp-mode")
    Optional<Boolean> allowSppMode();

    @Value.Derived
    default OAuth2Authorization oauth2Authorization() {
        return OAuth2Authorization.builder()
                .clientId(clientId())
                .clientSecret(clientSecret())
                .build();
    }

    /** Returns true if authorizeUri and redirectUri share host and port. */
    @JsonIgnore
    @Value.Derived
    default boolean isSinglePortProxyMode() {
        URI authorizeUri = URI.create(externalUri());
        URI redirectUri = URI.create(redirectUri());

        // enable only if authority matches
        return allowSppMode().orElse(true)
                && Objects.equals(authorizeUri.getRawAuthority(), redirectUri.getRawAuthority());
    }

    static Builder builder() {
        return new Builder();
    }

    final class Builder extends ImmutableOAuth2Configuration.Builder {}
}
