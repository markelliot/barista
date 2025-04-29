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

import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import com.markelliot.barista.SerDe;
import java.util.Map;

public final class DispatchingSerDe implements SerDe {
    private final ImmutableMap<MediaType, SerDe.MimeTypeSerDe> serdes;
    private final MimeTypeSerDe defaultSerDe;

    private DispatchingSerDe(Map<MediaType, MimeTypeSerDe> serdes, MimeTypeSerDe defaultSerDe) {
        this.serdes = ImmutableMap.copyOf(serdes);
        this.defaultSerDe = defaultSerDe;
    }

    @Override
    public MimeTypeSerDe<?> select(MediaType mimeType) {
        SerDe.MimeTypeSerDe maybe = serdes.get(mimeType.withoutParameters());
        if (maybe != null) {
            return maybe;
        }
        return defaultSerDe;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ImmutableMap.Builder<MediaType, SerDe.MimeTypeSerDe> serdes = ImmutableMap.builder();
        private SerDe.MimeTypeSerDe defaultSerDe = null;

        private Builder() {}

        public Builder add(MediaType mimeType, SerDe.MimeTypeSerDe serde) {
            serdes.put(mimeType.withoutParameters(), serde);
            return this;
        }

        public Builder defaultSerDe(SerDe.MimeTypeSerDe defaultSerDe) {
            this.defaultSerDe = defaultSerDe;
            return this;
        }

        public DispatchingSerDe build() {
            return new DispatchingSerDe(serdes.buildKeepingLast(), defaultSerDe);
        }
    }
}
