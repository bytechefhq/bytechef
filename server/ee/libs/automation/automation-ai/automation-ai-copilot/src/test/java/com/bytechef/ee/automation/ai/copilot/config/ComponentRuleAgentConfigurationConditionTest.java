/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ee.automation.ai.tool.componentrule.ComponentRuleToolCallbacksFactory;
import com.bytechef.ee.platform.component.rule.ComponentRuleService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * @version ee
 * @author Ivica Cardic
 */
class ComponentRuleAgentConfigurationConditionTest {

    @Test
    void testActivatesWhenOnlyAiHubIsEnabled() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            addProperties(context, Map.of("bytechef.ai.hub.enabled", "true", "bytechef.edition", "ee"));

            context.register(
                ComponentRuleDependenciesConfiguration.class, ComponentRuleAgentConfiguration.class);
            context.refresh();

            assertThat(context.getBeanNamesForType(ComponentRuleToolCallbacksFactory.class)).hasSize(1);
            assertThat(context.containsBean("componentRuleAskSpringAIAgent")).isTrue();
            assertThat(context.containsBean("componentRuleBuildSpringAIAgent")).isTrue();
        }
    }

    @Test
    void testActivatesWhenOnlyCopilotIsEnabled() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            addProperties(context, Map.of("bytechef.ai.copilot.enabled", "true", "bytechef.edition", "ee"));

            context.register(
                ComponentRuleDependenciesConfiguration.class, ComponentRuleAgentConfiguration.class);
            context.refresh();

            assertThat(context.getBeanNamesForType(ComponentRuleToolCallbacksFactory.class)).hasSize(1);
            assertThat(context.containsBean("componentRuleAskSpringAIAgent")).isTrue();
            assertThat(context.containsBean("componentRuleBuildSpringAIAgent")).isTrue();
        }
    }

    @Test
    void testBacksOffWhenNeitherAgentSurfaceIsEnabled() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            addProperties(context, Map.of("bytechef.edition", "ee"));

            context.register(
                ComponentRuleDependenciesConfiguration.class, ComponentRuleAgentConfiguration.class);
            context.refresh();

            assertThat(context.getBeanNamesForType(ComponentRuleToolCallbacksFactory.class)).isEmpty();
        }
    }

    @Test
    void testBacksOffOnACommunityEdition() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            addProperties(
                context,
                Map.of("bytechef.ai.copilot.enabled", "true", "bytechef.ai.hub.enabled", "true", "bytechef.edition",
                    "ce"));

            context.register(
                ComponentRuleDependenciesConfiguration.class, ComponentRuleAgentConfiguration.class);
            context.refresh();

            assertThat(context.getBeanNamesForType(ComponentRuleToolCallbacksFactory.class)).isEmpty();
        }
    }

    private static void addProperties(AnnotationConfigApplicationContext context, Map<String, Object> properties) {
        ConfigurableEnvironment environment = context.getEnvironment();

        MutablePropertySources propertySources = environment.getPropertySources();

        propertySources.addFirst(new MapPropertySource("componentRuleAgentConfigurationConditionTest", properties));
    }

    @Configuration
    static class ComponentRuleDependenciesConfiguration {

        @Bean
        ChatMemory chatMemory() {
            return Mockito.mock(ChatMemory.class);
        }

        @Bean
        ChatModel chatModel() {
            return Mockito.mock(ChatModel.class);
        }

        @Bean
        ClusterElementDefinitionService clusterElementDefinitionService() {
            return Mockito.mock(ClusterElementDefinitionService.class);
        }

        @Bean
        ComponentDefinitionService componentDefinitionService() {
            return Mockito.mock(ComponentDefinitionService.class);
        }

        @Bean
        ComponentRuleService componentRuleService() {
            return Mockito.mock(ComponentRuleService.class);
        }

        @Bean
        Evaluator evaluator() {
            return Mockito.mock(Evaluator.class);
        }

        @Bean
        SecurityContextRehydrator securityContextRehydrator() {
            return Mockito.mock(SecurityContextRehydrator.class);
        }
    }
}
