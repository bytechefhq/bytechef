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
 * Pins that {@link AiHubAgentConversationRecorder} can actually be instantiated BY THE CONTAINER, which every other
 * test in this package structurally cannot show: the unit suite calls the constructor directly and the integration
 * suites hand-build the bean in their own {@code @Bean} method, so none of them exercises Spring's constructor
 * selection.
 *
 * <p>
 * The bug this guards: the class carries two constructors — the production one and a package-private one taking a
 * {@link java.time.Clock} for tests. Spring auto-selects a constructor only when there is exactly one candidate; with
 * two and no {@code @Autowired} marker it falls back to the no-arg constructor, which does not exist, and the whole
 * application context dies at startup with {@code BeanInstantiationException: No default constructor found}.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubAgentConversationRecorderBeanWiringTest {

    @Test
    void testContainerInstantiatesRecorder() {
        try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
            // The recorder is @ConditionalOnProperty; without this the bean definition would be skipped and the test
            // would pass vacuously.
            applicationContext.getEnvironment()
                .getPropertySources()
                .addFirst(new MapPropertySource("test", Map.of("bytechef.ai.hub.enabled", "true")));

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
