/*
 * This file is directly derived from:
 * https://github.com/palantir/conjure-java-runtime/blob/\
 * 27be7573c5b0e9d09e5b0a4ed10e882f969330a9/keystores/\
 * src/main/java/com/palantir/conjure/java/config/ssl/DefaultCas.java
 *
 * Such derivations are subject to the following copyright and license:
 *
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
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

package barista.tls;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class DefaultCas {
    /**
     * This should be updated by running `./gradlew regenerateCAs` whenever the Java version we use
     * to compile changes, to ensure we pick up new CAs or revoke insecure ones.
     */
    private static final File CA_PEM_FILE = new File("var/security", "cas.pem");

    private static final Supplier<Map<String, X509Certificate>> TRUSTED_CERTIFICATES =
            Suppliers.memoize(DefaultCas::getTrustedCertificates);

    static Map<String, X509Certificate> getCertificates() {
        return TRUSTED_CERTIFICATES.get();
    }

    private static Map<String, X509Certificate> getTrustedCertificates() {
        ImmutableMap.Builder<String, X509Certificate> certificateMap = ImmutableMap.builder();
        try {
            List<X509Certificate> caCertificates =
                    TransportLayerSecurity.readX509Certificates(new FileInputStream(CA_PEM_FILE))
                            .stream()
                            .map(cert -> (X509Certificate) cert)
                            .collect(Collectors.toList());
            for (X509Certificate cert : caCertificates) {
                String certificateCommonName =
                        cert.getSubjectX500Principal().getName().toLowerCase(Locale.ENGLISH);
                certificateMap.put(certificateCommonName, cert);
            }
        } catch (CertificateException | IOException e) {
            throw new RuntimeException("Could not read file as an X.509 certificate", e);
        }

        return certificateMap.build();
    }

    private DefaultCas() {}
}
