/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import com.bytechef.ee.message.broker.aws.AwsMessageBroker;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.route.MessageRoute;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @version ee
 *
 * @author Marko Krikovic
 */
@SpringBootTest(properties = "bytechef.message-broker.provider=aws")
@Testcontainers
class AwsMessageBrokerListenerRegistrarConfigurationIntTest {

    private static final String QUEUE_NAME = "awsTest";
    private static final String MESSAGE = "Hello World";

    @Container
    private static final LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0"));

    private static final MessageRoute MESSAGE_ROUTE = new MessageRoute() {

        @Override
        public Exchange getExchange() {
            return Exchange.MESSAGE;
        }

        @Override
        public String getName() {
            return QUEUE_NAME;
        }
    };

    @Autowired
    private AwsMessageBroker awsMessageBroker;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.sqs.endpoint", () -> String.valueOf(localStack.getEndpointOverride(SQS)));
    }

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", QUEUE_NAME);
    }

    @Test
    void testMessageListenerEndpoint() {
        awsMessageBroker.send(MESSAGE_ROUTE, new Event(MESSAGE));

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> assertThat(TestDelegate.message).isEqualTo(MESSAGE));
    }

    record Event(String message) {
    }

    static class TestDelegate {

        public static String message;

        @SuppressFBWarnings("ST")
        public void onEvent(Event event) {
            message = event.message();
        }
    }

    @Configuration
    @Import({
        AwsMessageBrokerConfiguration.class, AwsMessageBrokerListenerRegistrarConfiguration.class
    })
    @EnableAutoConfiguration
    static class AwsMessageBrokerIntTestConfiguration {

        @Bean
        MessageBrokerConfigurer<?> messageBrokerConfigurer() {
            return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> messageBrokerListenerRegistrar
                .registerListenerEndpoint(listenerEndpointRegistrar, MESSAGE_ROUTE, 1, new TestDelegate(), "onEvent");
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
