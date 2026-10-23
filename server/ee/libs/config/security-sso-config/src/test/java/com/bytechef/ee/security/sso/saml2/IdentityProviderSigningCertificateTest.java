/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.saml2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.user.domain.IdentityProviderSigningCertificate;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IdentityProviderSigningCertificateTest {

    @Test
    void testBareBase64AndPemNormalizeToTheSamePem() throws IOException {
        String base64 = keycloakCertificateBase64();

        String pem = "-----BEGIN CERTIFICATE-----\n" + base64 + "\n-----END CERTIFICATE-----";

        String normalizedPem = IdentityProviderSigningCertificate.normalize(pem);

        assertThat(normalizedPem).startsWith("-----BEGIN CERTIFICATE-----\n")
            .endsWith("-----END CERTIFICATE-----\n");
        assertThat(IdentityProviderSigningCertificate.normalize(base64)).isEqualTo(normalizedPem);
        assertThat(IdentityProviderSigningCertificate.normalize("  " + normalizedPem.replace("\n", "\r\n") + "  "))
            .isEqualTo(normalizedPem);
    }

    @Test
    void testParseReadsTheCertificate() throws IOException {
        X509Certificate certificate = IdentityProviderSigningCertificate.parse(keycloakCertificateBase64());

        assertThat(certificate.getSubjectX500Principal()
            .getName()).isEqualTo("CN=bytechef-e2e");
    }

    @Test
    void testInvalidCertificatesAreRejected() {
        assertThatThrownBy(() -> IdentityProviderSigningCertificate.normalize("not a certificate"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("signingCertificate is not a valid X.509 certificate");
        assertThatThrownBy(() -> IdentityProviderSigningCertificate.normalize("-----BEGIN CERTIFICATE-----\nAAAA\n"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static String keycloakCertificateBase64() throws IOException {
        try (InputStream inputStream = IdentityProviderSigningCertificateTest.class.getResourceAsStream(
            "/saml2/keycloak-idp-metadata.xml")) {

            String metadata = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            Matcher matcher = Pattern.compile("<ds:X509Certificate>([^<]+)</ds:X509Certificate>")
                .matcher(metadata);

            assertThat(matcher.find()).isTrue();

            return matcher.group(1)
                .trim();
        }
    }
}
