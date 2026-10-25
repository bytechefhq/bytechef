/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

import com.bytechef.message.broker.redis.control.RedisControlChannel;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AwsMessageBrokerConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(AwsMessageBrokerConfiguration.class, SupportConfiguration.class)
        .withPropertyValues(
            "bytechef.message-broker.provider=aws", "bytechef.message-broker.aws.control-provider=redis");

    @Test
    void testRedisControlChannelIsDestroyedWhenApplicationContextCloses() {
        try (MockedConstruction<RedisControlChannel> redisControlChannelConstruction =
            mockConstruction(RedisControlChannel.class)) {

            applicationContextRunner.run(context -> context.getBean("awsRedisControlChannel"));

            assertThat(redisControlChannelConstruction.constructed()).hasSize(1);

            verify(redisControlChannelConstruction.constructed()
                .getFirst()).destroy();
        }
    }

    @Configuration
    static class SupportConfiguration {

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder()
                .build();
        }

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }

        @Bean
        SqsTemplate sqsTemplate() {
            return mock(SqsTemplate.class);
        }
    }
}
