/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubAgentConversationRecorderBeanWiringTest {

    @Test
    void testContainerInstantiatesRecorder() {
        try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
            applicationContext.getEnvironment()
                .getPropertySources()
                .addFirst(
                    new MapPropertySource(
                        "test", Map.of("bytechef.ai.hub.enabled", "true", "bytechef.ai.copilot.enabled", "true")));

            applicationContext.register(DependenciesConfiguration.class, AiHubAgentConversationRecorder.class);
            applicationContext.refresh();

            assertThat(applicationContext.getBean(AiHubAgentConversationRecorder.class)).isNotNull();
        }
    }

    @Configuration
    static class DependenciesConfiguration {

        @Bean
        AiHubChatRepository chatRepository() {
            return mock(AiHubChatRepository.class);
        }

        @Bean
        AiHubChatService chatService() {
            return mock(AiHubChatService.class);
        }

        @Bean
        ProjectService projectService() {
            return mock(ProjectService.class);
        }
    }
}
