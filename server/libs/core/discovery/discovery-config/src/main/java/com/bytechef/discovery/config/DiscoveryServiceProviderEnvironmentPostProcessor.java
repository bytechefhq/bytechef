/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.discovery.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * @author Ivica Cardic
 */
public class DiscoveryServiceProviderEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> source = new HashMap<>();

        String discoveryServiceProvider = environment.getProperty("bytechef.discovery-service.provider", String.class);

        source.put("spring.cloud.consul.enabled", false);
        source.put("spring.cloud.redis.enabled", false);

        if (Objects.equals(discoveryServiceProvider, "consul")) {
            source.put("spring.cloud.consul.enabled", true);
        } else {
            source.put("spring.cloud.redis.enabled", true);
        }

        MapPropertySource mapPropertySource = new MapPropertySource("Custom Spring Cloud Config", source);

        environment.getPropertySources().addFirst(mapPropertySource);
    }
}
