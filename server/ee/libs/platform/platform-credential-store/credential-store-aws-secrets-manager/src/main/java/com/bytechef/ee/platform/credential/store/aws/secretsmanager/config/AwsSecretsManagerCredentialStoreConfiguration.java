/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.credential.store.aws.secretsmanager.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.credential.store.aws.secretsmanager.AwsSecretsManagerCredentialStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Wires {@link AwsSecretsManagerCredentialStore} when the operator selects the AWS Secrets Manager external store
 * provider.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(
    prefix = "bytechef.credential-store.external", name = "provider", havingValue = "aws-secrets-manager")
public class AwsSecretsManagerCredentialStoreConfiguration {

    @Bean
    AwsSecretsManagerCredentialStore awsSecretsManagerCredentialStore(
        ApplicationProperties applicationProperties, ObjectMapper objectMapper,
        SecretsManagerClient secretsManagerClient) {

        return new AwsSecretsManagerCredentialStore(applicationProperties, objectMapper, secretsManagerClient);
    }
}
