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

package com.bytechef.ai.chat.memory.redis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author Ivica Cardic
 */
class RedisChatMemoryConfigurationTest {

    private static final Map<String, Object> BYTECHEF_DEFAULTS = Map.of(
        "bytechef.ai.memory.redis.host", "localhost", "bytechef.ai.memory.redis.port", "6379");

    @Test
    void testLegacyHostWithDefaultBytechefHostIsReported() {
        List<String> legacyPropertyWarnings = RedisChatMemoryConfiguration.getLegacyPropertyWarnings(
            environment(Map.of("spring.ai.chat.memory.redis.host", "redis.internal")));

        assertThat(legacyPropertyWarnings)
            .singleElement()
            .asString()
            .contains("spring.ai.chat.memory.redis.host=redis.internal")
            .contains("bytechef.ai.memory.redis.host=localhost")
            .contains("BYTECHEF_AI_MEMORY_REDIS_HOST");
    }

    @Test
    void testLegacyPortWithDefaultBytechefPortIsReported() {
        List<String> legacyPropertyWarnings = RedisChatMemoryConfiguration.getLegacyPropertyWarnings(
            environment(Map.of("spring.ai.chat.memory.redis.port", "6380")));

        assertThat(legacyPropertyWarnings)
            .singleElement()
            .asString()
            .contains("spring.ai.chat.memory.redis.port=6380")
            .contains("BYTECHEF_AI_MEMORY_REDIS_PORT");
    }

    @Test
    void testLegacyPropertiesAlreadyMigratedAreNotReported() {
        List<String> legacyPropertyWarnings = RedisChatMemoryConfiguration.getLegacyPropertyWarnings(
            environment(
                Map.of(
                    "spring.ai.chat.memory.redis.host", "redis.internal", "spring.ai.chat.memory.redis.port", "6380",
                    "bytechef.ai.memory.redis.host", "redis.internal", "bytechef.ai.memory.redis.port", "6380")));

        assertThat(legacyPropertyWarnings).isEmpty();
    }

    @Test
    void testNoLegacyPropertiesAreNotReported() {
        assertThat(RedisChatMemoryConfiguration.getLegacyPropertyWarnings(environment(Map.of()))).isEmpty();
    }

    @Test
    void testUsernameWithoutPasswordIsRejected() {
        RedisChatMemoryConfiguration redisChatMemoryConfiguration = new RedisChatMemoryConfiguration();

        StandardEnvironment environment = environment(Map.of("bytechef.ai.memory.redis.username", "bytechef"));

        assertThatThrownBy(() -> redisChatMemoryConfiguration.redisChatMemoryRedisClient(environment))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("bytechef.ai.memory.redis.password is required when a username is configured");
    }

    @Test
    void testUsernameWithBlankPasswordIsRejected() {
        RedisChatMemoryConfiguration redisChatMemoryConfiguration = new RedisChatMemoryConfiguration();

        StandardEnvironment environment = environment(
            Map.of("bytechef.ai.memory.redis.username", "bytechef", "bytechef.ai.memory.redis.password", " "));

        assertThatThrownBy(() -> redisChatMemoryConfiguration.redisChatMemoryRedisClient(environment))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("bytechef.ai.memory.redis.password is required when a username is configured");
    }

    private static StandardEnvironment environment(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addLast(new MapPropertySource("test", properties));
        mutablePropertySources.addLast(new MapPropertySource("application-bytechef", BYTECHEF_DEFAULTS));

        return environment;
    }
}
