
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

import com.bytechef.commons.util.EncodingUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Ivica Cardic
 */
@Component
public class EncryptionImpl implements Encryption {

    private final EncryptionKey encryptionKey;

    public EncryptionImpl(EncryptionKey encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    @Override
    public String decrypt(String encryptedString) {
        try {
            Cipher cipher = getCipher(Cipher.DECRYPT_MODE);

            return new String(cipher.doFinal(EncodingUtils.decodeBase64(encryptedString)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String encrypt(String content) {
        try {
            Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);

            return EncodingUtils.encodeBase64ToString(cipher.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Cipher getCipher(int encryptMode) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

        byte[] decodedKey = EncodingUtils.decodeBase64(encryptionKey.getKey());

        Key secretKey = new SecretKeySpec(Arrays.copyOf(decodedKey, 16), "AES");

        cipher.init(encryptMode, secretKey);

        return cipher;
    }
}
