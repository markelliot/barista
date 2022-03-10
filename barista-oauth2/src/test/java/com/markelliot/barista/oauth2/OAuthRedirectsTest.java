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

import com.markelliot.barista.oauth2.objects.OAuth2Configuration;
import java.net.URI;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public final class OAuthRedirectsTest {
    public static final OAuth2Configuration SPP_CONFIG =
            getConfig("https://authorizeUri/api", "https://authorizeUri/context");
    public static final OAuth2Configuration NON_SPP_CONFIG =
            getConfig("https://authorizeUri/api", "https://redirectUri/context");

    private static OAuth2Configuration getConfig(String authorizeUri, String redirectUri) {
        return OAuth2Configuration.builder()
                .clientId("clientId")
                .clientSecret("clientSecret")
                .externalUri(authorizeUri)
                .redirectUri(redirectUri)
                .build();
    }

    @Test
    public void test_isSppModeFalseWhenExternalAndRedirectUriHostAndPortDoNotMatch() {
        assertThat(getConfig("https://externalUri", "https://redirectUri").isSinglePortProxyMode())
                .isFalse();
        assertThat(
                        getConfig("https://localhost:8080", "https://localhost:8643")
                                .isSinglePortProxyMode())
                .isFalse();
        assertThat(
                        getConfig(
                                        "https://localhost:8080/foo/bar/baz",
                                        "https://localhost:8643/bar/baz/foo")
                                .isSinglePortProxyMode())
                .isFalse();
    }

    @Test
    public void test_isSppModeTrueWhenExternalAndRedirectUriMatchOnlyOnHostAndPort() {
        assertThat(
                        getConfig(
                                        "https://externalUri:8080/foo/bar/baz",
                                        "https://externalUri:8080/bar/baz/foo")
                                .isSinglePortProxyMode())
                .isTrue();
    }

    @Test
    public void test_getAuthorizeUri_preferHostButNoHostPresent() {
        Assertions.assertThat(
                        OAuthRedirects.getAuthorizeRedirectUri(
                                Optional.empty(), "encodedState", SPP_CONFIG))
                .isEqualTo(
                        "https://authorizeUri/api/oauth2/authorize?client_id=clientId"
                                + "&redirect_uri=https%3A%2F%2FauthorizeUri%2Fcontext"
                                + "&response_type=code&state=encodedState");
    }

    @Test
    public void test_getAuthorizeUri_preferHostWitHostPresent() {
        assertThat(
                        OAuthRedirects.getAuthorizeRedirectUri(
                                Optional.of("hostUri"), "encodedState", SPP_CONFIG))
                .isEqualTo(
                        "https://hostUri/api/oauth2/authorize?client_id=clientId"
                                + "&redirect_uri=https%3A%2F%2FhostUri%2Fcontext"
                                + "&response_type=code&state=encodedState");
    }

    @Test
    public void test_getAuthorizeUri_preferConfigNoHostPresent() {
        assertThat(
                        OAuthRedirects.getAuthorizeRedirectUri(
                                Optional.empty(), "encodedState", NON_SPP_CONFIG))
                .isEqualTo(
                        "https://authorizeUri/api/oauth2/authorize?client_id=clientId"
                                + "&redirect_uri=https%3A%2F%2FredirectUri%2Fcontext"
                                + "&response_type=code&state=encodedState");
    }

    @Test
    public void test_getAuthorizeUri_preferConfigWithHostPresent() {
        assertThat(
                        OAuthRedirects.getAuthorizeRedirectUri(
                                Optional.of("hostUri"), "encodedState", NON_SPP_CONFIG))
                .isEqualTo(
                        "https://authorizeUri/api/oauth2/authorize?client_id=clientId"
                                + "&redirect_uri=https%3A%2F%2FredirectUri%2Fcontext"
                                + "&response_type=code&state=encodedState");
    }

    @Test
    public void testLocalUri() throws Exception {
        URI local =
                OAuthRedirects.toLocalUri(
                        new URI("https://localhost/foo%2ebar/baz?foo%2ebar=baz%2ebang"));
        assertThat(local).isEqualTo(new URI("/foo%2ebar/baz?foo%2ebar=baz%2ebang"));
    }

    @Test
    public void testLocalUriSameAsInput() throws Exception {
        URI original = new URI("/baz?foo%2ebar=baz%2ebang");
        assertThat(OAuthRedirects.toLocalUri(original)).isSameAs(original);
    }
}
