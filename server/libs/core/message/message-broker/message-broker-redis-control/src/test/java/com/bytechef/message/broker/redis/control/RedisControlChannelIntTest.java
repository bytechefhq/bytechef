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

package com.bytechef.message.broker.redis.control;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.message.broker.redis.serializer.RedisMessageDeserializer;
import com.bytechef.message.broker.redis.serializer.RedisMessageSerializer;
import com.bytechef.message.route.MessageRoute;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class RedisControlChannelIntTest {

    private static final String CHANNEL_NAME = "sse.sse_stream_events";

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

    private final ObjectMapper objectMapper = JsonMapper.builder()
        .build();
    private LettuceConnectionFactory lettuceConnectionFactory;
    private RedisControlChannel nodeAControlChannel;
    private RedisControlChannel nodeBControlChannel;

    @BeforeEach
    void beforeEach() {
        lettuceConnectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));

        lettuceConnectionFactory.afterPropertiesSet();
        lettuceConnectionFactory.start();

        RedisMessageDeserializer redisMessageDeserializer = new RedisMessageDeserializer(objectMapper);

        nodeAControlChannel = new RedisControlChannel(lettuceConnectionFactory, redisMessageDeserializer);
        nodeBControlChannel = new RedisControlChannel(lettuceConnectionFactory, redisMessageDeserializer);
    }

    @AfterEach
    void afterEach() {
        nodeAControlChannel.stop();
        nodeBControlChannel.stop();

        lettuceConnectionFactory.destroy();
    }

    @Test
    void testMessageIsReceivedByEveryNode() throws InterruptedException {
        CountDownLatch nodeALatch = new CountDownLatch(1);
        CountDownLatch nodeBLatch = new CountDownLatch(1);
        MessageRoute messageRoute = new TestMessageRoute();

        nodeAControlChannel.register(messageRoute, new CountingDelegate(nodeALatch), "handle");
        nodeBControlChannel.register(messageRoute, new CountingDelegate(nodeBLatch), "handle");

        nodeAControlChannel.start();
        nodeBControlChannel.start();

        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(lettuceConnectionFactory);

        stringRedisTemplate.afterPropertiesSet();

        RedisMessageSerializer redisMessageSerializer = new RedisMessageSerializer(objectMapper);

        // Subscriptions are established asynchronously after start, so publish until the deadline passes or both
        // nodes have received the message.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

        while ((nodeALatch.getCount() > 0 || nodeBLatch.getCount() > 0) && System.nanoTime() < deadline) {
            stringRedisTemplate.convertAndSend(CHANNEL_NAME, redisMessageSerializer.serialize(new TestEvent("event")));

            TimeUnit.MILLISECONDS.sleep(200);
        }

        assertThat(nodeALatch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(nodeBLatch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    public record TestEvent(String name) {
    }

    @SuppressWarnings("unused")
    private static class CountingDelegate {

        private final CountDownLatch countDownLatch;

        private CountingDelegate(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        public void handle(TestEvent testEvent) {
            countDownLatch.countDown();
        }
    }

    private static class TestMessageRoute implements MessageRoute {

        @Override
        public Exchange getExchange() {
            return Exchange.CONTROL;
        }

        @Override
        public String getName() {
            return CHANNEL_NAME;
        }
    }
}
