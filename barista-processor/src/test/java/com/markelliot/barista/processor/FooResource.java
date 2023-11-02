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

package com.markelliot.barista.processor;

import com.markelliot.barista.annotations.Http;
import com.markelliot.barista.annotations.Param.Cookie;
import com.markelliot.barista.annotations.Param.Header;
import com.markelliot.barista.annotations.Param.Query;
import com.markelliot.barista.authz.VerifiedAuthToken;
import com.markelliot.barista.endpoints.HttpRedirect;
import java.util.Optional;

public final class FooResource {
    @Http.Get("/foo/authed/get")
    public String authedGet(VerifiedAuthToken authToken) {
        return "";
    }

    @Http.Get("/foo/authed/get-empty")
    public void authedGetEmpty(VerifiedAuthToken authToken) {}

    @Http.Get("/foo/authed/get-redirect")
    public HttpRedirect authedGetRedirect(VerifiedAuthToken authToken) {
        return HttpRedirect.temporary("https://google.com");
    }

    @Http.Post("/foo/authed/post")
    public void authedPost(VerifiedAuthToken authToken, String body) {}

    @Http.Post("/foo/authed/post/{pathParam}")
    public void authedPostWithParams(
            VerifiedAuthToken authToken,
            String body,
            String pathParam,
            @Query("differentQueryName") Optional<String> queryParam,
            @Header("header-name") Optional<String> headerParam,
            @Cookie("cookieName") Optional<String> cookieParam) {}

    @Http.Get("/foo/open/get")
    public String openGet() {
        return "Hello, World!";
    }

    @Http.Get("/foo/open/echo/{msg}")
    public String openEcho(String msg) {
        return msg;
    }
}
