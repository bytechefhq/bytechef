/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies Ed25519 signatures using a raw 32-byte public key distributed by Keygen as a hex string.
 *
 * <p>
 * The constructor is fail-closed: if the supplied key hex is invalid or unparseable, the stored key is left as
 * {@code null} and a warning is logged. All subsequent {@link #verify} calls will return {@code false}, ensuring EE
 * features remain locked without crashing the application context.
 *
 * @version ee
 */
public class Ed25519Verifier {

    private static final Logger log = LoggerFactory.getLogger(Ed25519Verifier.class);

    private final PublicKey publicKey;

    public Ed25519Verifier(String publicKeyHex) {
        PublicKey parsed = null;

        try {
            parsed = toPublicKey(publicKeyHex);
        } catch (Exception exception) {
            log.warn(
                "Invalid or unconfigured licence public key; licence verification disabled until a valid key is configured",
                exception);
        }

        this.publicKey = parsed;
    }

    public boolean verify(byte[] message, byte[] signature) {
        if (publicKey == null) {
            return false;
        }

        try {
            Signature verifier = Signature.getInstance("Ed25519");

            verifier.initVerify(publicKey);
            verifier.update(message);

            return verifier.verify(signature);
        } catch (Exception exception) {
            return false;
        }
    }

    private static PublicKey toPublicKey(String publicKeyHex) throws Exception {
        byte[] raw = HexFormat.of()
            .parseHex(publicKeyHex.trim());

        byte[] reversed = new byte[raw.length];

        for (int i = 0; i < raw.length; i++) {
            reversed[i] = raw[raw.length - 1 - i];
        }

        boolean xOdd = (reversed[0] & 0x80) != 0;
        reversed[0] &= (byte) 0x7F;

        BigInteger y = new BigInteger(1, reversed);

        NamedParameterSpec parameterSpec = NamedParameterSpec.ED25519;
        EdECPoint point = new EdECPoint(xOdd, y);
        EdECPublicKeySpec keySpec = new EdECPublicKeySpec(parameterSpec, point);

        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");

        return keyFactory.generatePublic(keySpec);
    }

    static byte[] reverse(byte[] input) {
        byte[] copy = Arrays.copyOf(input, input.length);

        for (int i = 0; i < copy.length / 2; i++) {
            byte tmp = copy[i];
            copy[i] = copy[copy.length - 1 - i];
            copy[copy.length - 1 - i] = tmp;
        }

        return copy;
    }
}
