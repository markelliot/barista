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

package com.markelliot.barista.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.Test;

final class DefaultCasTest {
    @Test
    void preservesSubjectAliasForUniqueCertificates() {
        X509Certificate certificate = new TestCertificate("CN=unique-ca");

        Map<String, X509Certificate> certificates = DefaultCas.getTrustedCertificates(List.of(certificate));

        assertThat(certificates.keySet()).containsExactly("cn=unique-ca");
        assertThat(certificates.get("cn=unique-ca")).isSameAs(certificate);
    }

    @Test
    void allowsMultipleCertificatesWithTheSameSubject() {
        X509Certificate firstCertificate = new TestCertificate("CN=tenant");
        X509Certificate secondCertificate = new TestCertificate("CN=tenant");

        Map<String, X509Certificate> certificates =
                DefaultCas.getTrustedCertificates(List.of(firstCertificate, secondCertificate));

        assertThat(certificates.keySet()).containsExactly("cn=tenant-1", "cn=tenant-2");
        assertThat(certificates.get("cn=tenant-1")).isSameAs(firstCertificate);
        assertThat(certificates.get("cn=tenant-2")).isSameAs(secondCertificate);
    }

    private static final class TestCertificate extends X509Certificate {
        private final X500Principal subject;

        private TestCertificate(String subject) {
            this.subject = new X500Principal(subject);
        }

        @Override
        public X500Principal getSubjectX500Principal() {
            return subject;
        }

        @Override
        public void checkValidity() {}

        @Override
        public void checkValidity(Date date) {}

        @Override
        public int getVersion() {
            return 3;
        }

        @Override
        public BigInteger getSerialNumber() {
            return BigInteger.ONE;
        }

        @Override
        public Principal getIssuerDN() {
            return subject;
        }

        @Override
        public Principal getSubjectDN() {
            return subject;
        }

        @Override
        public Date getNotBefore() {
            return new Date(0);
        }

        @Override
        public Date getNotAfter() {
            return new Date(0);
        }

        @Override
        public byte[] getTBSCertificate() throws CertificateEncodingException {
            return new byte[0];
        }

        @Override
        public byte[] getSignature() {
            return new byte[0];
        }

        @Override
        public String getSigAlgName() {
            return "";
        }

        @Override
        public String getSigAlgOID() {
            return "";
        }

        @Override
        public byte[] getSigAlgParams() {
            return new byte[0];
        }

        @Override
        public boolean[] getIssuerUniqueID() {
            return new boolean[0];
        }

        @Override
        public boolean[] getSubjectUniqueID() {
            return new boolean[0];
        }

        @Override
        public boolean[] getKeyUsage() {
            return new boolean[0];
        }

        @Override
        public int getBasicConstraints() {
            return -1;
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            return new byte[0];
        }

        @Override
        public void verify(PublicKey key) throws CertificateException {}

        @Override
        public void verify(PublicKey key, String sigProvider) throws CertificateException {}

        @Override
        public String toString() {
            return subject.toString();
        }

        @Override
        public PublicKey getPublicKey() {
            return null;
        }

        @Override
        public boolean hasUnsupportedCriticalExtension() {
            return false;
        }

        @Override
        public Set<String> getCriticalExtensionOIDs() {
            return Set.of();
        }

        @Override
        public Set<String> getNonCriticalExtensionOIDs() {
            return Set.of();
        }

        @Override
        public byte[] getExtensionValue(String oid) {
            return new byte[0];
        }
    }
}
