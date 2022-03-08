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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import com.palantir.conjure.java.jackson.optimizations.ObjectMapperOptimizations;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OAuth2StateSerdeImpl implements OAuth2StateSerde {

    private static final Logger log = LoggerFactory.getLogger(OAuth2StateSerdeImpl.class);
    private static final JsonMapper mapper =
            JsonMapper.builder()
                    .addModules(ObjectMapperOptimizations.createModules())
                    .disable(DeserializationFeature.WRAP_EXCEPTIONS)
                    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                    .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                    .build();
    private static final BaseEncoding ENCODER = BaseEncoding.base64Url().omitPadding();

    private final Supplier<UUID> randomUuidGenerator;

    public OAuth2StateSerdeImpl() {
        this(UUID::randomUUID);
    }

    @VisibleForTesting
    OAuth2StateSerdeImpl(Supplier<UUID> randomUuidGenerator) {
        this.randomUuidGenerator = randomUuidGenerator;
    }

    @Override
    public Optional<URI> decodeRedirectUrlFromState(String urlState) {
        try {
            byte[] decodedState = ENCODER.decode(urlState);
            return Optional.of(mapper.readValue(decodedState, OAuth2StateToken.class))
                    .map(OAuth2StateToken::requestedUri)
                    .map(URI::create);
        } catch (IOException | IllegalArgumentException e) {
            log.error("Unable to deserialize OAuth2StateToken", e);
            return Optional.empty();
        }
    }

    @Override
    public String encodeRedirectUrlToState(URI redirectUrl) {
        OAuth2StateToken token =
                OAuth2StateToken.builder()
                        .uuid(randomUuidGenerator.get())
                        .requestedUri(redirectUrl.toString())
                        .build();
        try {
            byte[] serializedState = mapper.writeValueAsBytes(token);
            return ENCODER.encode(serializedState);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
