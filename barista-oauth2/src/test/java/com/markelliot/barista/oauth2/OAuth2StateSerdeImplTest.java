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

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.runner.RunWith;

@RunWith(JUnitQuickcheck.class)
public final class OAuth2StateSerdeImplTest {

    private static final Pattern VALID_BASE_64_WITH_UNDERSCORE_INSTEAD_OF_EQUALS_FOR_URL_SAFETY =
            Pattern.compile("^[0-9a-zA-Z+/\\-_]+$");

    public static final class UriGenerator extends Generator<URI> {
        public UriGenerator(Class<URI> type) {
            super(type);
        }

        @Override
        public URI generate(SourceOfRandomness random, GenerationStatus status) {
            Generator<String> stringGenerator = gen().type(String.class);
            while (true) {
                try {
                    return new URI(stringGenerator.generate(random, status));
                } catch (URISyntaxException e) {
                    // Try again
                }
            }
        }
    }

    @Property
    public void urlIsPreservedAfterSerializationAndDeserialization(
            @From(UriGenerator.class) URI url) {
        String encoded = new OAuth2StateSerdeImpl().encodeRedirectUrlToState(url);
        Optional<URI> decoded = new OAuth2StateSerdeImpl().decodeRedirectUrlFromState(encoded);

        assertThat(decoded).contains(url);
    }

    @Property
    public void encodedStateIsAsciiSafe(@From(UriGenerator.class) URI url) {
        String encoded = new OAuth2StateSerdeImpl().encodeRedirectUrlToState(url);

        assertThat(encoded)
                .containsPattern(VALID_BASE_64_WITH_UNDERSCORE_INSTEAD_OF_EQUALS_FOR_URL_SAFETY);
    }

    @Property
    public void returnsEmptyWhenUndecodable(String encodedUrl) {
        assertThat(new OAuth2StateSerdeImpl().decodeRedirectUrlFromState(encodedUrl)).isEmpty();
    }
}
