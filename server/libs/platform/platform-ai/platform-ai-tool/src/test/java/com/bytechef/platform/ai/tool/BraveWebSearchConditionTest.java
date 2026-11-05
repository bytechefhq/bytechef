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

package com.bytechef.platform.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
class BraveWebSearchConditionTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(BraveConfiguration.class);

    @Test
    void testMatchesWhenEnabledAndApiKeySet() {
        applicationContextRunner
            .withPropertyValues("bytechef.ai.brave.enabled=true", "bytechef.ai.brave.api-key=test-key")
            .run(context -> assertThat(context).hasBean("braveMarker"));
    }

    @Test
    void testDoesNotMatchWhenDisabled() {
        applicationContextRunner
            .withPropertyValues("bytechef.ai.brave.enabled=false", "bytechef.ai.brave.api-key=test-key")
            .run(context -> assertThat(context).doesNotHaveBean("braveMarker"));
    }

    @Test
    void testDoesNotMatchWhenEnabledPropertyMissing() {
        applicationContextRunner
            .withPropertyValues("bytechef.ai.brave.api-key=test-key")
            .run(context -> assertThat(context).doesNotHaveBean("braveMarker"));
    }

    @Test
    void testDoesNotMatchWhenApiKeyMissing() {
        applicationContextRunner
            .withPropertyValues("bytechef.ai.brave.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean("braveMarker"));
    }

    @Test
    void testDoesNotMatchWhenApiKeyBlank() {
        applicationContextRunner
            .withPropertyValues("bytechef.ai.brave.enabled=true", "bytechef.ai.brave.api-key= ")
            .run(context -> assertThat(context).doesNotHaveBean("braveMarker"));
    }

    @Configuration(proxyBeanMethods = false)
    static class BraveConfiguration {

        @Bean
        @Conditional(BraveWebSearchCondition.class)
        String braveMarker() {
            return "brave";
        }
    }
}
