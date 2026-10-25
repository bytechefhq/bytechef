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

package com.bytechef.platform.webhook.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
class ConditionalOnWebhookTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(WebhookOnlyConfiguration.class);

    @Test
    void testMatchesWhenPropertyIsMissing() {
        applicationContextRunner.run(context -> assertThat(context).hasBean("webhookMarker"));
    }

    @Test
    void testMatchesWhenEnabled() {
        applicationContextRunner.withPropertyValues("bytechef.webhook.enabled=true")
            .run(context -> assertThat(context).hasBean("webhookMarker"));
    }

    @Test
    void testDoesNotMatchWhenDisabled() {
        applicationContextRunner.withPropertyValues("bytechef.webhook.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean("webhookMarker"));
    }

    @Configuration
    @ConditionalOnWebhook
    static class WebhookOnlyConfiguration {

        @Bean
        String webhookMarker() {
            return "webhook";
        }
    }
}
