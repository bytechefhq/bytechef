
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.encryption;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Ivica Cardic
 */
public class Encryption {

    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    private final EncryptionKey encryptionKey;

    public Encryption(EncryptionKey encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String encrypt(String content) {
        try {
            Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);

            return ENCODER.encodeToString(cipher.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decrypt(String encryptedString) {
        try {
            Cipher cipher = getCipher(Cipher.DECRYPT_MODE);

            return new String(cipher.doFinal(DECODER.decode(encryptedString)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Cipher getCipher(int encryptMode) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

        byte[] decodedKey = DECODER
            .decode(encryptionKey.getKey());

        Key secretKey = new SecretKeySpec(Arrays.copyOf(decodedKey, 16), "AES");

        cipher.init(encryptMode, secretKey);

        return cipher;
    }
}
