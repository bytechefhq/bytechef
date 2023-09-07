
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

package com.bytechef.test.config.testcontainers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;

/**
 * @author Ivica Cardic
 */
@TestConfiguration(proxyBeanMethods = false)
public class RedisContainerConfiguration {

    @Bean
    public GenericContainer<?> redisContainer(DynamicPropertyRegistry registry) {
        GenericContainer<?> container = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

        registry.add("spring.data.redis.host=", container::getHost);
        registry.add("spring.data.redis.port=", () -> container.getMappedPort(6379));

        return container;
    }
}
