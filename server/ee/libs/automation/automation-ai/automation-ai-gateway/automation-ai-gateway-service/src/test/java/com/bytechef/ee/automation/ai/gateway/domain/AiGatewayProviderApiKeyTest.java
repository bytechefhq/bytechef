/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.data.jdbc.converter.EncryptedStringWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToEncryptedStringWrapperConverter;
import com.bytechef.commons.data.jdbc.wrapper.EncryptedStringWrapper;
import com.bytechef.encryption.EncryptionImpl;
import org.junit.jupiter.api.Test;

/**
 * Exercises the JDBC-level encryption converters that {@link AiGatewayProvider#setApiKey(String)} depends on. The full
 * integration path (service → repository → Postgres) relies on Spring Data wiring these converters into its
 * {@code JdbcCustomConversions} bean; this unit test proves the symmetry at the converter boundary itself so a
 * regression in the encryption algorithm, padding, or base64 encoding is caught even when the test-harness wiring
 * happens to skip the converters (as the current {@link AiGatewayIntTestConfiguration} does).
 *
 * @version ee
 */
class AiGatewayProviderApiKeyTest {

    // Same key used by the int-test config — not a real secret.
    private static final String TEST_KEY = "tTB1/UBIbYLuCXVi4PPfzA==";

    @Test
    void testEncryptDecryptRoundTripProducesOriginalPlaintext() {
        EncryptionImpl encryption = new EncryptionImpl(() -> TEST_KEY);

        StringToEncryptedStringWrapperConverter reader = new StringToEncryptedStringWrapperConverter(encryption);
        EncryptedStringWrapperToStringConverter writer = new EncryptedStringWrapperToStringConverter(encryption);

        String plaintextApiKey = "sk-provider-api-key-abc123";
        EncryptedStringWrapper wrapper = new EncryptedStringWrapper(plaintextApiKey);

        String persistedCiphertext = writer.convert(wrapper);

        assertThat(persistedCiphertext)
            .as("ciphertext must not equal plaintext — otherwise column-at-rest encryption is a no-op")
            .isNotEqualTo(plaintextApiKey);

        EncryptedStringWrapper roundTripped = reader.convert(persistedCiphertext);

        assertThat(roundTripped)
            .as("reader must return a non-null wrapper for a non-null input")
            .isNotNull();
        assertThat(roundTripped.getValue())
            .as("decrypted plaintext must equal the original secret")
            .isEqualTo(plaintextApiKey);
    }

    @Test
    void testApiKeyWrapperRedactsToString() {
        ApiKey apiKey = ApiKey.of("very-secret-" + System.nanoTime());

        assertThat(apiKey.toString())
            .as("ApiKey.toString must never leak the plaintext into logs, error messages, or toast notifications")
            .isEqualTo("***");

        assertThat(apiKey.reveal())
            .as("reveal() is the one sanctioned escape hatch; it must return the plaintext intact")
            .startsWith("very-secret-");
    }

    @Test
    void testNullOrBlankPlaintextYieldsNullApiKey() {
        assertThat(ApiKey.ofNullable(null)).isNull();
        assertThat(ApiKey.ofNullable("")).isNull();
        assertThat(ApiKey.ofNullable("  ")).isNull();
    }
}
