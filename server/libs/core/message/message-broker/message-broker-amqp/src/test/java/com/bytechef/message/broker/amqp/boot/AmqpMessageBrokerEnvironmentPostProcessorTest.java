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

package com.bytechef.message.broker.amqp.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author Ivica Cardic
 */
class AmqpMessageBrokerEnvironmentPostProcessorTest {

    private final AmqpMessageBrokerEnvironmentPostProcessor amqpMessageBrokerEnvironmentPostProcessor =
        new AmqpMessageBrokerEnvironmentPostProcessor();

    @Test
    void testPostProcessEnvironmentKeepsRabbitAutoConfigurationForAmqpProvider() {
        StandardEnvironment environment = createEnvironment("amqp");

        amqpMessageBrokerEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.autoconfigure.exclude", ""))
            .doesNotContain("RabbitAutoConfiguration");
        assertThat(environment.getProperty("management.health.rabbit.enabled", Boolean.class)).isTrue();
    }

    @Test
    void testPostProcessEnvironmentExcludesRabbitAutoConfigurationForRedisProvider() {
        StandardEnvironment environment = createEnvironment("redis");

        amqpMessageBrokerEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.autoconfigure.exclude")).contains("RabbitAutoConfiguration");
        assertThat(environment.getProperty("management.health.rabbit.enabled", Boolean.class)).isFalse();
    }

    private static StandardEnvironment createEnvironment(String messageBrokerProvider) {
        StandardEnvironment environment = new StandardEnvironment();

        environment.getPropertySources()
            .addLast(new MapPropertySource(
                "application", Map.of("bytechef.message-broker.provider", messageBrokerProvider)));

        return environment;
    }
}
