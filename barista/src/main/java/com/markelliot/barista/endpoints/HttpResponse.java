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

package com.markelliot.barista.endpoints;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

public record HttpResponse(
        HttpStatus status, String bytes, Map<String, String> headers, Set<Cookie> cookies) {
    public enum HttpStatus {
        OK(200),
        OK_EMPTY(201),
        CLIENT_ERROR_MALFORMED_REQUEST(400),
        CLIENT_ERROR_UNAUTHENTICATED(401),
        CLIENT_ERROR_UNAUTHORIZED(403),
        SERVER_ERROR(500);

        private final int statusCode;

        HttpStatus(int statusCode) {
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }

    public record Cookie(
            String name, String value, String path, Duration lifetime, String sameSiteMode) {}
}
