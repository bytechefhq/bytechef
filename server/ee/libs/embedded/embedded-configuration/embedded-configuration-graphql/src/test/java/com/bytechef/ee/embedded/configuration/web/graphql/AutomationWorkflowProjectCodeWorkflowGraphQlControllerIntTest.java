/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCodeWorkflowSourceDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectCodeWorkflowFacade;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlTestConfiguration;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    EmbeddedConfigurationGraphQlTestConfiguration.class,
    AutomationWorkflowProjectCodeWorkflowGraphQlController.class
})
@GraphQlTest(
    controllers = AutomationWorkflowProjectCodeWorkflowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@EmbeddedConfigurationGraphQlConfigurationSharedMocks
class AutomationWorkflowProjectCodeWorkflowGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade;

    @Test
    void testAutomationWorkflowProjectCodeWorkflowSource() {
        when(automationWorkflowProjectCodeWorkflowFacade.getCodeWorkflowSource(7L))
            .thenReturn(new AutomationWorkflowProjectCodeWorkflowSourceDTO("print('hi')", Language.PYTHON));

        graphQlTester
            .document("query { automationWorkflowProjectCodeWorkflowSource(automationWorkflowProjectId: 7) " +
                "{ content language } }")
            .execute()
            .path("automationWorkflowProjectCodeWorkflowSource.content")
            .entity(String.class)
            .isEqualTo("print('hi')")
            .path("automationWorkflowProjectCodeWorkflowSource.language")
            .entity(String.class)
            .isEqualTo("PYTHON");
    }

    @Test
    void testUpdateAutomationWorkflowProjectCodeWorkflowSource() {
        graphQlTester
            .document("mutation { updateAutomationWorkflowProjectCodeWorkflowSource(automationWorkflowProjectId: 7, " +
                "content: \"x\") }")
            .execute()
            .path("updateAutomationWorkflowProjectCodeWorkflowSource")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectCodeWorkflowFacade).updateCodeWorkflowSource(7L, "x");
    }

    @Test
    void testCreateAutomationWorkflowProjectCodeWorkflow() {
        when(automationWorkflowProjectCodeWorkflowFacade.createEmptyCodeWorkflow(
            eq("orders"), eq(Language.JAVASCRIPT), isNull(), eq("Sales"), eq(List.of("crm")), isNull()))
                .thenReturn(7L);

        graphQlTester
            .document(
                "mutation { createAutomationWorkflowProjectCodeWorkflow(name: \"orders\", language: JAVASCRIPT, " +
                    "category: \"Sales\", tags: [\"crm\"]) }")
            .execute()
            .path("createAutomationWorkflowProjectCodeWorkflow")
            .entity(String.class)
            .isEqualTo("7");
    }
}
