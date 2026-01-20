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

package com.bytechef.encryption;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.encryption.exception.InvalidEncryptionKeyException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Encryption implementation using AES-GCM for new encryptions and backwards-compatible decryption of legacy AES-ECB
 * data.
 *
 * <p>
 * New encryptions use AES-GCM which provides:
 * <ul>
 * <li>Confidentiality (encryption)</li>
 * <li>Integrity (authentication tag prevents tampering)</li>
 * <li>No pattern leakage (random IV for each encryption)</li>
 * </ul>
 *
 * <p>
 * Legacy ECB-encrypted data (without version prefix) is still decryptable for backwards compatibility during migration.
 *
 * @author Ivica Cardic
 */
@Component
public class EncryptionImpl implements Encryption {

    private static final String GCM_VERSION_PREFIX = "v2:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EncryptionKey encryptionKey;

    public EncryptionImpl(EncryptionKey encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    @Override
    public String decrypt(String encryptedString) {
        try {
            if (encryptedString.startsWith(GCM_VERSION_PREFIX)) {
                return decryptGcm(encryptedString.substring(GCM_VERSION_PREFIX.length()));
            } else {
                // Legacy ECB decryption for backwards compatibility
                return decryptLegacyEcb(encryptedString);
            }
        } catch (BadPaddingException badPaddingException) {
            throw new InvalidEncryptionKeyException("Invalid encryption key", badPaddingException);
        } catch (InvalidEncryptionKeyException e) {
            throw e;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public String encrypt(String content) {
        try {
            return GCM_VERSION_PREFIX + encryptGcm(content);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Encrypts content using AES-GCM with a random IV. Format: base64(IV + ciphertext + auth_tag)
     */
    private String encryptGcm(String content) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];

        SECURE_RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        Key secretKey = getSecretKey();
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

        byte[] ciphertext = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

        // Combine IV and ciphertext (auth tag is appended to ciphertext by GCM)
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);

        buffer.put(iv);
        buffer.put(ciphertext);

        return EncodingUtils.base64EncodeToString(buffer.array());
    }

    /**
     * Decrypts content encrypted with AES-GCM. Expected format: base64(IV + ciphertext + auth_tag)
     */
    private String decryptGcm(String encryptedString) throws Exception {
        byte[] encryptedData = EncodingUtils.base64Decode(encryptedString);

        // Extract IV
        byte[] iv = Arrays.copyOfRange(encryptedData, 0, GCM_IV_LENGTH);

        // Extract ciphertext (includes auth tag)
        byte[] ciphertext = Arrays.copyOfRange(encryptedData, GCM_IV_LENGTH, encryptedData.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        Key secretKey = getSecretKey();
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    /**
     * Decrypts content encrypted with legacy AES-ECB mode. Kept for backwards compatibility with existing encrypted
     * data.
     */
    @SuppressFBWarnings({
        "CIPHER_INTEGRITY", "ECB_MODE"
    })
    private String decryptLegacyEcb(String encryptedString) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        Key secretKey = getSecretKey();

        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        return new String(cipher.doFinal(EncodingUtils.base64Decode(encryptedString)), StandardCharsets.UTF_8);
    }

    private Key getSecretKey() {
        byte[] decodedKey = EncodingUtils.base64Decode(encryptionKey.getKey());

        return new SecretKeySpec(Arrays.copyOf(decodedKey, 16), "AES");
    }
}
