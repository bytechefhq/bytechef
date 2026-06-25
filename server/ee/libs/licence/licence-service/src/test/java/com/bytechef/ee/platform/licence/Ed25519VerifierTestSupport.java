/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence;

import java.security.KeyPair;
import java.security.interfaces.EdECPublicKey;
import java.util.HexFormat;

/**
 * Shared test helper that encodes an Ed25519 {@link KeyPair}'s public key as the raw 32-byte hex string expected by
 * {@link Ed25519Verifier} (and distributed by Keygen).
 *
 * @version ee
 */
class Ed25519VerifierTestSupport {

    private Ed25519VerifierTestSupport() {
    }

    /**
     * Returns the raw 32-byte little-endian hex encoding of the public key from the given key pair. This is the format
     * distributed by Keygen and consumed by {@link Ed25519Verifier}.
     */
    static String publicKeyHex(KeyPair keyPair) {
        EdECPublicKey edECPublicKey = (EdECPublicKey) keyPair.getPublic();

        byte[] yReversed = edECPublicKey.getPoint()
            .getY()
            .toByteArray();

        byte[] raw = new byte[32];

        for (int i = 0; i < yReversed.length && i < 32; i++) {
            raw[i] = yReversed[yReversed.length - 1 - i];
        }

        if (edECPublicKey.getPoint()
            .isXOdd()) {
            raw[31] |= (byte) 0x80;
        }

        return HexFormat.of()
            .formatHex(raw);
    }
}
