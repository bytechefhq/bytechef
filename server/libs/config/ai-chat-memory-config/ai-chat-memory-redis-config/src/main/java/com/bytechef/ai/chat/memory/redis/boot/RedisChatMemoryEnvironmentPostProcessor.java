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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * @author Ivica Cardic
 */
public class RedisChatMemoryEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> source = new HashMap<>();

        if (Objects.equals(environment.getProperty("bytechef.ai.memory.provider", String.class), "redis")) {
            source.computeIfPresent(
                "spring.autoconfigure.exclude",
                (k, v) -> ((String) v).replace(
                    "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,", ""));
        }

        if (!Objects.equals(environment.getProperty("bytechef.ai.memory.provider", String.class), "redis")) {
            source.put(
                "spring.autoconfigure.exclude",
                StringUtils.join(
                    environment.getProperty("spring.autoconfigure.exclude"),
                    ", org.springframework.ai.model.chat.memory.redis.autoconfigure.RedisChatMemoryAutoConfiguration"));
        }

        MapPropertySource mapPropertySource =
            new MapPropertySource("Memory provider JDBC initialization Config", source);

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addFirst(mapPropertySource);
    }
}
