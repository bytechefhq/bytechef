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
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * @author Ivica Cardic
 */
class AmqpMessageBrokerEnvironmentPostProcessorTest {

    private static final String RABBIT_AUTO_CONFIGURATION =
        "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration";

    private final AmqpMessageBrokerEnvironmentPostProcessor amqpMessageBrokerEnvironmentPostProcessor =
        new AmqpMessageBrokerEnvironmentPostProcessor();

    @Test
    void testPostProcessEnvironmentKeepsRabbitAutoConfigurationForAmqpProviderFromApplicationYaml() {
        StandardEnvironment environment = createEnvironment(
            new MapPropertySource("application", Map.of("bytechef.message-broker.provider", "amqp")));

        amqpMessageBrokerEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.health.rabbit.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("spring.autoconfigure.exclude")).isNull();
    }

    @Test
    void testPostProcessEnvironmentKeepsRabbitAutoConfigurationForAmqpProviderFromEnvironmentVariable() {
        StandardEnvironment environment = createEnvironment(
            new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                Map.of("BYTECHEF_MESSAGE_BROKER_PROVIDER", "amqp")));

        amqpMessageBrokerEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.health.rabbit.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("spring.autoconfigure.exclude")).isNull();
    }

    @Test
    void testPostProcessEnvironmentKeepsRabbitAutoConfigurationForUppercaseAmqpProvider() {
        StandardEnvironment environment = createEnvironment(
            new MapPropertySource("application", Map.of("bytechef.message-broker.provider", "AMQP")));

        amqpMessageBrokerEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.health.rabbit.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("spring.autoconfigure.exclude")).isNull();
    }

    @Test
    void testPostProcessEnvironmentExcludesRabbitAutoConfigurationForOtherProvider() {
        StandardEnvironment environment = createEnvironment(
            new MapPropertySource("application", Map.of("bytechef.message-broker.provider", "redis")));

        amqpMessageBrokerEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.health.rabbit.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.autoconfigure.exclude")).contains(RABBIT_AUTO_CONFIGURATION);
    }

    private static StandardEnvironment createEnvironment(PropertySource<?> propertySource) {
        StandardEnvironment environment = new StandardEnvironment();

        environment.getPropertySources()
            .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources()
            .addLast(propertySource);

        ConfigurationPropertySources.attach(environment);

        return environment;
    }
}
