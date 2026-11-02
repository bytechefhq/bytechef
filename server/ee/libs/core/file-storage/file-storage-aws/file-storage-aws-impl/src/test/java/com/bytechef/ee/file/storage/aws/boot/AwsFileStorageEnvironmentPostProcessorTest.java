/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.file.storage.aws.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AwsFileStorageEnvironmentPostProcessorTest {

    private final AwsFileStorageEnvironmentPostProcessor postProcessor = new AwsFileStorageEnvironmentPostProcessor();

    @ParameterizedTest
    @ValueSource(
        strings = {
            "bytechef.ai.auto-memory.provider", "bytechef.data-storage.provider", "bytechef.file-storage.provider"
        })
    void testEnablesS3WhenProviderIsAws(String providerPropertyName) {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.cloud.aws.s3.enabled", "false")
            .withProperty("bytechef.file-storage.provider", "FILESYSTEM")
            .withProperty(providerPropertyName, "AWS");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.cloud.aws.s3.enabled", Boolean.class)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "FILESYSTEM", "JDBC"
    })
    void testKeepsS3DisabledWithoutAwsProvider(String provider) {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.cloud.aws.s3.enabled", "false")
            .withProperty("bytechef.data-storage.provider", provider)
            .withProperty("bytechef.file-storage.provider", provider);

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.cloud.aws.s3.enabled", Boolean.class)).isFalse();
    }
}
