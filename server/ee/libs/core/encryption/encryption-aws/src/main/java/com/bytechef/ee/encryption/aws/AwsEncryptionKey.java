/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.encryption.aws;

import com.bytechef.encryption.AbstractEncryptionKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

/**
 * @version ee
 *
 * @author Ivica Caardic
 */
public final class AwsEncryptionKey extends AbstractEncryptionKey {

    private static final Logger log = LoggerFactory.getLogger(AwsEncryptionKey.class);

    private static final String KEY = "encryption-key";

    private String key;
    private final SecretsManagerClient secretsManagerClient;

    @SuppressFBWarnings("EI")
    public AwsEncryptionKey(SecretsManagerClient secretsManagerClient) {
        this.secretsManagerClient = secretsManagerClient;

        key = getSecretValue();

        if (key == null) {
            createSecret(secretsManagerClient);

            key = getSecretValue();
        }
    }

    @Override
    protected String doGetKey() {
        return key;
    }

    private void createSecret(SecretsManagerClient secretsManagerClient) {
        CreateSecretRequest key;

        try {
            key = CreateSecretRequest.builder()
                .name(KEY)
                .secretString(generateKey())
                .build();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        CreateSecretResponse secret = secretsManagerClient.createSecret(key);

        if (log.isInfoEnabled()) {
            log.info("Secret created: Version Id: {}", secret.versionId());
        }
    }

    private String getSecretValue() {
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(KEY)
                .build();

            GetSecretValueResponse secretValue = secretsManagerClient.getSecretValue(request);

            return secretValue.secretString();
        } catch (ResourceNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Encryption key not found");
            }
        }

        return null;
    }
}
