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

package com.bytechef.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties.Ai.Memory.Redis;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * @author Ivica Cardic
 */
class ApplicationPropertiesTest {

    @Test
    void testBindsTheAiMemoryRedisSettingsWithoutUnknownProperties() {
        MapConfigurationPropertySource mapConfigurationPropertySource = new MapConfigurationPropertySource(
            Map.of(
                "bytechef.ai.memory.provider", "redis",
                "bytechef.ai.memory.redis.host", "redis.internal",
                "bytechef.ai.memory.redis.port", "6380",
                "bytechef.ai.memory.redis.username", "bytechef",
                "bytechef.ai.memory.redis.password", "secret",
                "bytechef.ai.memory.redis.key-prefix", "session:"));

        Binder binder = new Binder(mapConfigurationPropertySource);

        ApplicationProperties applicationProperties = binder.bind(
            "bytechef", Bindable.of(ApplicationProperties.class), new NoUnboundElementsBindHandler(BindHandler.DEFAULT))
            .get();

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        ApplicationProperties.Ai.Memory memory = ai.getMemory();

        Redis redis = memory.getRedis();

        assertThat(memory.getProvider()).isEqualTo(ApplicationProperties.Ai.Memory.Provider.REDIS);
        assertThat(redis.getHost()).isEqualTo("redis.internal");
        assertThat(redis.getPort()).isEqualTo(6380);
        assertThat(redis.getUsername()).isEqualTo("bytechef");
        assertThat(redis.getPassword()).isEqualTo("secret");
        assertThat(redis.getKeyPrefix()).isEqualTo("session:");
    }
}
