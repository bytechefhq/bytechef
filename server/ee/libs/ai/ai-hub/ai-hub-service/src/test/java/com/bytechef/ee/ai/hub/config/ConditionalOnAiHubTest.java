/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.EnvironmentInfoContributor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConditionalOnAiHubTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(GatedConfiguration.class);

    @Test
    void testBeanLoadsOnlyWhenHubAndCopilotAreEnabled() {
        contextRunner.withPropertyValues("bytechef.ai.hub.enabled=true", "bytechef.ai.copilot.enabled=true")
            .run(context -> assertThat(context).hasBean("gatedBean"));
        contextRunner.withPropertyValues("bytechef.ai.hub.enabled=true", "bytechef.ai.copilot.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean("gatedBean"));
        contextRunner.withPropertyValues("bytechef.ai.hub.enabled=false", "bytechef.ai.copilot.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean("gatedBean"));
        contextRunner.run(context -> assertThat(context).doesNotHaveBean("gatedBean"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testInfoAiMapCarriesCopilotKnowledgeBaseAndHubFlags() {
        ConfigurableEnvironment environment = new StandardEnvironment();

        environment.getPropertySources()
            .addFirst(new MapPropertySource(
                "test", Map.of(
                    "bytechef.ai.copilot.enabled", "true",
                    "bytechef.ai.hub.enabled", "true",
                    "info.ai.copilot.enabled", "${bytechef.ai.copilot.enabled}",
                    "info.ai.hub.enabled", "${bytechef.ai.hub.enabled:false}",
                    "info.ai.knowledgeBase.enabled", "${bytechef.ai.knowledge-base.enabled:false}")));

        Info.Builder builder = new Info.Builder();

        new EnvironmentInfoContributor(environment).contribute(builder);

        Map<String, Object> ai = (Map<String, Object>) builder.build()
            .getDetails()
            .get("ai");

        assertThat(ai).containsOnlyKeys("copilot", "hub", "knowledgeBase");
        assertThat((Map<String, Object>) ai.get("hub")).containsEntry("enabled", "true");
        assertThat((Map<String, Object>) ai.get("copilot")).containsEntry("enabled", "true");
        assertThat((Map<String, Object>) ai.get("knowledgeBase")).containsEntry("enabled", "false");
    }

    @Test
    void testSecondContributorWritingTheAiKeyReplacesTheWholeMap() {
        Info.Builder builder = new Info.Builder();

        builder.withDetail("ai", Map.of("copilot", Map.of("enabled", "true")));
        builder.withDetail("ai", Map.of("hub", Map.of("enabled", "true")));

        assertThat(builder.build()
            .getDetails()
            .get("ai")).isEqualTo(Map.of("hub", Map.of("enabled", "true")));
    }

    @Configuration
    @ConditionalOnAiHub
    static class GatedConfiguration {

        @Bean
        String gatedBean() {
            return "gated";
        }
    }
}
