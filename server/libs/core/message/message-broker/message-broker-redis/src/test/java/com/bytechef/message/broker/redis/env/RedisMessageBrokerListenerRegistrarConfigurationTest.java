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

package com.bytechef.message.broker.redis.env;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.message.broker.redis.serializer.RedisMessageDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author Ivica Cardic
 */
class RedisMessageBrokerListenerRegistrarConfigurationTest {

    private final RedisMessageBrokerListenerRegistrarConfiguration redisMessageBrokerListenerRegistrarConfiguration =
        new RedisMessageBrokerListenerRegistrarConfiguration(
            null, mock(RedisConnectionFactory.class), mock(RedisMessageDeserializer.class),
            mock(StringRedisTemplate.class));

    @Test
    void testPhaseIsHigherThanConnectionFactoryPhase() {
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory();

        assertThat(redisMessageBrokerListenerRegistrarConfiguration.getPhase())
            .isGreaterThan(lettuceConnectionFactory.getPhase());
    }

    @Test
    void testStopStopsTheRegistrar() {
        redisMessageBrokerListenerRegistrarConfiguration.afterSingletonsInstantiated();

        assertThat(redisMessageBrokerListenerRegistrarConfiguration.isRunning()).isTrue();

        redisMessageBrokerListenerRegistrarConfiguration.stop();

        assertThat(redisMessageBrokerListenerRegistrarConfiguration.isRunning()).isFalse();
    }
}
