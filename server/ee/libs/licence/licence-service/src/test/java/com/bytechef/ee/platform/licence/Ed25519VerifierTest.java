/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class Ed25519VerifierTest {

    @Test
    void testVerifyReturnsFalseWhenPublicKeyInvalid() {
        Ed25519Verifier verifier = new Ed25519Verifier("not-valid-hex");

        assertThat(verifier.verify("any".getBytes(StandardCharsets.UTF_8), "sig".getBytes(StandardCharsets.UTF_8)))
            .isFalse();
    }

    @Test
    void testVerifyAcceptsValidSignatureAndRejectsTampered() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair keyPair = generator.generateKeyPair();

        byte[] message = "license/payload".getBytes(StandardCharsets.UTF_8);

        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keyPair.getPrivate());
        signer.update(message);

        byte[] signature = signer.sign();

        String publicKeyHex = Ed25519VerifierTestSupport.publicKeyHex(keyPair);

        Ed25519Verifier verifier = new Ed25519Verifier(publicKeyHex);

        assertThat(verifier.verify(message, signature)).isTrue();

        byte[] tampered = "license/tampered".getBytes(StandardCharsets.UTF_8);

        assertThat(verifier.verify(tampered, signature)).isFalse();
    }
}
