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

import java.net.URI;
import java.net.URISyntaxException;

public record HttpRedirect(RedirectType type, URI location) {
    public static HttpRedirect permanent(URI location) {
        return new HttpRedirect(RedirectType.PERMANANT, location);
    }

    public static HttpRedirect temporary(String location) {
        try {
            return temporary(new URI(location));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpRedirect temporary(URI location) {
        return new HttpRedirect(RedirectType.TEMPORARY, location);
    }

    public enum RedirectType {
        PERMANANT(301),
        TEMPORARY(302);

        private final int statusCode;

        RedirectType(int statusCode) {
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }
}
