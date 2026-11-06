/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.encryption.aws.kms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AwsKmsEncryptionKeyTest {

    @Test
    void testFirstBootGeneratesAndPersists(@TempDir Path tempDir) throws IOException {
        Path dataKeyPath = tempDir.resolve("aws-kms-data-key");

        byte[] plaintextBytes = "32-byte-test-aes-256-data-key!!!".getBytes(StandardCharsets.UTF_8);
        byte[] ciphertextBytes = "fake-kms-ciphertext-blob".getBytes(StandardCharsets.UTF_8);

        KmsClient kmsClient = mock(KmsClient.class);

        when(kmsClient.generateDataKey(any(GenerateDataKeyRequest.class))).thenReturn(
            GenerateDataKeyResponse.builder()
                .plaintext(SdkBytes.fromByteArray(plaintextBytes))
                .ciphertextBlob(SdkBytes.fromByteArray(ciphertextBytes))
                .build());

        AwsKmsEncryptionKey key = new AwsKmsEncryptionKey(kmsClient, "alias/test", dataKeyPath);

        verify(kmsClient).generateDataKey(any(GenerateDataKeyRequest.class));

        assertThat(dataKeyPath).exists();
        assertThat(Files.readAllBytes(dataKeyPath)).isEqualTo(ciphertextBytes);
        assertThat(key.getKey()).isEqualTo(Base64.getEncoder()
            .encodeToString(plaintextBytes));
    }

    @Test
    void testSubsequentBootReadsAndDecrypts(@TempDir Path tempDir) throws IOException {
        Path dataKeyPath = tempDir.resolve("aws-kms-data-key");

        byte[] ciphertextBytes = "fake-kms-ciphertext-blob".getBytes(StandardCharsets.UTF_8);
        byte[] plaintextBytes = "32-byte-test-aes-256-data-key!!!".getBytes(StandardCharsets.UTF_8);

        Files.write(dataKeyPath, ciphertextBytes);

        KmsClient kmsClient = mock(KmsClient.class);

        when(kmsClient.decrypt(any(DecryptRequest.class))).thenReturn(
            DecryptResponse.builder()
                .plaintext(SdkBytes.fromByteArray(plaintextBytes))
                .build());

        AwsKmsEncryptionKey key = new AwsKmsEncryptionKey(kmsClient, "alias/test", dataKeyPath);

        verify(kmsClient).decrypt(any(DecryptRequest.class));

        assertThat(key.getKey()).isEqualTo(Base64.getEncoder()
            .encodeToString(plaintextBytes));
    }
}
