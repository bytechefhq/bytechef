/*
 * Copyright 2025 ByteChef
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

package com.bytechef.message.broker.amqp.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.message.broker.amqp.AmqpMessageBroker;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.route.MessageRoute;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class AmqpMessageBrokerIntTest {

    private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(10);

    @Container
    private static final GenericContainer<?> RABBITMQ_CONTAINER = new GenericContainer<>("rabbitmq:4-alpine")
        .withExposedPorts(5672)
        .waitingFor(Wait.forLogMessage(".*Server startup complete.*\\n", 1))
        .withStartupTimeout(Duration.ofMinutes(3));

    private static AmqpMessageBroker amqpMessageBroker;
    private static CachingConnectionFactory connectionFactory;
    private static MessageConverter messageConverter;
    private static RabbitAdmin rabbitAdmin;

    private final List<RabbitListenerEndpointRegistry> registries = new ArrayList<>();

    record TestMessageRoute(Exchange exchange, String routeName) implements MessageRoute {

        @Override
        public Exchange getExchange() {
            return exchange;
        }

        @Override
        public String getName() {
            return routeName;
        }
    }

    public record TestMessage(String value) {
    }

    public static class RecordingDelegate {

        private final BlockingQueue<TestMessage> receivedMessages = new LinkedBlockingQueue<>();

        public void onMessage(TestMessage testMessage) {
            receivedMessages.add(testMessage);
        }

        TestMessage poll() throws InterruptedException {
            return receivedMessages.poll(DELIVERY_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }

        int receivedCount() {
            return receivedMessages.size();
        }
    }

    @BeforeAll
    static void beforeAll() {
        connectionFactory = new CachingConnectionFactory(
            RABBITMQ_CONTAINER.getHost(), RABBITMQ_CONTAINER.getMappedPort(5672));

        AmqpMessageBrokerConfiguration amqpMessageBrokerConfiguration = new AmqpMessageBrokerConfiguration();

        messageConverter = amqpMessageBrokerConfiguration.jacksonAmqpMessageConverter(JsonMapper.builder()
            .build());

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(messageConverter);

        amqpMessageBroker = amqpMessageBrokerConfiguration.amqpMessageBroker(rabbitTemplate);
        rabbitAdmin = amqpMessageBrokerConfiguration.rabbitAdmin(connectionFactory);
    }

    @AfterAll
    static void afterAll() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @AfterEach
    void afterEach() {
        for (RabbitListenerEndpointRegistry registry : registries) {
            registry.stop();
        }

        registries.clear();
    }

    @Test
    void testWorkQueueRouteDeliversEachMessageToExactlyOneInstance() throws InterruptedException {
        MessageRoute workRoute = workRoute();

        RecordingDelegate firstInstanceDelegate = new RecordingDelegate();
        RecordingDelegate secondInstanceDelegate = new RecordingDelegate();

        startInstance(workRoute, firstInstanceDelegate);
        startInstance(workRoute, secondInstanceDelegate);

        for (int index = 0; index < 5; index++) {
            amqpMessageBroker.send(workRoute, new TestMessage("work-" + index));
        }

        await(() -> firstInstanceDelegate.receivedCount() + secondInstanceDelegate.receivedCount() == 5);

        Thread.sleep(500);

        assertThat(firstInstanceDelegate.receivedCount() + secondInstanceDelegate.receivedCount()).isEqualTo(5);
    }

    @Test
    void testBroadcastRouteDeliversEachMessageToEveryInstanceAndEveryDelegate() throws InterruptedException {
        MessageRoute broadcastRoute = broadcastRoute();

        RecordingDelegate firstInstanceDelegate = new RecordingDelegate();
        RecordingDelegate firstInstanceSecondDelegate = new RecordingDelegate();
        RecordingDelegate secondInstanceDelegate = new RecordingDelegate();

        startInstance(broadcastRoute, firstInstanceDelegate, firstInstanceSecondDelegate);
        startInstance(broadcastRoute, secondInstanceDelegate);

        amqpMessageBroker.send(broadcastRoute, new TestMessage("broadcast"));

        assertThat(firstInstanceDelegate.poll()).isEqualTo(new TestMessage("broadcast"));
        assertThat(firstInstanceSecondDelegate.poll()).isEqualTo(new TestMessage("broadcast"));
        assertThat(secondInstanceDelegate.poll()).isEqualTo(new TestMessage("broadcast"));
    }

    @Test
    void testTwoListenersOnOneWorkQueueRouteInOneInstanceRegisterWithoutClashing() throws InterruptedException {
        MessageRoute workRoute = workRoute();

        RecordingDelegate firstDelegate = new RecordingDelegate();
        RecordingDelegate secondDelegate = new RecordingDelegate();

        startInstance(workRoute, firstDelegate, secondDelegate);

        amqpMessageBroker.send(workRoute, new TestMessage("work"));

        await(() -> firstDelegate.receivedCount() + secondDelegate.receivedCount() == 1);

        Thread.sleep(500);

        assertThat(firstDelegate.receivedCount() + secondDelegate.receivedCount()).isEqualTo(1);
    }

    private void startInstance(MessageRoute messageRoute, RecordingDelegate... delegates) {
        MessageBrokerConfigurer<RabbitListenerEndpointRegistrar> messageBrokerConfigurer =
            (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
                for (RecordingDelegate delegate : delegates) {
                    messageBrokerListenerRegistrar.registerListenerEndpoint(
                        listenerEndpointRegistrar, messageRoute, 1, delegate, "onMessage");
                }
            };

        RabbitListenerEndpointRegistry registry = new RabbitListenerEndpointRegistry();

        AmqpMessageBrokerListenerRegistrarConfiguration instance = new AmqpMessageBrokerListenerRegistrarConfiguration(
            connectionFactory, messageConverter, List.of(messageBrokerConfigurer), rabbitAdmin, new RabbitProperties(),
            registry);

        RabbitListenerEndpointRegistrar listenerEndpointRegistrar = new RabbitListenerEndpointRegistrar();

        listenerEndpointRegistrar.setEndpointRegistry(registry);

        instance.configureRabbitListeners(listenerEndpointRegistrar);

        listenerEndpointRegistrar.afterPropertiesSet();

        registry.start();

        registries.add(registry);
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        Instant deadline = Instant.now()
            .plus(DELIVERY_TIMEOUT);

        while (!condition.getAsBoolean()) {
            if (Instant.now()
                .isAfter(deadline)) {

                throw new AssertionError("Condition not met within " + DELIVERY_TIMEOUT);
            }

            Thread.sleep(50);
        }
    }

    private static MessageRoute workRoute() {
        return new TestMessageRoute(MessageRoute.Exchange.MESSAGE, "test.work_events." + UUID.randomUUID());
    }

    private static MessageRoute broadcastRoute() {
        return new TestMessageRoute(MessageRoute.Exchange.CONTROL, "test.broadcast_events." + UUID.randomUUID());
    }
}
