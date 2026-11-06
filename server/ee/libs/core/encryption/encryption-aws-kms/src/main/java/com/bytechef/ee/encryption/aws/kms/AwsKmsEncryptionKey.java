/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.encryption.aws.kms;

import com.bytechef.encryption.AbstractEncryptionKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

/**
 * @version ee
 * @author Ivica Cardic
 */
public class AwsKmsEncryptionKey extends AbstractEncryptionKey {

    private static final Logger log = LoggerFactory.getLogger(AwsKmsEncryptionKey.class);

    private final String dataKeyPlaintextBase64;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public AwsKmsEncryptionKey(KmsClient kmsClient, String keyId, Path dataKeyPath) {
        try {
            if (Files.exists(dataKeyPath)) {
                this.dataKeyPlaintextBase64 = decryptExisting(kmsClient, dataKeyPath);
            } else {
                this.dataKeyPlaintextBase64 = generateAndPersist(kmsClient, keyId, dataKeyPath);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize AWS KMS encryption key", e);
        }
    }

    @Override
    protected String doGetKey() {
        return dataKeyPlaintextBase64;
    }

    private static String decryptExisting(KmsClient kmsClient, Path dataKeyPath) throws Exception {
        byte[] ciphertext = Files.readAllBytes(dataKeyPath);

        DecryptResponse response = kmsClient.decrypt(
            DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(ciphertext))
                .build());

        SdkBytes plaintext = response.plaintext();

        log.info("Recovered AWS KMS data key from {}", dataKeyPath);

        return Base64.getEncoder()
            .encodeToString(plaintext.asByteArray());
    }

    private static String generateAndPersist(KmsClient kmsClient, String keyId, Path dataKeyPath) throws Exception {
        GenerateDataKeyResponse response = kmsClient.generateDataKey(
            GenerateDataKeyRequest.builder()
                .keyId(keyId)
                .keySpec(DataKeySpec.AES_256)
                .build());

        SdkBytes plaintext = response.plaintext();
        SdkBytes ciphertext = response.ciphertextBlob();

        Path parent = dataKeyPath.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.write(dataKeyPath, ciphertext.asByteArray());

        log.info("Generated and persisted new AWS KMS data key at {}", dataKeyPath);

        return Base64.getEncoder()
            .encodeToString(plaintext.asByteArray());
    }
}
