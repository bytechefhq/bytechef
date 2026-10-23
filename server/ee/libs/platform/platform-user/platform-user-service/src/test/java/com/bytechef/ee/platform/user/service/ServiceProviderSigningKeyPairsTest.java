/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.user.service.ServiceProviderSigningKeyPairs.PemKeyPair;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ServiceProviderSigningKeyPairsTest {

    @Test
    void testGeneratesASelfSignedCertificateForTheKey() throws GeneralSecurityException {
        PemKeyPair pemKeyPair = ServiceProviderSigningKeyPairs.generate();

        assertThat(pemKeyPair.certificate()).startsWith("-----BEGIN CERTIFICATE-----\n");
        assertThat(pemKeyPair.privateKey()).startsWith("-----BEGIN PRIVATE KEY-----\n");

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

        X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(
            new ByteArrayInputStream(pemKeyPair.certificate()
                .getBytes(StandardCharsets.US_ASCII)));

        certificate.verify(certificate.getPublicKey());

        assertThat(certificate.getSubjectX500Principal()
            .getName()).isEqualTo("CN=" + ServiceProviderSigningKeyPairs.COMMON_NAME);
        assertThat(certificate.getNotAfter()
            .toInstant()).isAfter(Instant.now()
                .plus(ServiceProviderSigningKeyPairs.VALIDITY)
                .minus(Duration.ofDays(1)));

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        String keyBase64 = pemKeyPair.privateKey()
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder()
            .decode(keyBase64)));

        byte[] payload = "AuthnRequest".getBytes(StandardCharsets.UTF_8);

        Signature signer = Signature.getInstance("SHA256withRSA");

        signer.initSign(privateKey);
        signer.update(payload);

        byte[] signature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");

        verifier.initVerify(certificate);
        verifier.update(payload);

        assertThat(verifier.verify(signature)).isTrue();
    }

    @Test
    void testEachCallGeneratesANewKey() {
        assertThat(ServiceProviderSigningKeyPairs.generate()
            .privateKey()).isNotEqualTo(
                ServiceProviderSigningKeyPairs.generate()
                    .privateKey());
    }

    @Test
    void testToStringDoesNotRevealThePrivateKey() {
        PemKeyPair pemKeyPair = ServiceProviderSigningKeyPairs.generate();

        assertThat(pemKeyPair.toString()).doesNotContain(pemKeyPair.privateKey())
            .contains("[REDACTED]");
    }
}
