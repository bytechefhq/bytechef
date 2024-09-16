/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import com.bytechef.message.route.MessageRoute;
import io.awspring.cloud.sqs.config.EndpointRegistrar;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
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
@SpringBootTest
@Testcontainers
class AwsMessageBrokerIntTest {

    private static final String QUEUE_NAME = "awsTest";
    private static final String MESSAGE = "Hello World";

    @Container
    private static final LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0"));

    private static MessageRoute route;

    @Autowired
    private AwsMessageBroker awsMessageBroker;

    @Autowired
    private SqsTemplate sqsTemplate;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.sqs.endpoint", () -> String.valueOf(localStack.getEndpointOverride(SQS)));
    }

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        route = new MessageRoute() {
            @Override
            public Exchange getExchange() {
                return Exchange.MESSAGE;
            }

            @Override
            public String getName() {
                return QUEUE_NAME;
            }
        };

        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", QUEUE_NAME);
    }

    @Test
    void canHandleMessage() {
        awsMessageBroker.send(route, MESSAGE);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                Optional<Message<String>> stringMessage =
                    sqsTemplate.receive(from -> from.queue(QUEUE_NAME), String.class);

                assertThat(stringMessage.get()
                    .getPayload()).isEqualTo(MESSAGE);
            });
    }

    @Disabled
    @Test
    void canRegisterListenerEndpoints() {
        EndpointRegistrar listenerEndpointRegistrar = new EndpointRegistrar();
        int concurrency = 1;
        TestClass testClass = new TestClass();
        String methodName = "testMethod";

//        awsMessageBrokerListenerRegistrarConfiguration.registerListenerEndpoint(
//            listenerEndpointRegistrar, route, concurrency, testClass, methodName);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                awsMessageBroker.send(route, MESSAGE);

                sqsTemplate.receiveAsync(from -> from.queue(QUEUE_NAME));

                assertThat(TestClass.message).isEqualTo(MESSAGE);
//                assertThat(stringMessage.get().getPayload()).isEqualTo(MESSAGE);
            });
    }

    private static class TestClass {

        public static String message;
    }

    @Configuration
    @ComponentScan("io.awspring.cloud")
    @EnableAutoConfiguration
    static class AwsMessageBrokerIntTestConfiguration {

        @Bean
        AwsMessageBroker awsMessageBroker(SqsTemplate sqsTemplate) {
            return new AwsMessageBroker(sqsTemplate);
        }

        @Bean
        MessageHandlerMethodFactory messageHandlerMethodFactory(MessageConverter messageConverter) {
            DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
            messageHandlerMethodFactory.setMessageConverter(messageConverter);

            return messageHandlerMethodFactory;
        }

        @Bean
        MessageConverter messageConverter() {
            var converter = new MappingJackson2MessageConverter();
            converter.setSerializedPayloadClass(String.class);
            converter.setStrictContentTypeMatch(false);
            return converter;
        }
    }
}
