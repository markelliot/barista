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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import com.markelliot.barista.Bytes;
import com.markelliot.barista.SerDe;
import com.markelliot.result.Result;
import java.io.IOException;

public final class ObjectMapperSerDe implements SerDe.MimeTypeSerDe<Exception> {
    private final MediaType mimeType;
    private final ObjectMapper mapper;

    public ObjectMapperSerDe(MediaType mimeType, ObjectMapper mapper) {
        this.mimeType = mimeType;
        this.mapper = mapper;
    }

    @Override
    public <T> Result<Bytes, Exception> serialize(T any) {
        try {
            return Result.ok(Bytes.from(mapper.writeValueAsBytes(any)));
        } catch (JsonProcessingException e) {
            return Result.error(e);
        }
    }

    @Override
    public <T> Result<T, Exception> deserialize(Bytes bytes, Class<T> type) {
        try {
            return Result.ok(mapper.readValue(bytes.getInputStream(), type));
        } catch (IOException e) {
            return Result.error(e);
        }
    }

    @Override
    public MediaType mimeType() {
        return mimeType;
    }
}
