/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ee.automation.ai.tool.contextstore.ContextStoreToolCallbacksFactory;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreFacade;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreSourceFacade;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
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
class ContextStoreAgentConfigurationTest {

    @Test
    void testBacksOffWhenContextStoreIsDisabled() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            addProperties(
                context,
                Map.of("bytechef.ai.copilot.enabled", "true", "bytechef.edition", "ee"));

            context.register(UngatedDependenciesConfiguration.class, ContextStoreAgentConfiguration.class);
            context.refresh();

            assertThat(context.getBeanNamesForType(ContextStoreToolCallbacksFactory.class)).isEmpty();
        }
    }

    @Test
    void testActivatesWhenContextStoreAndCopilotAreEnabled() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            addProperties(
                context,
                Map.of(
                    "bytechef.ai.copilot.enabled", "true", "bytechef.context-store.enabled", "true",
                    "bytechef.edition", "ee"));

            context.register(
                UngatedDependenciesConfiguration.class, ContextStoreDependenciesConfiguration.class,
                ContextStoreAgentConfiguration.class);
            context.refresh();

            assertThat(context.getBeanNamesForType(ContextStoreToolCallbacksFactory.class)).hasSize(1);
            assertThat(context.containsBean("contextStoreAskSpringAIAgent")).isTrue();
            assertThat(context.containsBean("contextStoreBuildSpringAIAgent")).isTrue();
        }
    }

    private static void addProperties(AnnotationConfigApplicationContext context, Map<String, Object> properties) {
        ConfigurableEnvironment environment = context.getEnvironment();

        MutablePropertySources propertySources = environment.getPropertySources();

        propertySources.addFirst(new MapPropertySource("contextStoreAgentConfigurationTest", properties));
    }

    @Configuration
    static class UngatedDependenciesConfiguration {

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
        SecurityContextRehydrator securityContextRehydrator() {
            return Mockito.mock(SecurityContextRehydrator.class);
        }
    }

    @Configuration
    static class ContextStoreDependenciesConfiguration {

        @Bean
        ContextStoreQueryService contextStoreQueryService() {
            return Mockito.mock(ContextStoreQueryService.class);
        }

        @Bean
        ContextStoreFacade contextStoreFacade() {
            return Mockito.mock(ContextStoreFacade.class);
        }

        @Bean
        ContextStoreSourceFacade contextStoreSourceFacade() {
            return Mockito.mock(ContextStoreSourceFacade.class);
        }

        @Bean
        WorkspaceContextStoreSourceService workspaceContextStoreSourceService() {
            return Mockito.mock(WorkspaceContextStoreSourceService.class);
        }
    }
}
