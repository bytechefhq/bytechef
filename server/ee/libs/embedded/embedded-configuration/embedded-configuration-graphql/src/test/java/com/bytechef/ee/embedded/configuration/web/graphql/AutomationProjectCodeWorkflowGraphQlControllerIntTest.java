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

import com.bytechef.ee.embedded.configuration.dto.AutomationProjectCodeWorkflowSourceDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectCodeWorkflowFacade;
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
    AutomationProjectCodeWorkflowGraphQlController.class
})
@GraphQlTest(
    controllers = AutomationProjectCodeWorkflowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@EmbeddedConfigurationGraphQlConfigurationSharedMocks
class AutomationProjectCodeWorkflowGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private AutomationProjectCodeWorkflowFacade automationProjectCodeWorkflowFacade;

    @Test
    void testAutomationProjectCodeWorkflowSource() {
        when(automationProjectCodeWorkflowFacade.getCodeWorkflowSource(7L))
            .thenReturn(new AutomationProjectCodeWorkflowSourceDTO("print('hi')", Language.PYTHON));

        graphQlTester
            .document("query { automationProjectCodeWorkflowSource(automationProjectId: 7) " +
                "{ content language } }")
            .execute()
            .path("automationProjectCodeWorkflowSource.content")
            .entity(String.class)
            .isEqualTo("print('hi')")
            .path("automationProjectCodeWorkflowSource.language")
            .entity(String.class)
            .isEqualTo("PYTHON");
    }

    @Test
    void testUpdateAutomationProjectCodeWorkflowSource() {
        graphQlTester
            .document("mutation { updateAutomationProjectCodeWorkflowSource(automationProjectId: 7, " +
                "content: \"x\") }")
            .execute()
            .path("updateAutomationProjectCodeWorkflowSource")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationProjectCodeWorkflowFacade).updateCodeWorkflowSource(7L, "x");
    }

    @Test
    void testCreateAutomationProjectCodeWorkflow() {
        when(automationProjectCodeWorkflowFacade.createEmptyCodeWorkflow(
            eq("orders"), eq(Language.JAVASCRIPT), isNull(), eq("Sales"), eq(List.of("crm")), isNull()))
                .thenReturn(7L);

        graphQlTester
            .document(
                "mutation { createAutomationProjectCodeWorkflow(name: \"orders\", language: JAVASCRIPT, " +
                    "category: \"Sales\", tags: [\"crm\"]) }")
            .execute()
            .path("createAutomationProjectCodeWorkflow")
            .entity(String.class)
            .isEqualTo("7");
    }
}
