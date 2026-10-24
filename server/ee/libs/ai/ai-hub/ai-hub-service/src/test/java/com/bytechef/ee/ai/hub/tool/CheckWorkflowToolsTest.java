/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.PersistedWorkflowChecker;
import com.bytechef.ai.copilot.tool.PersistedWorkflowChecker.Issue;
import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.ai.hub.tool.CheckWorkflowTools.WorkflowCheck;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CheckWorkflowToolsTest {

    private static final String WORKFLOW_ID = "workflow-1";

    private final PersistedWorkflowChecker persistedWorkflowChecker = mock(PersistedWorkflowChecker.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final CheckWorkflowTools checkWorkflowTools = new CheckWorkflowTools(
        persistedWorkflowChecker, projectService);

    @Test
    void testCheckWorkflowReturnsTheEditorIssues() {
        List<Issue> issues = List.of(new Issue("googleMail_1", "Connection is required", true));

        when(projectService.getWorkflowProject(WORKFLOW_ID)).thenReturn(projectInWorkspace(7L));
        when(persistedWorkflowChecker.check(WORKFLOW_ID)).thenReturn(issues);

        WorkflowCheck workflowCheck = checkWorkflowTools.checkWorkflow(WORKFLOW_ID, workspaceContext(7L));

        assertThat(workflowCheck.workflowId()).isEqualTo(WORKFLOW_ID);
        assertThat(workflowCheck.issues()).isEqualTo(issues);
    }

    @Test
    void testCheckWorkflowRejectsAWorkflowOfAnotherWorkspace() {
        when(projectService.getWorkflowProject(WORKFLOW_ID)).thenReturn(projectInWorkspace(8L));

        assertThatThrownBy(() -> checkWorkflowTools.checkWorkflow(WORKFLOW_ID, workspaceContext(7L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("was not found");

        verifyNoInteractions(persistedWorkflowChecker);
    }

    @Test
    void testCheckWorkflowRejectsAnUnknownWorkflow() {
        when(projectService.getWorkflowProject(WORKFLOW_ID)).thenThrow(new NoSuchElementException());

        assertThatThrownBy(() -> checkWorkflowTools.checkWorkflow(WORKFLOW_ID, workspaceContext(7L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("was not found");

        verifyNoInteractions(persistedWorkflowChecker);
    }

    @Test
    void testCheckWorkflowRefusesWithoutAWorkspace() {
        assertThatThrownBy(() -> checkWorkflowTools.checkWorkflow(WORKFLOW_ID, new ToolContext(Map.of())))
            .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(projectService, persistedWorkflowChecker);
    }

    private static Project projectInWorkspace(long workspaceId) {
        Project project = new Project();

        project.setWorkspaceId(workspaceId);

        return project;
    }

    private static ToolContext workspaceContext(long workspaceId) {
        return new ToolContext(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId));
    }
}
