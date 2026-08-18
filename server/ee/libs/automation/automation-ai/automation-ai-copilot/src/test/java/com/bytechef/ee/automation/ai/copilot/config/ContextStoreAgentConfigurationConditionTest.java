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
 * Pins the activation gate of {@link ContextStoreAgentConfiguration}: the configuration must back off when the
 * context-store feature itself is disabled, because every context-store service and facade it injects is registered
 * only when {@code bytechef.context-store.enabled=true}. Enabling only the Copilot or AI Hub surface used to activate
 * the configuration without its dependencies and fail server startup with an unsatisfied
 * {@link WorkspaceContextStoreSourceService} dependency.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ContextStoreAgentConfigurationConditionTest {

    @Test
    void testBacksOffWhenContextStoreIsDisabled() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            addProperties(
                context,
                Map.of(
                    "bytechef.ai.copilot.enabled", "true", "bytechef.ai.hub.enabled", "true", "bytechef.edition",
                    "ee"));

            // Only the beans that exist when the context-store feature is off are registered here, mirroring a real
            // server context; refresh() fails with an unsatisfied dependency if the configuration activates anyway.
            context.register(UngatedDependenciesConfiguration.class, ContextStoreAgentConfiguration.class);
            context.refresh();

            assertThat(context.getBeanNamesForType(ContextStoreToolCallbacksFactory.class)).isEmpty();
        }
    }

    @Test
    void testActivatesWhenContextStoreAndAnAgentSurfaceAreEnabled() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            addProperties(
                context,
                Map.of(
                    "bytechef.ai.hub.enabled", "true", "bytechef.context-store.enabled", "true", "bytechef.edition",
                    "ee"));

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

        propertySources.addFirst(new MapPropertySource("contextStoreAgentConfigurationConditionTest", properties));
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
