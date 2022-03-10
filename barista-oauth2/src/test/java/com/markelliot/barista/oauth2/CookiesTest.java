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

package com.markelliot.barista.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public final class CookiesTest {
    @Test
    public void getSafeCookiePath() throws Exception {
        Assertions.assertThat(Cookies.getSafeCookiePath(null)).isEqualTo("/");
        assertThat(Cookies.getSafeCookiePath("")).isEqualTo("/");
        assertThat(Cookies.getSafeCookiePath("/foo")).isEqualTo("/foo");
        assertThat(Cookies.getSafeCookiePath("foo")).isEqualTo("/foo");
        assertThat(Cookies.getSafeCookiePath("/foo/")).isEqualTo("/foo");
        assertThat(Cookies.getSafeCookiePath("foo/")).isEqualTo("/foo");
    }
}
