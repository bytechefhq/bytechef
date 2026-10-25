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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Computes the environment properties that enable Spring Boot's Redis auto-configuration, which the default
 * configuration excludes.
 *
 * @author Ivica Cardic
 */
public final class DataRedisAutoConfigurationEnabler {

    private static final String DATA_REDIS_AUTO_CONFIGURATION =
        "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration";

    private DataRedisAutoConfigurationEnabler() {
    }

    public static Map<String, Object> getProperties(ConfigurableEnvironment environment) {
        Map<String, Object> properties = new HashMap<>();

        properties.put("management.health.redis.enabled", true);

        String exclude = environment.getProperty("spring.autoconfigure.exclude");

        if (exclude != null) {
            properties.put(
                "spring.autoconfigure.exclude",
                Arrays.stream(exclude.split(","))
                    .map(String::trim)
                    .filter(className -> !className.isEmpty() && !className.equals(DATA_REDIS_AUTO_CONFIGURATION))
                    .collect(Collectors.joining(",")));
        }

        return properties;
    }
}
