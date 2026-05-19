/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.encryption.aws.kms.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.encryption.aws.kms.AwsKmsEncryptionKey;
import com.bytechef.encryption.EncryptionKey;
import java.net.URI;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.encryption", name = "provider", havingValue = "aws-kms")
public class AwsKmsEncryptionConfiguration {

    @Bean
    KmsClient kmsClient(
        AwsCredentialsProvider awsCredentialsProvider, AwsRegionProvider awsRegionProvider,
        ApplicationProperties applicationProperties) {

        ApplicationProperties.Encryption.AwsKms awsKms = applicationProperties.getEncryption()
            .getAwsKms();

        KmsClientBuilder builder = KmsClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(awsRegionProvider.getRegion());

        String endpoint = awsKms.getEndpoint();

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    @Bean
    EncryptionKey encryptionKey(KmsClient kmsClient, ApplicationProperties applicationProperties) {
        ApplicationProperties.Encryption.AwsKms awsKms = applicationProperties.getEncryption()
            .getAwsKms();

        return new AwsKmsEncryptionKey(kmsClient, awsKms.getKeyId(), Path.of(awsKms.getDataKeyPath()));
    }
}
