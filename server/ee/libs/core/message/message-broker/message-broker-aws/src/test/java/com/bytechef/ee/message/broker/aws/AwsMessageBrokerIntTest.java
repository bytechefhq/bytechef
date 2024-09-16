/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ee.message.broker.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import com.bytechef.ee.message.broker.aws.config.AwsMessageBrokerListenerRegistrarConfiguration;
import com.bytechef.message.route.MessageRoute;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
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

@SpringBootTest
@Testcontainers
class AwsMessageBrokerIntTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0"));

    static final String QUEUE_NAME = "awsTest";
    static final String MESSAGE = "Hello World";

    private static MessageRoute route;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.cloud.aws.region.static",
            () -> localStack.getRegion());
        registry.add(
            "spring.cloud.aws.credentials.access-key",
            () -> localStack.getAccessKey());
        registry.add(
            "spring.cloud.aws.credentials.secret-key",
            () -> localStack.getSecretKey());
        registry.add(
            "spring.cloud.aws.sqs.endpoint",
            () -> localStack.getEndpointOverride(SQS)
                .toString());
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

//    @Test
//    void canRegisterListenerEndpoints() {
//        EndpointRegistrar listenerEndpointRegistrar = new EndpointRegistrar();
//        int concurrency = 1;
//        TestClass testClass = new TestClass();
//        String methodName = "testMethod";
//
//        awsMessageBrokerListenerRegistrarConfiguration.registerListenerEndpoint(listenerEndpointRegistrar, route,
//            concurrency, testClass, methodName);
//
//        await()
//            .pollInterval(Duration.ofSeconds(2))
//            .atMost(Duration.ofSeconds(10))
//            .ignoreExceptions()
//            .untilAsserted(() -> {
//                awsMessageBroker.send(route, MESSAGE);
//
//                sqsTemplate.receiveAsync(from -> from.queue(QUEUE_NAME));
//
//                assertThat(TestClass.message).isEqualTo(MESSAGE);
////                assertThat(stringMessage.get().getPayload()).isEqualTo(MESSAGE);
//            });
//    }

    private class TestClass {
        public static String message;

        public void testMethod() {
            message = MESSAGE;
//            awsMessageBroker.send(route, MESSAGE);
        }
    }

    @Autowired
    AwsMessageBroker awsMessageBroker;

    @Autowired
    AwsMessageBrokerListenerRegistrarConfiguration awsMessageBrokerListenerRegistrarConfiguration;

    @Autowired
    SqsTemplate sqsTemplate;

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

        @Bean
        AwsMessageBrokerListenerRegistrarConfiguration awsMessageBrokerListenerRegistrarConfiguration(
            BeanFactory beanFactory, MessageHandlerMethodFactory messageHandlerMethodFactory,
            SqsMessageListenerContainerFactory sqsMessageListenerContainerFactory) {
            return new AwsMessageBrokerListenerRegistrarConfiguration(beanFactory, null, messageHandlerMethodFactory,
                sqsMessageListenerContainerFactory);
        }

    }
}
