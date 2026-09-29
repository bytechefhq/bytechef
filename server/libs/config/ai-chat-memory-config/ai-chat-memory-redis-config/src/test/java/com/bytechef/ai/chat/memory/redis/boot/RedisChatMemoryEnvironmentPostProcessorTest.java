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

package com.bytechef.ai.chat.memory.redis.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.chat.memory.repository.redis.autoconfigure.RedisChatMemoryRepositoryAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author Ivica Cardic
 */
class RedisChatMemoryEnvironmentPostProcessorTest {

    private final RedisChatMemoryEnvironmentPostProcessor redisChatMemoryEnvironmentPostProcessor =
        new RedisChatMemoryEnvironmentPostProcessor();

    @Test
    void testExcludesSpringAiRedisChatMemoryRepositoryAutoConfiguration() {
        StandardEnvironment environment = new StandardEnvironment();

        redisChatMemoryEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.autoconfigure.exclude"))
            .contains(RedisChatMemoryRepositoryAutoConfiguration.class.getName());
    }

    @Test
    void testKeepsTheExistingExclusions() {
        StandardEnvironment environment = new StandardEnvironment();

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addLast(
            new MapPropertySource(
                "test", Map.of("spring.autoconfigure.exclude", "com.example.ExistingAutoConfiguration")));

        redisChatMemoryEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.autoconfigure.exclude"))
            .contains("com.example.ExistingAutoConfiguration")
            .contains(RedisChatMemoryRepositoryAutoConfiguration.class.getName());
    }
}
