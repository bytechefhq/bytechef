/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AwsMessageBrokerEnvironmentPostProcessorTest {

    private static final String DATA_REDIS_AUTO_CONFIGURATION =
        "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration";

    private final AwsMessageBrokerEnvironmentPostProcessor awsMessageBrokerEnvironmentPostProcessor =
        new AwsMessageBrokerEnvironmentPostProcessor();

    @Test
    void testControlProviderRedisEnablesRedisAutoConfiguration() {
        MockEnvironment environment = environment()
            .withProperty("bytechef.message-broker.aws.control-provider", "redis");

        awsMessageBrokerEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.cloud.aws.sqs.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("spring.autoconfigure.exclude")).doesNotContain(
            DATA_REDIS_AUTO_CONFIGURATION);
        assertThat(environment.getProperty("management.health.redis.enabled")).isEqualTo("true");
    }

    @Test
    void testControlProviderAbsentKeepsRedisAutoConfigurationExcluded() {
        MockEnvironment environment = environment();

        awsMessageBrokerEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.cloud.aws.sqs.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("spring.autoconfigure.exclude")).contains(DATA_REDIS_AUTO_CONFIGURATION);
    }

    @Test
    void testControlProviderRedisIgnoredForOtherProviders() {
        MockEnvironment environment = environment()
            .withProperty("bytechef.message-broker.provider", "kafka")
            .withProperty("bytechef.message-broker.aws.control-provider", "redis");

        awsMessageBrokerEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.autoconfigure.exclude")).contains(DATA_REDIS_AUTO_CONFIGURATION);
    }

    private static MockEnvironment environment() {
        MockEnvironment environment = new MockEnvironment();

        environment.withProperty("bytechef.message-broker.provider", "aws");
        environment.withProperty(
            "spring.autoconfigure.exclude",
            "com.example.Other," + DATA_REDIS_AUTO_CONFIGURATION);

        return environment;
    }
}
