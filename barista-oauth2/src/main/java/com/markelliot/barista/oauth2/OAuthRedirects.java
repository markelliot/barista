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

import com.google.common.base.Strings;
import com.palantir.logsafe.exceptions.SafeRuntimeException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.net.URIBuilder;

final class OAuthRedirects {

    private OAuthRedirects() {}

    static String getAuthorizeRedirectUri(
            Optional<String> externalHostHeader,
            String encodedState,
            OAuth2Configuration currentConfig) {
        try {
            return new URIBuilder(getAuthorizeUri(currentConfig, externalHostHeader))
                    .addParameter("client_id", currentConfig.clientId())
                    .addParameter("redirect_uri", getRedirectUri(currentConfig, externalHostHeader))
                    .addParameter("response_type", "code")
                    .addParameter("state", encodedState)
                    .toString();
        } catch (URISyntaxException e) {
            throw new SafeRuntimeException("Failed to construct uri", e);
        }
    }

    private static String getAuthorizeUri(
            OAuth2Configuration currentConfig, Optional<String> externalHostHeader) {
        return StringUtils.appendIfMissing(
                        getUri(
                                currentConfig.isSinglePortProxyMode(),
                                externalHostHeader,
                                currentConfig.externalUri()),
                        "/")
                + "oauth2/authorize";
    }

    static String getRedirectUri(
            OAuth2Configuration currentConfig, Optional<String> externalHostHeader) {
        return StringUtils.removeEnd(
                getUri(
                        currentConfig.isSinglePortProxyMode(),
                        externalHostHeader,
                        currentConfig.redirectUri()),
                "/");
    }

    static URI toLocalUri(URI uri) {
        if (uri.getHost() != null || uri.getScheme() != null || uri.getPort() != -1) {
            String path = Strings.isNullOrEmpty(uri.getRawPath()) ? "/" : uri.getRawPath();
            return URI.create(
                    Strings.nullToEmpty(
                            path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery())));
        }
        // Already a local uri
        return uri;
    }

    /**
     * In SPP mode, we replace the authorizeUri and redirectUri host and port with the host header.
     * This enables support for gateway-decided hosts, and enables multiple, external hostnames to
     * be used without additional per-service configuration.
     */
    private static String getUri(
            boolean sppMode, Optional<String> externalHostHeader, String configUri) {
        if (sppMode && externalHostHeader.isPresent()) {
            return StringUtils.removeEnd("https://" + externalHostHeader.get(), "/")
                    + URI.create(configUri).getPath();
        }

        return configUri;
    }
}
