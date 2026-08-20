/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CodeWorkflowGraphQlControllerTest {

    private final ProjectCodeWorkflowFacade projectCodeWorkflowFacade = mock(ProjectCodeWorkflowFacade.class);
    private final CodeWorkflowGraphQlController controller =
        new CodeWorkflowGraphQlController(projectCodeWorkflowFacade);

    @Test
    void testCodeWorkflowSourceDelegatesToFacade() {
        when(projectCodeWorkflowFacade.getCodeWorkflowSource(42L)).thenReturn("console.log('hi');");

        String source = controller.codeWorkflowSource(42L);

        assertThat(source).isEqualTo("console.log('hi');");
    }

    @Test
    void testUpdateCodeWorkflowSourceDelegatesToFacadeAndReturnsTrue() {
        boolean result = controller.updateCodeWorkflowSource(42L, "console.log('updated');");

        assertThat(result).isTrue();

        verify(projectCodeWorkflowFacade).updateCodeWorkflowSource(42L, "console.log('updated');");
    }

    @Test
    void testCreateCodeWorkflowDelegatesToFacadeAndReturnsProjectId() {
        Project project = new Project();

        project.setId(123L);
        project.setName("my-code-project");

        when(projectCodeWorkflowFacade.createEmptyCodeWorkflow(eq(7L), eq("my-code-project"), eq(Language.JAVASCRIPT),
            isNull(), isNull(), isNull()))
                .thenReturn(project);

        String projectId = controller.createCodeWorkflow(7L, "my-code-project", Language.JAVASCRIPT, null, null, null);

        assertThat(projectId).isEqualTo("123");

        verify(projectCodeWorkflowFacade).createEmptyCodeWorkflow(7L, "my-code-project", Language.JAVASCRIPT, null,
            null, null);
    }

    @Test
    void testCreateCodeWorkflowSupportsAllNonJavaLanguages() {
        Project project = new Project();

        project.setId(1L);

        when(projectCodeWorkflowFacade.createEmptyCodeWorkflow(anyLong(), anyString(), eq(Language.PYTHON), isNull(),
            isNull(), isNull()))
                .thenReturn(project);
        // RUBY-DISABLED: the facade now rejects RUBY with LANGUAGE_NOT_SUPPORTED — org.graalvm.polyglot:ruby is
        // published only up to 25.0.0 and crashes on the Truffle 25.2.4 this repo pins. The RUBY member of the
        // GraphQL Language enum is kept (existing Ruby containers must still be readable), but this controller test
        // no longer claims a Ruby create succeeds. Restore the two commented blocks once a polyglot ruby jar built on
        // Truffle 25.2+ is published (or GraalVM is downgraded). Grep RUBY-DISABLED.
//        when(projectCodeWorkflowFacade.createEmptyCodeWorkflow(anyLong(), anyString(), eq(Language.RUBY), isNull(),
//            isNull(), isNull()))
//                .thenReturn(project);

        assertThat(controller.createCodeWorkflow(1L, "python-project", Language.PYTHON, null, null, null))
            .isEqualTo("1");
//        assertThat(controller.createCodeWorkflow(1L, "ruby-project", Language.RUBY, null, null, null)).isEqualTo("1");
    }
}
