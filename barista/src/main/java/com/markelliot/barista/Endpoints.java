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

import com.markelliot.barista.authz.VerifiedAuthToken;

/**
 * @deprecated Prefer using Barista's annotation processors, which generate {@link
 *     com.markelliot.barista.endpoints.Endpoints} implementations, or Barista's Conjure
 *     integration, which also generates {@link com.markelliot.barista.endpoints.Endpoints}
 *     implementations.
 */
@Deprecated
public final class Endpoints {
    private interface Endpoint<Request> {
        Class<Request> requestClass();

        String path();

        HttpMethod method();
    }

    /**
     * @deprecated Prefer using Barista's annotation processors, which generate {@link
     *     com.markelliot.barista.endpoints.Endpoints} implementations, or Barista's Conjure
     *     integration, which also generates {@link com.markelliot.barista.endpoints.Endpoints}
     *     implementations.
     */
    @Deprecated
    public interface VerifiedAuth<Request, Response> extends Endpoint<Request> {
        Response call(VerifiedAuthToken authToken, Request request);
    }

    /**
     * @deprecated Prefer using Barista's annotation processors, which generate {@link
     *     com.markelliot.barista.endpoints.Endpoints} implementations, or Barista's Conjure
     *     integration, which also generates {@link com.markelliot.barista.endpoints.Endpoints}
     *     implementations.
     */
    @Deprecated
    public interface Open<Request, Response> extends Endpoint<Request> {
        Response call(Request request);
    }
}
