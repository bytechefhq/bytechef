/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.config;

import static org.mockito.Mockito.mock;

import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectCodeWorkflowFacade;
import com.bytechef.jackson.config.JacksonConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = {
    "com.bytechef.ee.embedded.configuration.public_.web.rest",
    "com.bytechef.platform.configuration.web.rest.adapter",
    "com.bytechef.platform.configuration.web.rest.mapper",
    "com.bytechef.web.rest.mapper"
})
@Configuration
@Import(JacksonConfiguration.class)
public class EmbeddedConfigurationPublicRestTestConfiguration {

    /**
     * Declared here rather than as a {@code @MockitoBean} on each test class: the component scan above picks up every
     * controller in the package, so {@code AutomationProjectCodeWorkflowApiController}'s collaborators have to resolve
     * in every context built from this configuration, not only in the tests that exercise it.
     */
    @Bean
    AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade() {
        return mock(AutomationWorkflowProjectCodeWorkflowFacade.class);
    }
}
