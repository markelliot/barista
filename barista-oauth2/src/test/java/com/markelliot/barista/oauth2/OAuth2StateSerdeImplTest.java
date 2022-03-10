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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public final class OAuth2StateSerdeImplTest {

    private static final Pattern VALID_BASE_64_WITH_UNDERSCORE_INSTEAD_OF_EQUALS_FOR_URL_SAFETY =
            Pattern.compile("^[0-9a-zA-Z+/\\-_]+$");
    private static final int NUM_ITER = 100;
    private final Stream<URI> generator = generator();

    @Test
    public void urlIsPreservedAfterSerializationAndDeserialization() {
        generator.limit(NUM_ITER)
                .forEach(url -> {
                    String encoded = new OAuth2StateSerdeImpl().encodeRedirectUrlToState(url);
                    Optional<URI> decoded = new OAuth2StateSerdeImpl().decodeRedirectUrlFromState(encoded);
                    assertThat(decoded).contains(url);
                });
    }

    @Test
    public void encodedStateIsAsciiSafe() {
        generator.limit(NUM_ITER)
                .forEach(url -> {
                    String encoded = new OAuth2StateSerdeImpl().encodeRedirectUrlToState(url);
                    assertThat(encoded)
                            .containsPattern(VALID_BASE_64_WITH_UNDERSCORE_INSTEAD_OF_EQUALS_FOR_URL_SAFETY);
                });
    }

    @Test
    public void returnsEmptyWhenUndecodable() {
        assertThat(new OAuth2StateSerdeImpl().decodeRedirectUrlFromState("encodedUrl")).isEmpty();
    }

    private static Stream<URI> generator() {
        Random random = new Random();
        byte[] byteBuffer = new byte[16];
        return LongStream.generate(() -> 0L)
                .mapToObj(_unused -> {
                    while (true) {
                        try {
                            random.nextBytes(byteBuffer);
                            return new URI(new String(byteBuffer, StandardCharsets.US_ASCII));
                        } catch (URISyntaxException e) {
                            // Try again
                        }
                    }
                });
    }
}
