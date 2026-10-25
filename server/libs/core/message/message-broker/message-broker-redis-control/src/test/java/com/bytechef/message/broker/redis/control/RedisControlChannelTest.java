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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.message.broker.redis.serializer.RedisMessageDeserializer;
import com.bytechef.message.route.MessageRoute;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * @author Ivica Cardic
 */
class RedisControlChannelTest {

    private static final String CHANNEL_NAME = "sse.sse_stream_events";
    private static final String RAW_MESSAGE = "raw-message";

    private final List<Object> firstReceivedEvents = new ArrayList<>();
    private final RedisMessageDeserializer redisMessageDeserializer = mock(RedisMessageDeserializer.class);
    private final List<Object> secondReceivedEvents = new ArrayList<>();
    private RedisControlChannel redisControlChannel;

    @BeforeEach
    void beforeEach() {
        when(redisMessageDeserializer.deserialize(RAW_MESSAGE)).thenReturn("deserialized-event");

        redisControlChannel = new RedisControlChannel(mock(RedisConnectionFactory.class), redisMessageDeserializer);
    }

    @Test
    void testMessageIsDispatchedToEveryDelegateOfTheChannel() {
        MessageRoute messageRoute = new TestMessageRoute();

        redisControlChannel.register(messageRoute, new FirstDelegate(), "handle");
        redisControlChannel.register(messageRoute, new SecondDelegate(), "handle");

        redisControlChannel.onMessage(CHANNEL_NAME, RAW_MESSAGE);

        assertThat(firstReceivedEvents).containsExactly("deserialized-event");
        assertThat(secondReceivedEvents).containsExactly("deserialized-event");
    }

    @Test
    void testChannelIsSubscribedOnceForTwoDelegates() {
        MessageRoute messageRoute = new TestMessageRoute();

        redisControlChannel.register(messageRoute, new FirstDelegate(), "handle");
        redisControlChannel.register(messageRoute, new SecondDelegate(), "handle");

        assertThat(redisControlChannel.getSubscribedChannelNames()).isEqualTo(Set.of(CHANNEL_NAME));
    }

    @Test
    void testMessageOnUnknownChannelIsIgnored() {
        redisControlChannel.register(new TestMessageRoute(), new FirstDelegate(), "handle");

        redisControlChannel.onMessage("unknown.channel", RAW_MESSAGE);

        assertThat(firstReceivedEvents).isEmpty();
    }

    @Test
    void testDelegateExceptionDoesNotStopOtherDelegates() {
        MessageRoute messageRoute = new TestMessageRoute();

        redisControlChannel.register(messageRoute, new FailingDelegate(), "handle");
        redisControlChannel.register(messageRoute, new SecondDelegate(), "handle");

        redisControlChannel.onMessage(CHANNEL_NAME, RAW_MESSAGE);

        assertThat(secondReceivedEvents).containsExactly("deserialized-event");
    }

    @SuppressWarnings("unused")
    private class FirstDelegate {

        public void handle(Object event) {
            firstReceivedEvents.add(event);
        }
    }

    @SuppressWarnings("unused")
    private class SecondDelegate {

        public void handle(Object event) {
            secondReceivedEvents.add(event);
        }
    }

    @SuppressWarnings("unused")
    private static class FailingDelegate {

        public void handle(Object event) {
            throw new IllegalStateException("boom");
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
