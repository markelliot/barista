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

import io.undertow.util.HttpString;
import io.undertow.util.Methods;

@SuppressWarnings("ImmutableEnumChecker")
public enum HttpMethod {
    GET(Methods.GET),
    PUT(Methods.PUT),
    POST(Methods.POST),
    DELETE(Methods.DELETE);

    private final HttpString method;

    HttpMethod(HttpString method) {
        this.method = method;
    }

    public HttpString method() {
        return method;
    }
}
