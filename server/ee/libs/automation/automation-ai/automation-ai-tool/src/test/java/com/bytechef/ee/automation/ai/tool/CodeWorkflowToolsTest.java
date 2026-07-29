/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier.CodeWorkflowInfo;
import com.bytechef.ee.automation.ai.tool.exception.CodeWorkflowToolErrorType;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.exception.ExecutionException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class CodeWorkflowToolsTest {

    @Mock
    private ProjectCodeWorkflowFacade projectCodeWorkflowFacade;

    @Mock
    private ProjectCodeWorkflowInfoSupplier projectCodeWorkflowInfoSupplier;

    @Test
    void testCreateCodeWorkflowReturnsConfirmationMessage() {
        CodeWorkflowTools tools = new CodeWorkflowTools(projectCodeWorkflowFacade);

        Project project = new Project();

        project.setId(7L);
        project.setName("my-code-project");

        when(projectCodeWorkflowFacade.createEmptyCodeWorkflow(1L, "my-code-project", Language.JAVASCRIPT))
            .thenReturn(project);

        String result = tools.createCodeWorkflow(1L, "my-code-project", "JAVASCRIPT");

        verify(projectCodeWorkflowFacade).createEmptyCodeWorkflow(1L, "my-code-project", Language.JAVASCRIPT);
        assertThat(result).contains("7")
            .contains("my-code-project");
    }

    @Test
    void testCreateCodeWorkflowDefaultsWorkspaceIdWhenOmitted() {
        CodeWorkflowTools tools = new CodeWorkflowTools(projectCodeWorkflowFacade);

        Project project = new Project();

        project.setId(7L);
        project.setName("my-code-project");

        when(projectCodeWorkflowFacade.createEmptyCodeWorkflow(
            Workspace.DEFAULT_WORKSPACE_ID, "my-code-project", Language.JAVASCRIPT))
                .thenReturn(project);

        String result = tools.createCodeWorkflow(null, "my-code-project", "JAVASCRIPT");

        verify(projectCodeWorkflowFacade).createEmptyCodeWorkflow(
            Workspace.DEFAULT_WORKSPACE_ID, "my-code-project", Language.JAVASCRIPT);
        assertThat(result).contains("7")
            .contains("my-code-project");
    }

    @Test
    void testCreateCodeWorkflowUsesExplicitWorkspaceIdWhenProvided() {
        CodeWorkflowTools tools = new CodeWorkflowTools(projectCodeWorkflowFacade);

        Project project = new Project();

        project.setId(9L);
        project.setName("my-code-project");

        when(projectCodeWorkflowFacade.createEmptyCodeWorkflow(42L, "my-code-project", Language.JAVASCRIPT))
            .thenReturn(project);

        String result = tools.createCodeWorkflow(42L, "my-code-project", "JAVASCRIPT");

        verify(projectCodeWorkflowFacade).createEmptyCodeWorkflow(42L, "my-code-project", Language.JAVASCRIPT);
        assertThat(result).contains("9")
            .contains("my-code-project");
    }

    @Test
    void testCreateCodeWorkflowAcceptsLanguageNameCaseInsensitively() {
        CodeWorkflowTools tools = new CodeWorkflowTools(projectCodeWorkflowFacade);

        Project project = new Project();

        project.setId(3L);
        project.setName("my-code-project");

        when(projectCodeWorkflowFacade.createEmptyCodeWorkflow(1L, "my-code-project", Language.PYTHON))
            .thenReturn(project);

        tools.createCodeWorkflow(1L, "my-code-project", "python");

        verify(projectCodeWorkflowFacade).createEmptyCodeWorkflow(1L, "my-code-project", Language.PYTHON);
    }

    @Test
    void testCreateCodeWorkflowThrowsExecutionExceptionOnFacadeFailure() {
        CodeWorkflowTools tools = new CodeWorkflowTools(projectCodeWorkflowFacade);

        when(projectCodeWorkflowFacade.createEmptyCodeWorkflow(anyLong(), anyString(), any()))
            .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> tools.createCodeWorkflow(1L, "my-code-project", "JAVASCRIPT"))
            .isInstanceOf(ExecutionException.class)
            .extracting(thrown -> ((ExecutionException) thrown).getErrorKey())
            .isEqualTo(CodeWorkflowToolErrorType.CREATE.getErrorKey());
    }

    @Test
    void testCreateCodeWorkflowRejectsJavaLanguage() {
        CodeWorkflowTools tools = new CodeWorkflowTools(projectCodeWorkflowFacade);

        assertThatThrownBy(() -> tools.createCodeWorkflow(1L, "my-code-project", "JAVA"))
            .isInstanceOf(ExecutionException.class)
            .extracting(thrown -> ((ExecutionException) thrown).getErrorKey())
            .isEqualTo(CodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE.getErrorKey());

        verifyNoInteractions(projectCodeWorkflowFacade);
    }

    @Test
    void testCreateCodeWorkflowRejectsUnknownLanguage() {
        CodeWorkflowTools tools = new CodeWorkflowTools(projectCodeWorkflowFacade);

        assertThatThrownBy(() -> tools.createCodeWorkflow(1L, "my-code-project", "COBOL"))
            .isInstanceOf(ExecutionException.class)
            .extracting(thrown -> ((ExecutionException) thrown).getErrorKey())
            .isEqualTo(CodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE.getErrorKey());

        verifyNoInteractions(projectCodeWorkflowFacade);
    }

    @Test
    void testCreateCodeWorkflowRejectsBlankLanguage() {
        CodeWorkflowTools tools = new CodeWorkflowTools(projectCodeWorkflowFacade);

        assertThatThrownBy(() -> tools.createCodeWorkflow(1L, "my-code-project", "   "))
            .isInstanceOf(ExecutionException.class)
            .extracting(thrown -> ((ExecutionException) thrown).getErrorKey())
            .isEqualTo(CodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE.getErrorKey());

        verifyNoInteractions(projectCodeWorkflowFacade);
    }

    @Test
    void testUpdateCodeWorkflowSourceReturnsConfirmationMessage() {
        CodeWorkflowTools tools = new CodeWorkflowTools(projectCodeWorkflowFacade);

        String result = tools.updateCodeWorkflowSource(5L, "({name: 'my-code-project'})");

        verify(projectCodeWorkflowFacade).updateCodeWorkflowSource(5L, "({name: 'my-code-project'})");
        assertThat(result).isNotBlank()
            .contains("5");
    }

    @Test
    void testUpdateCodeWorkflowSourceThrowsExecutionExceptionOnFacadeFailure() {
        CodeWorkflowTools tools = new CodeWorkflowTools(projectCodeWorkflowFacade);

        doThrow(new RuntimeException("boom"))
            .when(projectCodeWorkflowFacade)
            .updateCodeWorkflowSource(5L, "content");

        assertThatThrownBy(() -> tools.updateCodeWorkflowSource(5L, "content"))
            .isInstanceOf(ExecutionException.class)
            .extracting(thrown -> ((ExecutionException) thrown).getErrorKey())
            .isEqualTo(CodeWorkflowToolErrorType.UPDATE_SOURCE.getErrorKey());
    }

    @Test
    void testGetCodeWorkflowSourceReturnsSourceText() {
        ReadCodeWorkflowTools tools = new ReadCodeWorkflowTools(
            projectCodeWorkflowFacade, projectCodeWorkflowInfoSupplier);

        when(projectCodeWorkflowFacade.getCodeWorkflowSource(3L)).thenReturn("({name: 'my-code-project'})");

        String result = tools.getCodeWorkflowSource(3L);

        verify(projectCodeWorkflowFacade).getCodeWorkflowSource(3L);
        assertThat(result).isEqualTo("({name: 'my-code-project'})");
    }

    @Test
    void testGetCodeWorkflowSourceThrowsExecutionExceptionOnFacadeFailure() {
        ReadCodeWorkflowTools tools = new ReadCodeWorkflowTools(
            projectCodeWorkflowFacade, projectCodeWorkflowInfoSupplier);

        when(projectCodeWorkflowFacade.getCodeWorkflowSource(3L)).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> tools.getCodeWorkflowSource(3L))
            .isInstanceOf(ExecutionException.class)
            .extracting(thrown -> ((ExecutionException) thrown).getErrorKey())
            .isEqualTo(CodeWorkflowToolErrorType.GET_SOURCE.getErrorKey());
    }

    @Test
    void testListCodeWorkflowsReturnsFacadeListWithResolvedLanguage() {
        ReadCodeWorkflowTools tools = new ReadCodeWorkflowTools(
            projectCodeWorkflowFacade, projectCodeWorkflowInfoSupplier);

        Project project = new Project();

        project.setId(1L);
        project.setName("my-code-project");

        when(projectCodeWorkflowFacade.getCodeWorkflowProjects()).thenReturn(List.of(project));
        when(projectCodeWorkflowInfoSupplier.fetchCodeWorkflowInfo(1L))
            .thenReturn(Optional.of(new CodeWorkflowInfo("JAVASCRIPT")));

        List<ReadCodeWorkflowTools.CodeWorkflowProjectInfo> result = tools.listCodeWorkflows();

        verify(projectCodeWorkflowFacade).getCodeWorkflowProjects();
        assertThat(result).containsExactly(
            new ReadCodeWorkflowTools.CodeWorkflowProjectInfo(1L, "my-code-project", "JAVASCRIPT"));
    }

    @Test
    void testListCodeWorkflowsReturnsNullLanguageWhenInfoUnavailable() {
        ReadCodeWorkflowTools tools = new ReadCodeWorkflowTools(
            projectCodeWorkflowFacade, projectCodeWorkflowInfoSupplier);

        Project project = new Project();

        project.setId(1L);
        project.setName("my-code-project");

        when(projectCodeWorkflowFacade.getCodeWorkflowProjects()).thenReturn(List.of(project));
        when(projectCodeWorkflowInfoSupplier.fetchCodeWorkflowInfo(1L)).thenReturn(Optional.empty());

        List<ReadCodeWorkflowTools.CodeWorkflowProjectInfo> result = tools.listCodeWorkflows();

        assertThat(result).containsExactly(
            new ReadCodeWorkflowTools.CodeWorkflowProjectInfo(1L, "my-code-project", null));
    }

    @Test
    void testListCodeWorkflowsThrowsExecutionExceptionOnFacadeFailure() {
        ReadCodeWorkflowTools tools = new ReadCodeWorkflowTools(
            projectCodeWorkflowFacade, projectCodeWorkflowInfoSupplier);

        when(projectCodeWorkflowFacade.getCodeWorkflowProjects()).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(tools::listCodeWorkflows)
            .isInstanceOf(ExecutionException.class)
            .extracting(thrown -> ((ExecutionException) thrown).getErrorKey())
            .isEqualTo(CodeWorkflowToolErrorType.LIST.getErrorKey());
    }
}
