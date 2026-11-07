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

package com.bytechef.message.broker.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.redis.env.RedisMessageBrokerListenerRegistrarConfiguration;
import com.bytechef.message.broker.redis.listener.RedisListenerEndpointRegistrar;
import com.bytechef.message.broker.redis.serializer.RedisMessageDeserializer;
import com.bytechef.message.broker.redis.serializer.RedisMessageSerializer;
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
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class RedisMessageBrokerIntTest {

    private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(10);
    private static final String CONSUMER_GROUP = "message_event_group";
    private static final TestMessage PROBE = new TestMessage("probe");

    @Container
    private static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>("redis:8-alpine")
        .withExposedPorts(6379);

    private static LettuceConnectionFactory redisConnectionFactory;
    private static RedisMessageBroker redisMessageBroker;
    private static RedisMessageDeserializer redisMessageDeserializer;
    private static StringRedisTemplate stringRedisTemplate;

    private final List<RedisMessageBrokerListenerRegistrarConfiguration> instances = new ArrayList<>();
    private final List<String> streamNames = new ArrayList<>();

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

        TestMessage pollSkippingProbes() throws InterruptedException {
            TestMessage testMessage = poll();

            while (PROBE.equals(testMessage)) {
                testMessage = poll();
            }

            return testMessage;
        }

        int receivedCount() {
            return receivedMessages.size();
        }

        void drain() {
            receivedMessages.clear();
        }
    }

    @BeforeAll
    static void beforeAll() {
        redisConnectionFactory = new LettuceConnectionFactory(
            REDIS_CONTAINER.getHost(), REDIS_CONTAINER.getMappedPort(6379));

        redisConnectionFactory.afterPropertiesSet();

        stringRedisTemplate = new StringRedisTemplate(redisConnectionFactory);

        JsonMapper jsonMapper = JsonMapper.builder()
            .build();

        redisMessageBroker = new RedisMessageBroker(new RedisMessageSerializer(jsonMapper), stringRedisTemplate);
        redisMessageDeserializer = new RedisMessageDeserializer(jsonMapper);
    }

    @AfterAll
    static void afterAll() {
        if (redisConnectionFactory != null) {
            redisConnectionFactory.destroy();
        }
    }

    @AfterEach
    void afterEach() {
        for (RedisMessageBrokerListenerRegistrarConfiguration instance : instances) {
            instance.destroy();
        }

        instances.clear();

        if (!streamNames.isEmpty()) {
            stringRedisTemplate.delete(streamNames);
        }

        streamNames.clear();
    }

    @Test
    void testWorkQueueRouteDeliversEachMessageToExactlyOneInstanceAndAcknowledgesIt() throws InterruptedException {
        MessageRoute workRoute = workRoute();

        RecordingDelegate firstInstanceDelegate = new RecordingDelegate();
        RecordingDelegate secondInstanceDelegate = new RecordingDelegate();

        startInstance(workRoute, firstInstanceDelegate);
        startInstance(workRoute, secondInstanceDelegate);

        for (int index = 0; index < 5; index++) {
            redisMessageBroker.send(workRoute, new TestMessage("work-" + index));
        }

        await(() -> firstInstanceDelegate.receivedCount() + secondInstanceDelegate.receivedCount() == 5);

        Thread.sleep(500);

        assertThat(firstInstanceDelegate.receivedCount() + secondInstanceDelegate.receivedCount()).isEqualTo(5);

        await(() -> pendingCount(workRoute.getName()) == 0);
    }

    @Test
    void testBroadcastRouteDeliversEachMessageToEveryInstanceAndEveryDelegate() throws InterruptedException {
        MessageRoute broadcastRoute = broadcastRoute();

        RecordingDelegate firstInstanceDelegate = new RecordingDelegate();
        RecordingDelegate firstInstanceSecondDelegate = new RecordingDelegate();
        RecordingDelegate secondInstanceDelegate = new RecordingDelegate();

        startInstance(broadcastRoute, firstInstanceDelegate, firstInstanceSecondDelegate);
        startInstance(broadcastRoute, secondInstanceDelegate);

        awaitSubscribed(broadcastRoute, firstInstanceDelegate, firstInstanceSecondDelegate, secondInstanceDelegate);

        redisMessageBroker.send(broadcastRoute, new TestMessage("broadcast"));

        assertThat(firstInstanceDelegate.pollSkippingProbes()).isEqualTo(new TestMessage("broadcast"));
        assertThat(firstInstanceSecondDelegate.pollSkippingProbes()).isEqualTo(new TestMessage("broadcast"));
        assertThat(secondInstanceDelegate.pollSkippingProbes()).isEqualTo(new TestMessage("broadcast"));
    }

    @Test
    void testWorkQueueRouteDeliversEachMessageToASingleDelegateOfTheReceivingInstance() throws InterruptedException {
        MessageRoute workRoute = workRoute();

        RecordingDelegate firstDelegate = new RecordingDelegate();
        RecordingDelegate secondDelegate = new RecordingDelegate();

        startInstance(workRoute, firstDelegate, secondDelegate);

        redisMessageBroker.send(workRoute, new TestMessage("work-1"));
        redisMessageBroker.send(workRoute, new TestMessage("work-2"));

        await(() -> firstDelegate.receivedCount() + secondDelegate.receivedCount() == 2);

        Thread.sleep(500);

        assertThat(firstDelegate.receivedCount()).isEqualTo(1);
        assertThat(secondDelegate.receivedCount()).isEqualTo(1);
    }

    private MessageRoute workRoute() {
        String streamName = "test.work_events." + UUID.randomUUID();

        streamNames.add(streamName);

        return new TestMessageRoute(MessageRoute.Exchange.MESSAGE, streamName);
    }

    private static MessageRoute broadcastRoute() {
        return new TestMessageRoute(MessageRoute.Exchange.CONTROL, "test.broadcast_events." + UUID.randomUUID());
    }

    private static void awaitSubscribed(MessageRoute broadcastRoute, RecordingDelegate... delegates)
        throws InterruptedException {

        await(() -> {
            redisMessageBroker.send(broadcastRoute, PROBE);

            for (RecordingDelegate delegate : delegates) {
                if (delegate.receivedCount() == 0) {
                    return false;
                }
            }

            return true;
        });

        for (RecordingDelegate delegate : delegates) {
            delegate.drain();
        }
    }

    private void startInstance(MessageRoute messageRoute, RecordingDelegate... delegates) {
        MessageBrokerConfigurer<RedisListenerEndpointRegistrar> messageBrokerConfigurer =
            (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
                for (RecordingDelegate delegate : delegates) {
                    messageBrokerListenerRegistrar.registerListenerEndpoint(
                        listenerEndpointRegistrar, messageRoute, 1, delegate, "onMessage");
                }
            };

        RedisMessageBrokerListenerRegistrarConfiguration instance =
            new RedisMessageBrokerListenerRegistrarConfiguration(
                List.of(messageBrokerConfigurer), redisConnectionFactory, redisMessageDeserializer,
                stringRedisTemplate);

        instance.afterSingletonsInstantiated();

        instances.add(instance);
    }

    private static long pendingCount(String streamName) {
        PendingMessagesSummary pendingMessagesSummary = stringRedisTemplate.opsForStream()
            .pending(streamName, CONSUMER_GROUP);

        return pendingMessagesSummary == null ? 0 : pendingMessagesSummary.getTotalPendingMessages();
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
}
