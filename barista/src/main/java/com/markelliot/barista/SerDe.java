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

import com.google.common.net.MediaType;
import com.markelliot.result.Result;

public interface SerDe {
    MimeTypeSerDe<?> select(MediaType mimeType);

    interface MimeTypeSerDe<E> {
        <T> Result<Bytes, E> serialize(T any);

        <T> Result<T, E> deserialize(Bytes bytes, Class<T> type);

        MediaType mimeType();
    }
}
