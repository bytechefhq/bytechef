/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import com.bytechef.ee.message.broker.aws.AwsMessageBroker;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.redis.control.RedisControlChannel;
import com.bytechef.message.broker.redis.serializer.RedisMessageDeserializer;
import com.bytechef.message.route.MessageRoute;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    properties = {
        "bytechef.message-broker.provider=aws", "bytechef.message-broker.aws.control-provider=redis"
    })
@Testcontainers
@Import({
    AwsMessageBrokerConfiguration.class, AwsMessageBrokerListenerRegistrarConfiguration.class,
    JacksonConfiguration.class
})
class AwsMessageBrokerRedisControlIntTest {

    private static final String CONTROL_QUEUE_NAME = "awsRedisControlTest";
    private static final String QUEUE_NAME = "awsRedisMessageTest";
    private static final String MESSAGE = "Hello World";

    @Container
    private static final LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0"));

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

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

    private static final MessageRoute CONTROL_MESSAGE_ROUTE = new MessageRoute() {

        @Override
        public Exchange getExchange() {
            return Exchange.CONTROL;
        }

        @Override
        public String getName() {
            return CONTROL_QUEUE_NAME;
        }
    };

    private static final CountDownLatch CONTEXT_CONTROL_LATCH = new CountDownLatch(1);
    private static final CountDownLatch MESSAGE_LATCH = new CountDownLatch(1);
    private static final CountDownLatch UNEXPECTED_SECOND_MESSAGE_LATCH = new CountDownLatch(1);

    @Autowired
    private AwsMessageBroker awsMessageBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.sqs.endpoint", () -> String.valueOf(localStack.getEndpointOverride(SQS)));
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", QUEUE_NAME);
    }

    @Test
    void testControlMessageReachesEveryNode() throws InterruptedException {
        CountDownLatch otherNodeLatch = new CountDownLatch(1);

        RedisControlChannel otherNodeControlChannel = new RedisControlChannel(
            redisConnectionFactory, new RedisMessageDeserializer(objectMapper));

        otherNodeControlChannel.register(CONTROL_MESSAGE_ROUTE, new OtherNodeDelegate(otherNodeLatch), "onEvent");
        otherNodeControlChannel.start();

        try {
            // Subscriptions are established asynchronously, so send until both nodes have received the message or
            // the deadline passes.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);

            while ((CONTEXT_CONTROL_LATCH.getCount() > 0 || otherNodeLatch.getCount() > 0) &&
                System.nanoTime() < deadline) {

                awsMessageBroker.send(CONTROL_MESSAGE_ROUTE, new Event(MESSAGE));

                TimeUnit.MILLISECONDS.sleep(200);
            }

            assertThat(CONTEXT_CONTROL_LATCH.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(otherNodeLatch.await(1, TimeUnit.SECONDS)).isTrue();
        } finally {
            otherNodeControlChannel.stop();
        }
    }

    @Test
    void testMessageRouteIsConsumedOnce() throws InterruptedException {
        awsMessageBroker.send(MESSAGE_ROUTE, new Event(MESSAGE));

        assertThat(MESSAGE_LATCH.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(UNEXPECTED_SECOND_MESSAGE_LATCH.await(2, TimeUnit.SECONDS)).isFalse();
    }

    record Event(String message) {
    }

    static class ContextControlDelegate {

        public void onEvent(Event event) {
            CONTEXT_CONTROL_LATCH.countDown();
        }
    }

    static class OtherNodeDelegate {

        private final CountDownLatch countDownLatch;

        OtherNodeDelegate(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        public void onEvent(Event event) {
            countDownLatch.countDown();
        }
    }

    static class MessageDelegate {

        public void onEvent(Event event) {
            if (MESSAGE_LATCH.getCount() == 0) {
                UNEXPECTED_SECOND_MESSAGE_LATCH.countDown();
            }

            MESSAGE_LATCH.countDown();
        }
    }

    @Configuration
    @EnableAutoConfiguration
    static class AwsMessageBrokerRedisControlIntTestConfiguration {

        @Bean
        MessageBrokerConfigurer<?> messageBrokerConfigurer() {
            return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> messageBrokerListenerRegistrar
                .registerListenerEndpoint(listenerEndpointRegistrar, MESSAGE_ROUTE, 1, new MessageDelegate(),
                    "onEvent");
        }

        @Bean
        MessageBrokerConfigurer<?> controlMessageBrokerConfigurer() {
            return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> messageBrokerListenerRegistrar
                .registerListenerEndpoint(
                    listenerEndpointRegistrar, CONTROL_MESSAGE_ROUTE, 1, new ContextControlDelegate(), "onEvent");
        }
    }
}
