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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.markelliot.barista.Server;
import com.markelliot.barista.authz.Authz;
import com.palantir.conjure.java.api.errors.UnknownRemoteException;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AuthRedirectResourceIntegrationTest {

    private static final int PORT = 8683;
    private static final String CONTEXT_PATH = "/oauth-test";
    private static final OAuth2Configuration OAUTH2_CONFIG =
            OAuth2Configuration.builder()
                    .clientId("id")
                    .clientSecret("secret")
                    .externalUri("https://localhost:8643/multipass/api")
                    .redirectUri("https://localhost:8080/test/redirect")
                    .build();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private OAuth2ClientBlocking oauth2Client;

    private Server barista;

    @Before
    public void before() {
        barista =
                Server.builder()
                        .authz(Authz.denyAll())
                        .allowAllOrigins()
                        .port(PORT)
                        .oauth2(oa)
                        .tracingRate(1.0)
                        .disableTls()
                        .start();
        barista(new OAuth2Feature(CONTEXT_PATH, oauth2Client, () -> OAUTH2_CONFIG));
    }

    @After
    public void after() {
        if (barista != null) {
            barista.stop();
        }
    }

    @Test
    public void testMissingParameterResultsIn4xx() {
        RedirectClient client =
                barista.conjureClients().client(RedirectClient.class, "redirect").get();
        assertThatThrownBy(
                        () -> client.request(Optional.empty(), Optional.empty(), Optional.empty()))
                .isInstanceOfSatisfying(
                        UnknownRemoteException.class,
                        unknownRemoteException ->
                                assertThat(unknownRemoteException.getStatus()).isEqualTo(400));
    }

    @Path("redirect")
    public interface RedirectClient {

        @GET
        Response request(
                @QueryParam("error") Optional<String> error,
                @QueryParam("state") Optional<String> urlState,
                @QueryParam("code") Optional<String> code);
    }
}
