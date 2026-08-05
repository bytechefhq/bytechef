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

package com.bytechef.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.automation.ai.tool.knowledgebase.KnowledgeBaseToolCallbacksFactory;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
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
 * Pins the activation gate of {@link KnowledgeBaseAgentConfiguration}: the configuration must back off when the
 * knowledge-base feature itself is disabled, because every knowledge-base service and facade it injects is registered
 * only when {@code bytechef.ai.knowledge-base.enabled=true}. Enabling only the Copilot or AI Hub surface used to
 * activate the configuration without its dependencies and fail server startup with an unsatisfied
 * {@link WorkspaceKnowledgeBaseFacade} dependency.
 *
 * @author Ivica Cardic
 */
class KnowledgeBaseAgentConfigurationConditionTest {

    @Test
    void testBacksOffWhenKnowledgeBaseIsDisabled() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            addProperties(context, Map.of("bytechef.ai.copilot.enabled", "true", "bytechef.ai.hub.enabled", "true"));

            // Only the beans that exist when the knowledge-base feature is off are registered here, mirroring a real
            // server context; refresh() fails with an unsatisfied dependency if the configuration activates anyway.
            context.register(UngatedDependenciesConfiguration.class, KnowledgeBaseAgentConfiguration.class);
            context.refresh();

            assertThat(context.getBeanNamesForType(KnowledgeBaseToolCallbacksFactory.class)).isEmpty();
        }
    }

    @Test
    void testActivatesWhenKnowledgeBaseAndAnAgentSurfaceAreEnabled() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            addProperties(
                context, Map.of("bytechef.ai.hub.enabled", "true", "bytechef.ai.knowledge-base.enabled", "true"));

            context.register(
                UngatedDependenciesConfiguration.class, KnowledgeBaseDependenciesConfiguration.class,
                KnowledgeBaseAgentConfiguration.class);
            context.refresh();

            assertThat(context.getBeanNamesForType(KnowledgeBaseToolCallbacksFactory.class)).hasSize(1);
            assertThat(context.containsBean("knowledgeBaseAskSubAgentChatClient")).isTrue();
            assertThat(context.containsBean("knowledgeBaseBuildSubAgentChatClient")).isTrue();
        }
    }

    private static void addProperties(AnnotationConfigApplicationContext context, Map<String, Object> properties) {
        ConfigurableEnvironment environment = context.getEnvironment();

        MutablePropertySources propertySources = environment.getPropertySources();

        propertySources.addFirst(new MapPropertySource("knowledgeBaseAgentConfigurationConditionTest", properties));
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
        SecurityContextRehydrator securityContextRehydrator() {
            return Mockito.mock(SecurityContextRehydrator.class);
        }
    }

    @Configuration
    static class KnowledgeBaseDependenciesConfiguration {

        @Bean
        KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade() {
            return Mockito.mock(KnowledgeBaseDocumentFacade.class);
        }

        @Bean
        KnowledgeBaseDocumentService knowledgeBaseDocumentService() {
            return Mockito.mock(KnowledgeBaseDocumentService.class);
        }

        @Bean
        KnowledgeBaseFacade knowledgeBaseFacade() {
            return Mockito.mock(KnowledgeBaseFacade.class);
        }

        @Bean
        KnowledgeBaseService knowledgeBaseService() {
            return Mockito.mock(KnowledgeBaseService.class);
        }

        @Bean
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade() {
            return Mockito.mock(WorkspaceKnowledgeBaseFacade.class);
        }
    }
}
