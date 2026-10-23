/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.saml2;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
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
import org.springframework.security.saml2.core.Saml2X509Credential;

/**
 * Creates and reads the key pair ByteChef signs SAML authentication requests with. Each identity provider gets its own
 * RSA key and self-signed certificate, which the service provider metadata publishes for the IdP to trust.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ServiceProviderSigningCredentials {

    static final Duration VALIDITY = Duration.ofDays(3650);

    private static final String CERTIFICATE_LABEL = "CERTIFICATE";
    private static final String PRIVATE_KEY_LABEL = "PRIVATE KEY";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private ServiceProviderSigningCredentials() {
    }

    static PemCredential generate(String commonName) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048, SECURE_RANDOM);

            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            X500Name subject = new X500Name("CN=" + commonName);
            Instant now = Instant.now();

            JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                subject, new BigInteger(64, SECURE_RANDOM), Date.from(now.minus(Duration.ofDays(1))),
                Date.from(now.plus(VALIDITY)), subject, keyPair.getPublic());

            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());

            X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

            X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certificateHolder);

            return new PemCredential(
                toPem(CERTIFICATE_LABEL, certificate.getEncoded()),
                toPem(PRIVATE_KEY_LABEL, keyPair.getPrivate()
                    .getEncoded()));
        } catch (GeneralSecurityException | OperatorCreationException exception) {
            throw new IllegalStateException("Unable to generate a SAML signing credential", exception);
        }
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

    private static String toPem(String label, byte[] encoded) {
        Base64.Encoder encoder = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII));

        return "-----BEGIN " + label + "-----\n" + encoder.encodeToString(encoded) + "\n-----END " + label + "-----\n";
    }

    record PemCredential(String certificate, String privateKey) {

        @Override
        public String toString() {
            return "PemCredential{certificate='" + certificate + "', privateKey='[REDACTED]'}";
        }
    }
}
