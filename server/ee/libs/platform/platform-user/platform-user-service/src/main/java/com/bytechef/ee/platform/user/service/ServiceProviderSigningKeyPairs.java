/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Generates the key pair ByteChef signs SAML authentication requests with: an RSA key and a self-signed certificate per
 * identity provider, which the service provider metadata publishes for the IdP to trust.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ServiceProviderSigningKeyPairs {

    static final String COMMON_NAME = "ByteChef SAML Service Provider";
    static final Duration VALIDITY = Duration.ofDays(3650);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private ServiceProviderSigningKeyPairs() {
    }

    static PemKeyPair generate() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048, SECURE_RANDOM);

            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            X500Name subject = new X500Name("CN=" + COMMON_NAME);
            Instant now = Instant.now();

            JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                subject, new BigInteger(64, SECURE_RANDOM), Date.from(now.minus(Duration.ofDays(1))),
                Date.from(now.plus(VALIDITY)), subject, keyPair.getPublic());

            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());

            X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

            X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certificateHolder);

            return new PemKeyPair(
                toPem("CERTIFICATE", certificate.getEncoded()),
                toPem("PRIVATE KEY", keyPair.getPrivate()
                    .getEncoded()));
        } catch (GeneralSecurityException | OperatorCreationException exception) {
            throw new IllegalStateException("Unable to generate a SAML signing key pair", exception);
        }
    }

    private static String toPem(String label, byte[] encoded) {
        Base64.Encoder encoder = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII));

        return "-----BEGIN " + label + "-----\n" + encoder.encodeToString(encoded) + "\n-----END " + label + "-----\n";
    }

    record PemKeyPair(String certificate, String privateKey) {

        @Override
        public String toString() {
            return "PemKeyPair{certificate='" + certificate + "', privateKey='[REDACTED]'}";
        }
    }
}
