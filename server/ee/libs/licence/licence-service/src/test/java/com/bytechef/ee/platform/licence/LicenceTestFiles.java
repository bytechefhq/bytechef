/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.StringJoiner;

/**
 * @version ee
 */
class LicenceTestFiles {

    private final KeyPair keyPair;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    LicenceTestFiles() {
        this.keyPair = generateKeyPair();
    }

    String publicKeyHex() {
        return Ed25519VerifierTestSupport.publicKeyHex(keyPair);
    }

    byte[] signLicence(String licenceId, String expiryIso, long allowedJobs, String... features) {
        StringJoiner featureArray = new StringJoiner("\",\"", "[\"", "\"]");

        for (String feature : features) {
            featureArray.add(feature);
        }

        String featuresJson = (features.length == 0) ? "[]" : featureArray.toString();

        String dataset = """
            {"data":{"id":"%s","attributes":{"expiry":"%s","created":"2026-01-01T00:00:00.000Z",\
            "metadata":{"allowedJobs":%d,"features":%s}}}}"""
            .formatted(licenceId, expiryIso, allowedJobs, featuresJson);

        return signDataset(dataset);
    }

    byte[] signDataset(String dataset) {
        try {
            String enc = Base64.getEncoder()
                .encodeToString(dataset.getBytes(StandardCharsets.UTF_8));

            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(keyPair.getPrivate());
            signer.update(("license/" + enc).getBytes(StandardCharsets.UTF_8));

            String sig = Base64.getEncoder()
                .encodeToString(signer.sign());

            String envelope =
                "{\"enc\":\"" + enc + "\",\"sig\":\"" + sig + "\",\"alg\":\"base64+ed25519\"}";
            String armored = "-----BEGIN LICENSE FILE-----\n"
                + Base64.getEncoder()
                    .encodeToString(envelope.getBytes(StandardCharsets.UTF_8))
                + "\n-----END LICENSE FILE-----\n";

            return armored.getBytes(StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    byte[] signDatasetWithCorruptedSignature(String dataset) {
        try {
            String enc = Base64.getEncoder()
                .encodeToString(dataset.getBytes(StandardCharsets.UTF_8));

            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(keyPair.getPrivate());
            signer.update(("license/" + enc).getBytes(StandardCharsets.UTF_8));

            byte[] sigBytes = signer.sign();
            sigBytes[0] ^= 0xFF;

            String sig = Base64.getEncoder()
                .encodeToString(sigBytes);

            String envelope =
                "{\"enc\":\"" + enc + "\",\"sig\":\"" + sig + "\",\"alg\":\"base64+ed25519\"}";
            String armored = "-----BEGIN LICENSE FILE-----\n"
                + Base64.getEncoder()
                    .encodeToString(envelope.getBytes(StandardCharsets.UTF_8))
                + "\n-----END LICENSE FILE-----\n";

            return armored.getBytes(StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519")
                .generateKeyPair();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
