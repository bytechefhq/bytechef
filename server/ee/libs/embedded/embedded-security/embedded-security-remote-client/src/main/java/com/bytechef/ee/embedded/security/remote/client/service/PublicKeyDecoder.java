/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.remote.client.service;

import com.google.protobuf.ByteString;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

/**
 * @version ee
 */
final class PublicKeyDecoder {

    private PublicKeyDecoder() {
    }

    static PublicKey decode(ByteString encoded) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(new X509EncodedKeySpec(encoded.toByteArray()));
        } catch (GeneralSecurityException generalSecurityException) {
            throw new IllegalStateException("Invalid public key", generalSecurityException);
        }
    }
}
