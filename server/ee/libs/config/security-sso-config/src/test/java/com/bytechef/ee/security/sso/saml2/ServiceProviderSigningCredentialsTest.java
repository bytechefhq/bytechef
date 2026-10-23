/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.saml2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import org.junit.jupiter.api.Test;
import org.springframework.security.saml2.core.Saml2X509Credential;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ServiceProviderSigningCredentialsTest {

    @Test
    void testStoredKeyPairReadsBackAsASigningCredential() throws GeneralSecurityException, IOException {
        Saml2X509Credential signingCredential = ServiceProviderSigningCredentials.toSigningCredential(
            resource("sp-signing-certificate.pem"), resource("sp-signing-key.pem"));

        assertThat(signingCredential.isSigningCredential()).isTrue();

        byte[] payload = "AuthnRequest".getBytes(StandardCharsets.UTF_8);

        Signature signer = Signature.getInstance("SHA256withRSA");

        signer.initSign(signingCredential.getPrivateKey());
        signer.update(payload);

        byte[] signature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");

        verifier.initVerify(signingCredential.getCertificate());
        verifier.update(payload);

        assertThat(verifier.verify(signature)).isTrue();
    }

    @Test
    void testAnUnreadablePrivateKeyIsRejected() throws IOException {
        String certificate = resource("sp-signing-certificate.pem");

        assertThatThrownBy(() -> ServiceProviderSigningCredentials.toSigningCredential(
            certificate, "-----BEGIN PRIVATE KEY-----\nAAAA\n-----END PRIVATE KEY-----\n"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to read the SAML signing credential");
    }

    private static String resource(String name) throws IOException {
        try (InputStream inputStream = ServiceProviderSigningCredentialsTest.class.getResourceAsStream(
            "/saml2/" + name)) {

            return new String(inputStream.readAllBytes(), StandardCharsets.US_ASCII);
        }
    }
}
