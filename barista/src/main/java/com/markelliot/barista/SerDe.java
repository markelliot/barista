/*
 * (c) Copyright 2021 Mark Elliot. All rights reserved.
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

package com.markelliot.barista;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.net.MediaType;
import java.io.IOException;

public interface SerDe {
    <T> ByteRepr serialize(T any);

    <T> T deserialize(ByteRepr bytes, Class<T> objClass);

    String contentType();

    default SerDe forMimeType(String mimeType) {
        return this;
    }

    record ByteRepr(String raw) {}

    final class MimeTypeDispatchingSerDe implements SerDe {
        public static final SerDe INSTANCE = new MimeTypeDispatchingSerDe();

        private MimeTypeDispatchingSerDe() {}

        @Override
        public <T> ByteRepr serialize(T any) {
            return ObjectMapperSerDe.JSON.serialize(any);
        }

        @Override
        public <T> T deserialize(ByteRepr bytes, Class<T> objClass) {
            return ObjectMapperSerDe.JSON.deserialize(bytes, objClass);
        }

        @Override
        public String contentType() {
            return ObjectMapperSerDe.JSON.contentType();
        }

        @Override
        public SerDe forMimeType(String mimeType) {
            if (mimeType.startsWith("application/yaml")) {
                return ObjectMapperSerDe.YAML;
            }
            return ObjectMapperSerDe.JSON;
        }
    }

    final class ObjectMapperSerDe implements SerDe {
        static final SerDe JSON = new ObjectMapperSerDe(new ObjectMapper(), MediaType.JSON_UTF_8);
        static final SerDe YAML = new ObjectMapperSerDe(new ObjectMapper(new YAMLFactory()
                .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
                .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
                .configure(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID, false)),
                MediaType.create("text", "x-yaml")
        );

        private final ObjectMapper mapper;
        private final MediaType mediaType;

        private ObjectMapperSerDe(ObjectMapper mapper, MediaType mediaType) {
            this.mapper = mapper
                    .registerModule(new GuavaModule())
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule())
                    .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            this.mediaType = mediaType;
        }

        @Override
        public <T> ByteRepr serialize(T any) {
            try {
                return new ByteRepr(mapper.writeValueAsString(any));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error while serializing object to bytes", e);
            }
        }

        @Override
        public <T> T deserialize(ByteRepr bytes, Class<T> objClass) {
            try {
                return mapper.readValue(bytes.raw(), objClass);
            } catch (IOException e) {
                throw new RuntimeException("Error while deserializing object from bytes", e);
            }
        }

        @Override
        public String contentType() {
            return mediaType.toString();
        }
    }
}
