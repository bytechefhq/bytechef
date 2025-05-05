/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.helper.util;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.Option;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Nikolina Spehar
 */
public class CryptoHelperUtil {

    private static final Base64.Decoder DECODER = Base64.getDecoder();

    private CryptoHelperUtil() {
    }

    public static String convertBytesToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }

    public static List<Option<String>> getHashAlgorithmOptions() {
        return Arrays.stream(CryptographicAlgorithm.values())
            .map(algorithm -> option(algorithm.getName(), algorithm.getName()))
            .collect(Collectors.toList());
    }

    public static List<Option<String>> getHmacAlgorithmOptions() {
        return Arrays.stream(CryptographicAlgorithm.values())
            .map(algorithm -> option(algorithm.getName(), algorithm.getLabel()))
            .collect(Collectors.toList());
    }

    public static PublicKey getPublicRSAKeyFromString(String key) throws Exception {
        String publicKeyPEM = key
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(DECODER.decode(publicKeyPEM));

        return keyFactory.generatePublic(keySpec);
    }

    public static PrivateKey getPrivateRSAKeyFromString(String pemKey) throws Exception {
        String privateKeyPEM = pemKey
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", ""); // Remove all whitespace

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(DECODER.decode(privateKeyPEM));

        return keyFactory.generatePrivate(keySpec);
    }
}
