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

package com.bytechef.message.broker.redis.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.message.broker.redis.serializer.RedisMessageDeserializer;
import com.bytechef.message.route.MessageRoute;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author Ivica Cardic
 */
class RedisListenerEndpointRegistrarTest {

    private static final String CHANNEL_NAME = "test_channel";
    private static final String CONSUMER_GROUP = "message_event_group";
    private static final String STREAM_NAME = "test_stream";

    private final List<Object> firstReceivedMessages = new ArrayList<>();
    private final RedisMessageDeserializer redisMessageDeserializer = mock(RedisMessageDeserializer.class);
    private final List<Object> secondReceivedMessages = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private final StreamOperations<String, Object, Object> streamOperations = mock(StreamOperations.class);

    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private RedisListenerEndpointRegistrar redisListenerEndpointRegistrar;

    @BeforeEach
    void beforeEach() {
        doReturn(streamOperations).when(stringRedisTemplate)
            .opsForStream();

        redisListenerEndpointRegistrar = new RedisListenerEndpointRegistrar(
            redisMessageDeserializer, stringRedisTemplate, mock(TaskExecutor.class));
    }

    @Test
    void testEveryDelegateOfAControlRouteReceivesEveryMessage() {
        MessageRoute controlMessageRoute = new TestMessageRoute(MessageRoute.Exchange.CONTROL, CHANNEL_NAME);

        redisListenerEndpointRegistrar.registerListenerEndpoint(
            controlMessageRoute, new FirstMessageHandler(), "handle");
        redisListenerEndpointRegistrar.registerListenerEndpoint(
            controlMessageRoute, new SecondMessageHandler(), "handle");

        when(redisMessageDeserializer.deserialize("raw-message")).thenReturn("deserialized-payload");

        redisListenerEndpointRegistrar.onMessage(
            new DefaultMessage(
                CHANNEL_NAME.getBytes(StandardCharsets.UTF_8), "raw-message".getBytes(StandardCharsets.UTF_8)),
            null);

        assertThat(firstReceivedMessages).containsExactly("deserialized-payload");
        assertThat(secondReceivedMessages).containsExactly("deserialized-payload");

        verify(streamOperations, never()).createGroup(CHANNEL_NAME, CONSUMER_GROUP);
    }

    @Test
    void testMessageOnAChannelWithoutDelegatesIsDropped() {
        redisListenerEndpointRegistrar.onMessage(
            new DefaultMessage(
                "unknown_channel".getBytes(StandardCharsets.UTF_8), "raw-message".getBytes(StandardCharsets.UTF_8)),
            null);

        assertThat(firstReceivedMessages).isEmpty();
        assertThat(secondReceivedMessages).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testEachStreamRecordIsDeliveredToASingleDelegate() {
        RedisListenerEndpointRegistrar streamRedisListenerEndpointRegistrar = new RedisListenerEndpointRegistrar(
            redisMessageDeserializer, stringRedisTemplate, Runnable::run);

        MessageRoute streamMessageRoute = new TestMessageRoute(MessageRoute.Exchange.MESSAGE, STREAM_NAME);

        streamRedisListenerEndpointRegistrar.registerListenerEndpoint(
            streamMessageRoute, new FirstMessageHandler(), "handle");
        streamRedisListenerEndpointRegistrar.registerListenerEndpoint(
            streamMessageRoute, new SecondMessageHandler(), "handle");

        when(redisMessageDeserializer.deserialize("raw-message-1")).thenReturn("payload-1");
        when(redisMessageDeserializer.deserialize("raw-message-2")).thenReturn("payload-2");

        List<MapRecord<String, Object, Object>> records = List.of(
            createStreamRecord("1-0", "raw-message-1"), createStreamRecord("2-0", "raw-message-2"));

        when(streamOperations.read(any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class)))
            .thenReturn(records)
            .thenAnswer(invocation -> {
                streamRedisListenerEndpointRegistrar.stop();

                return List.of();
            });

        streamRedisListenerEndpointRegistrar.start();

        assertThat(firstReceivedMessages).containsExactly("payload-1");
        assertThat(secondReceivedMessages).containsExactly("payload-2");

        verify(streamOperations).acknowledge(STREAM_NAME, CONSUMER_GROUP, RecordId.of("1-0"));
        verify(streamOperations).acknowledge(STREAM_NAME, CONSUMER_GROUP, RecordId.of("2-0"));
    }

    private static MapRecord<String, Object, Object> createStreamRecord(String recordId, String message) {
        MapRecord<String, Object, Object> mapRecord = MapRecord.create(STREAM_NAME, Map.of("message", message));

        return mapRecord.withId(RecordId.of(recordId));
    }

    public class FirstMessageHandler {

        public void handle(String message) {
            firstReceivedMessages.add(message);
        }
    }

    public class SecondMessageHandler {

        public void handle(String message) {
            secondReceivedMessages.add(message);
        }
    }

    private record TestMessageRoute(Exchange exchange, String routeName) implements MessageRoute {

        @Override
        public Exchange getExchange() {
            return exchange;
        }

        @Override
        public String getName() {
            return routeName;
        }
    }
}
