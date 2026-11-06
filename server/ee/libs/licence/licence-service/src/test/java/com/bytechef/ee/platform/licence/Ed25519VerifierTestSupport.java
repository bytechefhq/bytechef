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
 * @version ee
 */
class Ed25519VerifierTestSupport {

    private Ed25519VerifierTestSupport() {
    }

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
