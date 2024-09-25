/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.encryption.aws;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SECRETSMANAGER;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * @version ee
 *
 * @author Marko Krikovic
 */
@SpringBootTest
@Testcontainers
class AwsEncryptionKeyIntTest {
    @Container
    private static final LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0"));

    @Autowired
    private AwsEncryptionKey storageService;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.secretsmanager.endpoint",
            () -> String.valueOf(localStack.getEndpointOverride(SECRETSMANAGER)));
    }

    @Test
    void canFetchKey() {
        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                String key = storageService.fetchKey();

                assertEquals(30, key.length());
                assertFalse(key.contains(" "));
            });
    }

    @Test
    void canFetchKeyIfAlreadyExists() {
        SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
            .credentialsProvider(() -> AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey()))
            .region(Region.US_EAST_1)
            .endpointOverride(localStack.getEndpointOverride(SECRETSMANAGER))
            .build();

        AwsEncryptionKey awsEncryptionKey = new AwsEncryptionKey(secretsManagerClient);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                String key1 = storageService.fetchKey();
                String key2 = awsEncryptionKey.fetchKey();

                assertEquals(30, key1.length());
                assertEquals(30, key2.length());
                assertEquals(key1, key2);
            });
    }

    @Configuration
    @ComponentScan("io.awspring.cloud")
    @EnableAutoConfiguration
    static class AwsEncryptionKeyIntTestConfiguration {
        @Bean
        AwsEncryptionKey awsEncryptionKey() {
            SecretsManagerClient client = SecretsManagerClient.builder()
                .credentialsProvider(
                    () -> AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey()))
                .region(Region.US_EAST_1)
                .endpointOverride(localStack.getEndpointOverride(SECRETSMANAGER))
                .build();

            return new AwsEncryptionKey(client);
        }
    }
}
