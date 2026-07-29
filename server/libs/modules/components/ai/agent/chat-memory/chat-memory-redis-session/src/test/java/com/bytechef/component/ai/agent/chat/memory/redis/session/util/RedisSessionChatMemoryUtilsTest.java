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

package com.bytechef.component.ai.agent.chat.memory.redis.session.util;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.session.redis.RedisSessionRepository;
import redis.clients.jedis.JedisPooled;

/**
 * @author Ivica Cardic
 */
class RedisSessionChatMemoryUtilsTest {

    @Test
    void testGetSessionRepositoryBuildsRedisSessionRepository() {
        try (JedisPooled jedisPooled = RedisSessionChatMemoryUtils.getJedisPooled(
            MockParametersFactory.create(Map.of("host", "localhost", "port", 6379)))) {

            assertInstanceOf(JedisPooled.class, jedisPooled);
        }

        assertInstanceOf(
            RedisSessionRepository.class,
            RedisSessionChatMemoryUtils.getSessionRepository(
                MockParametersFactory.create(Map.of("host", "localhost", "port", 6379))));
    }

    @Test
    void testGetJedisPooledRequiresPasswordWhenUsernameIsProvided() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RedisSessionChatMemoryUtils.getJedisPooled(
                MockParametersFactory.create(Map.of("host", "localhost", "port", 6379, "username", "user"))));
    }
}
