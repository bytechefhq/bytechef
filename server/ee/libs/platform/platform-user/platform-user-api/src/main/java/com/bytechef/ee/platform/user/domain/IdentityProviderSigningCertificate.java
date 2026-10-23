/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.domain;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * The certificate a SAML identity provider signs its responses with, as an administrator pastes it: PEM, or the bare
 * base64 body an IdP console often shows. When set, it is the only certificate assertions are verified against.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class IdentityProviderSigningCertificate {

    private static final String BEGIN = "-----BEGIN CERTIFICATE-----";
    private static final String END = "-----END CERTIFICATE-----";

    private IdentityProviderSigningCertificate() {
    }

    /**
     * Returns the certificate as canonical PEM, or throws {@link IllegalArgumentException} if it is not one X.509
     * certificate.
     */
    public static String normalize(String certificate) {
        X509Certificate x509Certificate = parse(certificate);

        try {
            Base64.Encoder encoder = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII));

            return BEGIN + "\n" + encoder.encodeToString(x509Certificate.getEncoded()) + "\n" + END + "\n";
        } catch (CertificateEncodingException certificateEncodingException) {
            throw new IllegalArgumentException(
                "signingCertificate is not a valid X.509 certificate", certificateEncodingException);
        }
    }

    public static X509Certificate parse(String certificate) {
        String base64 = certificate.replace(BEGIN, "")
            .replace(END, "")
            .replaceAll("\\s", "");

        try {
            byte[] encoded = Base64.getDecoder()
                .decode(base64);

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(encoded));
        } catch (CertificateException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("signingCertificate is not a valid X.509 certificate", exception);
        }
    }
}
