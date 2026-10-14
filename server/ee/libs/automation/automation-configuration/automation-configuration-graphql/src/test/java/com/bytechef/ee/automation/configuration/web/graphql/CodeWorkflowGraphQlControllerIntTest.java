/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.ee.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlTestConfiguration;
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
    AutomationConfigurationGraphQlTestConfiguration.class,
    CodeWorkflowGraphQlController.class
})
@GraphQlTest(
    controllers = CodeWorkflowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class CodeWorkflowGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ProjectCodeWorkflowFacade projectCodeWorkflowFacade;

    @Test
    void testCodeWorkflowSource() {
        when(projectCodeWorkflowFacade.getCodeWorkflowSource(42L)).thenReturn("console.log('hi');");

        graphQlTester
            .document("query { codeWorkflowSource(projectId: 42) }")
            .execute()
            .path("codeWorkflowSource")
            .entity(String.class)
            .isEqualTo("console.log('hi');");
    }

    @Test
    void testUpdateCodeWorkflowSource() {
        graphQlTester
            .document("mutation { updateCodeWorkflowSource(projectId: 42, content: \"console.log('updated');\") }")
            .execute()
            .path("updateCodeWorkflowSource")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(projectCodeWorkflowFacade).updateCodeWorkflowSource(42L, "console.log('updated');");
    }

    @Test
    void testCreateCodeWorkflow() {
        Project project = new Project();

        project.setId(123L);

        when(projectCodeWorkflowFacade.createEmptyCodeWorkflow(
            eq(7L), eq("my-code-project"), eq(Language.JAVASCRIPT), isNull(), isNull(), eq(List.of("billing"))))
                .thenReturn(project);

        graphQlTester
            .document(
                "mutation { createCodeWorkflow(workspaceId: 7, name: \"my-code-project\", language: JAVASCRIPT, "
                    + "tags: [\"billing\"]) }")
            .execute()
            .path("createCodeWorkflow")
            .entity(String.class)
            .isEqualTo("123");
    }
}
