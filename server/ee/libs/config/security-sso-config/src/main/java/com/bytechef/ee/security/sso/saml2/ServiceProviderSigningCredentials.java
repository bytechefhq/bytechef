/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.saml2;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.springframework.security.saml2.core.Saml2X509Credential;

/**
 * Reads the stored PEM key pair ByteChef signs SAML authentication requests with into a Spring Security signing
 * credential.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ServiceProviderSigningCredentials {

    private static final String PRIVATE_KEY_LABEL = "PRIVATE KEY";

    private ServiceProviderSigningCredentials() {
    }

    static Saml2X509Credential toSigningCredential(String certificatePem, String privateKeyPem) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(
                new ByteArrayInputStream(certificatePem.getBytes(StandardCharsets.US_ASCII)));

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            PrivateKey privateKey = keyFactory.generatePrivate(
                new PKCS8EncodedKeySpec(fromPem(PRIVATE_KEY_LABEL, privateKeyPem)));

            return Saml2X509Credential.signing(privateKey, certificate);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to read the SAML signing credential", exception);
        }
    }

    private static byte[] fromPem(String label, String pem) {
        String base64 = pem.replace("-----BEGIN " + label + "-----", "")
            .replace("-----END " + label + "-----", "")
            .replaceAll("\\s", "");

        return Base64.getDecoder()
            .decode(base64);
    }
}
