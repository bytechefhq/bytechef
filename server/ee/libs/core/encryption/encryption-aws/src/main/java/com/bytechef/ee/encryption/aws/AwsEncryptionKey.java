/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.encryption.aws;

import com.bytechef.encryption.AbstractEncryptionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetRandomPasswordRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetRandomPasswordResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * @version ee
 *
 * @author Ivica Caardic
 */
public class AwsEncryptionKey extends AbstractEncryptionKey {

    private static final Logger log = LoggerFactory.getLogger(AwsEncryptionKey.class);
    private final SecretsManagerClient secretsManagerClient;

    public AwsEncryptionKey(SecretsManagerClient secretsManagerClient) {
        this.secretsManagerClient = secretsManagerClient;

        CreateSecretRequest bytechefKey = CreateSecretRequest.builder()
            .name("bytechef-key")
            .secretString(getRandomPassword())
            .build();

        CreateSecretResponse secret = null;
        try {
            secret = secretsManagerClient.createSecret(bytechefKey);

            if (log.isInfoEnabled()) {
                log.info("Secret created: Version Id: {}", secret.versionId());
            }
        } catch (AwsServiceException e) {
            if (log.isInfoEnabled()) {
                log.info("New Secret was not created because it already exists");
            }
        }
    }

    @Override
    protected String fetchKey() {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
            .secretId("bytechef-key")
            .build();

        GetSecretValueResponse secretValue = secretsManagerClient.getSecretValue(request);

        return secretValue.secretString();
    }

    private String getRandomPassword(){
        GetRandomPasswordRequest passwordRequest = GetRandomPasswordRequest.builder()
            .passwordLength(30L)
//            .excludeCharacters()
            .excludeLowercase(false)
            .excludeUppercase(false)
            .excludeNumbers(false)
            .excludePunctuation(true)
            .includeSpace(false)
            .build();

        GetRandomPasswordResponse randomPassword = secretsManagerClient.getRandomPassword(passwordRequest);

        return randomPassword.randomPassword();
    }
}
