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

package com.bytechef.cache.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author Ivica Cardic
 */
class RedisCacheEnvironmentPostProcessorTest {

    private static final String EXCLUDE = """
        org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration,
        org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,
        org.springframework.boot.neo4j.autoconfigure.Neo4jAutoConfiguration""";

    private final RedisCacheEnvironmentPostProcessor redisCacheEnvironmentPostProcessor =
        new RedisCacheEnvironmentPostProcessor();

    @Test
    void testPostProcessEnvironmentRemovesDataRedisAutoConfigurationExclusionForRedisProvider() {
        StandardEnvironment environment = createEnvironment("redis");

        redisCacheEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.autoconfigure.exclude"))
            .doesNotContain("DataRedisAutoConfiguration")
            .contains("OpenAiChatAutoConfiguration", "Neo4jAutoConfiguration");
        assertThat(environment.getProperty("management.health.redis.enabled", Boolean.class)).isTrue();
    }

    @Test
    void testPostProcessEnvironmentKeepsDataRedisAutoConfigurationExclusionForCaffeineProvider() {
        StandardEnvironment environment = createEnvironment("caffeine");

        redisCacheEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.autoconfigure.exclude")).contains("DataRedisAutoConfiguration");
    }

    private static StandardEnvironment createEnvironment(String cacheProvider) {
        StandardEnvironment environment = new StandardEnvironment();

        Map<String, Object> properties = new HashMap<>();

        properties.put("bytechef.cache.provider", cacheProvider);
        properties.put("spring.autoconfigure.exclude", EXCLUDE);

        environment.getPropertySources()
            .addLast(new MapPropertySource("application", properties));

        return environment;
    }
}
