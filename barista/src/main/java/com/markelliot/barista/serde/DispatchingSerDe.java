/*
 * (c) Copyright 2025 Mark Elliot. All rights reserved.
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

package com.markelliot.barista.serde;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import com.markelliot.barista.SerDe;
import java.util.Map;

public final class DispatchingSerDe<E> implements SerDe<E> {
    private final ImmutableMap<MediaType, SerDe.MimeTypeSerDe<E>> serdes;
    private final MimeTypeSerDe<E> defaultSerDe;

    private DispatchingSerDe(Map<MediaType, MimeTypeSerDe<E>> serdes, MimeTypeSerDe<E> defaultSerDe) {
        this.serdes = ImmutableMap.copyOf(serdes);
        this.defaultSerDe = defaultSerDe;
    }

    @Override
    public MimeTypeSerDe<E> select(MediaType mimeType) {
        SerDe.MimeTypeSerDe<E> maybe = serdes.get(mimeType.withoutParameters());
        if (maybe != null) {
            return maybe;
        }
        return defaultSerDe;
    }

    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    public static DispatchingSerDe<Exception> createDefault() {
        ObjectMapperSerDe yaml = new ObjectMapperSerDe(
                MediaType.create("application", "yaml"),
                new ObjectMapper(new YAMLFactory()
                                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
                        .registerModule(new GuavaModule())
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule())
                        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
        ObjectMapperSerDe json = new ObjectMapperSerDe(MediaType.create("application", "json"), new ObjectMapper());
        return DispatchingSerDe.<Exception>builder()
                .add(MediaType.create("application", "yaml"), yaml)
                .add(MediaType.create("application", "json"), json)
                .defaultSerDe(json)
                .build();
    }

    public static final class Builder<E> {
        private ImmutableMap.Builder<MediaType, SerDe.MimeTypeSerDe<E>> serdes = ImmutableMap.builder();
        private SerDe.MimeTypeSerDe<E> defaultSerDe = null;

        private Builder() {}

        public Builder<E> add(MediaType mimeType, SerDe.MimeTypeSerDe<E> serde) {
            serdes.put(mimeType.withoutParameters(), serde);
            return this;
        }

        public Builder<E> defaultSerDe(SerDe.MimeTypeSerDe<E> defaultSerDe) {
            this.defaultSerDe = defaultSerDe;
            return this;
        }

        public DispatchingSerDe<E> build() {
            return new DispatchingSerDe<>(serdes.buildKeepingLast(), defaultSerDe);
        }
    }
}
