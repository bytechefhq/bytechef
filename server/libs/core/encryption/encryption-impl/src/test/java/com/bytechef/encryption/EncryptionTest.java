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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for encryption implementation including GCM mode and legacy ECB backwards compatibility.
 *
 * @author Ivica Cardic
 */
class EncryptionTest {

    private static final Encryption ENCRYPTION = new EncryptionImpl(() -> "tTB1/UBIbYLuCXVi4PPfzA==");

    @Test
    void testEncryptDecryptRoundtrip() {
        String originalText = "test text";

        String encrypted = ENCRYPTION.encrypt(originalText);
        String decrypted = ENCRYPTION.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(originalText);
    }

    @Test
    void testEncryptedValueHasGcmPrefix() {
        String encrypted = ENCRYPTION.encrypt("text");

        assertThat(encrypted).startsWith("v2:");
    }

    @Test
    void testEncryptProducesDifferentOutputsForSameInput() {
        // GCM uses random IV, so same input should produce different ciphertext
        String encrypted1 = ENCRYPTION.encrypt("same text");
        String encrypted2 = ENCRYPTION.encrypt("same text");

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    void testDecryptLegacyEcbFormat() {
        // Legacy ECB-encrypted value (without v2: prefix)
        String legacyEncrypted = "EQuGMfU8kiNQIxJ/Y0xoeg==";

        String decrypted = ENCRYPTION.decrypt(legacyEncrypted);

        assertThat(decrypted).isEqualTo("text");
    }

    @Test
    void testEncryptDecryptEmptyString() {
        String encrypted = ENCRYPTION.encrypt("");
        String decrypted = ENCRYPTION.decrypt(encrypted);

        assertThat(decrypted).isEmpty();
    }

    @Test
    void testEncryptDecryptLongText() {
        String longText = "a".repeat(10000);

        String encrypted = ENCRYPTION.encrypt(longText);
        String decrypted = ENCRYPTION.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(longText);
    }

    @Test
    void testEncryptDecryptSpecialCharacters() {
        String specialChars = "Hello 世界 🎉 <>&\"'\\n\\t";

        String encrypted = ENCRYPTION.encrypt(specialChars);
        String decrypted = ENCRYPTION.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(specialChars);
    }

    @Test
    void testEncryptDecryptJsonContent() {
        String jsonContent = "{\"key\": \"value\", \"number\": 123, \"nested\": {\"array\": [1, 2, 3]}}";

        String encrypted = ENCRYPTION.encrypt(jsonContent);
        String decrypted = ENCRYPTION.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(jsonContent);
    }
}
