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

package com.bytechef.config.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.boot.support.EnvironmentPostProcessorsFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author Ivica Cardic
 */
class AiMemoryProviderEnvironmentPostProcessorTest {

    private final AiMemoryProviderEnvironmentPostProcessor aiMemoryProviderEnvironmentPostProcessor =
        new AiMemoryProviderEnvironmentPostProcessor();

    @Test
    void testRewritesEveryBindableSpellingToTheCanonicalLowercaseName() {
        assertThat(postProcess("REDIS")).isEqualTo("redis");
        assertThat(postProcess("Aws")).isEqualTo("aws");
        assertThat(postProcess("in-memory")).isEqualTo("in_memory");
        assertThat(postProcess("IN_MEMORY")).isEqualTo("in_memory");
        assertThat(postProcess("inmemory")).isEqualTo("in_memory");
        assertThat(postProcess("jdbc")).isEqualTo("jdbc");
    }

    @Test
    void testAnUnknownProviderFailsToBind() {
        assertThatThrownBy(() -> postProcess("mongodb"))
            .isInstanceOf(BindException.class)
            .hasMessageContaining(AiMemoryProviderEnvironmentPostProcessor.PROVIDER_PROPERTY);
    }

    @Test
    void testIsRegisteredAsASpringFactoriesEnvironmentPostProcessor() {
        EnvironmentPostProcessorsFactory environmentPostProcessorsFactory =
            EnvironmentPostProcessorsFactory.fromSpringFactories(getClass().getClassLoader());

        List<EnvironmentPostProcessor> environmentPostProcessors =
            environmentPostProcessorsFactory.getEnvironmentPostProcessors(
                new DeferredLogs(), new DefaultBootstrapContext());

        assertThat(environmentPostProcessors)
            .hasAtLeastOneElementOfType(AiMemoryProviderEnvironmentPostProcessor.class);
    }

    @Test
    void testLeavesAnUnsetProviderUnset() {
        StandardEnvironment environment = new StandardEnvironment();

        aiMemoryProviderEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(AiMemoryProviderEnvironmentPostProcessor.PROVIDER_PROPERTY)).isNull();
    }

    @Test
    void testRunsAfterTheConfigurationFilesAreLoadedAndBeforeUnorderedPostProcessors() {
        assertThat(aiMemoryProviderEnvironmentPostProcessor.getOrder())
            .isGreaterThan(ConfigDataEnvironmentPostProcessor.ORDER)
            .isLessThan(Ordered.LOWEST_PRECEDENCE);
    }

    private String postProcess(String provider) {
        StandardEnvironment environment = new StandardEnvironment();

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addLast(
            new MapPropertySource(
                "test", Map.of(AiMemoryProviderEnvironmentPostProcessor.PROVIDER_PROPERTY, provider)));

        aiMemoryProviderEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        return environment.getProperty(AiMemoryProviderEnvironmentPostProcessor.PROVIDER_PROPERTY);
    }
}
