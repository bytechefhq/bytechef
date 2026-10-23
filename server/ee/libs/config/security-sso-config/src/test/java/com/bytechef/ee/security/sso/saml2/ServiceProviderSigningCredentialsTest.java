/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.saml2;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.security.sso.saml2.ServiceProviderSigningCredentials.PemCredential;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.saml2.core.Saml2X509Credential;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ServiceProviderSigningCredentialsTest {

    @Test
    void testGeneratedCredentialReadsBackAsASigningCredential() throws GeneralSecurityException {
        PemCredential pemCredential = ServiceProviderSigningCredentials.generate("ByteChef saml-7");

        assertThat(pemCredential.certificate()).startsWith("-----BEGIN CERTIFICATE-----\n");
        assertThat(pemCredential.privateKey()).startsWith("-----BEGIN PRIVATE KEY-----\n");

        Saml2X509Credential signingCredential = ServiceProviderSigningCredentials.toSigningCredential(
            pemCredential.certificate(), pemCredential.privateKey());

        assertThat(signingCredential.isSigningCredential()).isTrue();

        X509Certificate certificate = signingCredential.getCertificate();

        certificate.verify(certificate.getPublicKey());

        assertThat(certificate.getSubjectX500Principal()
            .getName()).isEqualTo("CN=ByteChef saml-7");
        assertThat(certificate.getNotAfter()
            .toInstant()).isAfter(Instant.now()
                .plus(ServiceProviderSigningCredentials.VALIDITY)
                .minus(Duration.ofDays(1)));

        byte[] payload = "AuthnRequest".getBytes(StandardCharsets.UTF_8);

        Signature signer = Signature.getInstance("SHA256withRSA");

        signer.initSign(signingCredential.getPrivateKey());
        signer.update(payload);

        byte[] signature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");

        verifier.initVerify(certificate);
        verifier.update(payload);

        assertThat(verifier.verify(signature)).isTrue();
    }

    @Test
    void testEachIdentityProviderGetsItsOwnKey() {
        PemCredential firstPemCredential = ServiceProviderSigningCredentials.generate("ByteChef saml-1");
        PemCredential secondPemCredential = ServiceProviderSigningCredentials.generate("ByteChef saml-2");

        assertThat(firstPemCredential.privateKey()).isNotEqualTo(secondPemCredential.privateKey());
    }

    @Test
    void testToStringDoesNotRevealThePrivateKey() {
        PemCredential pemCredential = ServiceProviderSigningCredentials.generate("ByteChef saml-7");

        assertThat(pemCredential.toString()).doesNotContain(pemCredential.privateKey())
            .contains("[REDACTED]");
    }
}
