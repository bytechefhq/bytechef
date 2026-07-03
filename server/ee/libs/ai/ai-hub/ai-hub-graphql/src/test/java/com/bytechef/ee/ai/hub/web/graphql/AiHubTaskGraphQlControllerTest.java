/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.ai.hub.exception.ForbiddenException;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskArtifactService;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.task.AiHubTaskService.AiHubTaskPatch;
import com.bytechef.ee.ai.hub.task.AiHubTaskStatus;
import com.bytechef.ee.ai.hub.task.TitleGenerationService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubTaskGraphQlControllerTest {

    @Test
    void testGenerateTitleShortCircuitsWhenAlreadyTitled() {
        // Idempotency invariant: the client fires generate-title on every untitled turn; once a title lands,
        // every subsequent call must short-circuit before calling the LLM. Without this guard the model bill
        // grows linearly with task length even though the title never changes.
        UserService userService = mock(UserService.class);
        AiHubTaskArtifactService artifactService = mock(AiHubTaskArtifactService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubTask existing = mock(AiHubTask.class);

        // isAutoTitled false — the task has been authoritatively titled (LLM regenerated, or user
        // renamed). The handler should short-circuit before loading messages or calling the LLM.
        when(existing.isAutoTitled()).thenReturn(false);
        when(taskService.getById(42L, 7L, 10L)).thenReturn(existing);

        AiHubTaskGraphQlController controller = new AiHubTaskGraphQlController(
            artifactService, taskService, titleGenerationService, userService, workspaceFacade);

        AiHubTask result = controller.generateAiHubTaskTitle(7L, 42L);

        assertThat(result).isSameAs(existing);
        verify(titleGenerationService, never()).generateTitle(any());
        verify(taskService, never()).patch(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void testGenerateTitlePersistsModelOutput() {
        UserService userService = mock(UserService.class);
        AiHubTaskArtifactService artifactService = mock(AiHubTaskArtifactService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubTask existing = mock(AiHubTask.class);

        // isAutoTitled true — the LLM is free to regenerate. Mockito's default for primitive boolean is
        // false (which would short-circuit the handler), so we have to stub it explicitly.
        when(existing.isAutoTitled()).thenReturn(true);
        when(taskService.getById(42L, 7L, 10L)).thenReturn(existing);
        when(taskService.loadMessages(42L, 7L, 10L)).thenReturn(List.of());
        when(titleGenerationService.generateTitle(List.of())).thenReturn("Fresh title");

        AiHubTask patched = mock(AiHubTask.class);

        when(taskService.patch(eq(42L), eq(7L), eq(10L), any(AiHubTaskPatch.class)))
            .thenReturn(patched);

        AiHubTaskGraphQlController controller = new AiHubTaskGraphQlController(
            artifactService, taskService, titleGenerationService, userService, workspaceFacade);

        AiHubTask result = controller.generateAiHubTaskTitle(7L, 42L);

        assertThat(result).isSameAs(patched);
    }

    @Test
    void testGenerateTitleSwallowsBlankModelOutputWithoutPatch() {
        // Defensive invariant: when the upstream model returns a blank/over-length title the service returns
        // empty string, and we must NOT round-trip an empty patch — that would burn a write and emit a
        // misleading audit row. Pin: the existing row is returned untouched and patch is never called.
        UserService userService = mock(UserService.class);
        AiHubTaskArtifactService artifactService = mock(AiHubTaskArtifactService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubTask existing = mock(AiHubTask.class);

        when(existing.isAutoTitled()).thenReturn(true);
        when(taskService.getById(42L, 7L, 10L)).thenReturn(existing);
        when(taskService.loadMessages(42L, 7L, 10L)).thenReturn(List.of());
        when(titleGenerationService.generateTitle(List.of())).thenReturn("");

        AiHubTaskGraphQlController controller = new AiHubTaskGraphQlController(
            artifactService, taskService, titleGenerationService, userService, workspaceFacade);

        AiHubTask result = controller.generateAiHubTaskTitle(7L, 42L);

        assertThat(result).isSameAs(existing);
        verify(taskService, never()).patch(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void testListRejectsCallerOutsideWorkspace() {
        UserService userService = mock(UserService.class);
        AiHubTaskArtifactService artifactService = mock(AiHubTaskArtifactService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace foreignWorkspace = buildWorkspace(99L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(foreignWorkspace));

        AiHubTaskGraphQlController controller = new AiHubTaskGraphQlController(
            artifactService, taskService, titleGenerationService, userService, workspaceFacade);

        assertThatThrownBy(() -> controller.aiHubTasks(7L, 0, AiHubTaskStatus.ACTIVE))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Workspace is not accessible");

        verify(taskService, never()).list(anyLong(), anyLong(), any(Integer.class), any());
    }

    @Test
    void testCreateWorkflowChatTaskDelegatesWithTitle() {
        UserService userService = mock(UserService.class);
        AiHubTaskArtifactService artifactService = mock(AiHubTaskArtifactService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubTask created = mock(AiHubTask.class);

        when(taskService.createWorkflowChat(7L, 10L, 0, "exec-id", 99L, "Project A — Reply Bot"))
            .thenReturn(created);

        AiHubTaskGraphQlController controller = new AiHubTaskGraphQlController(
            artifactService, taskService, titleGenerationService, userService, workspaceFacade);

        AiHubTask result =
            controller.createWorkflowChatAiHubTask(7L, 0, "exec-id", 99L, "Project A — Reply Bot");

        // Pin the controller as a thin pass-through. The service-layer tests cover the always-new
        // semantics (every call inserts a new row); this test pins that the GraphQL surface forwards every
        // argument straight through (including the optional title) without filtering or mutation, and that
        // the verified workspace id flows from the membership check rather than the request body.
        assertThat(result).isSameAs(created);
        verify(taskService).createWorkflowChat(7L, 10L, 0, "exec-id", 99L, "Project A — Reply Bot");
    }

    @Test
    void testCreateWorkflowChatTaskForwardsNullTitle() {
        UserService userService = mock(UserService.class);
        AiHubTaskArtifactService artifactService = mock(AiHubTaskArtifactService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubTask created = mock(AiHubTask.class);

        when(taskService.createWorkflowChat(7L, 10L, 0, "exec-id", 99L, null)).thenReturn(created);

        AiHubTaskGraphQlController controller = new AiHubTaskGraphQlController(
            artifactService, taskService, titleGenerationService, userService, workspaceFacade);

        AiHubTask result =
            controller.createWorkflowChatAiHubTask(7L, 0, "exec-id", 99L, null);

        // Null title is a valid input (the schema marks the argument optional). The controller MUST forward
        // null verbatim — substituting an empty string would change the service-layer call signature and
        // could cause a future find-or-create-style branch to misbehave.
        assertThat(result).isSameAs(created);
        verify(taskService).createWorkflowChat(7L, 10L, 0, "exec-id", 99L, null);
    }

    @Test
    void testCreateWorkflowChatTaskRejectsNonMemberWorkspace() {
        UserService userService = mock(UserService.class);
        AiHubTaskArtifactService artifactService = mock(AiHubTaskArtifactService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        // User is a member of workspace 7 but is asking to create in workspace 99 — the membership guard
        // must throw before the service layer ever sees the request. Without this test, a regression that
        // skipped the WorkspaceAccessGuard call would silently let cross-tenant task creation
        // through.
        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubTaskGraphQlController controller = new AiHubTaskGraphQlController(
            artifactService, taskService, titleGenerationService, userService, workspaceFacade);

        assertThatThrownBy(
            () -> controller.createWorkflowChatAiHubTask(99L, 0, "exec-id", 1L, "Title"))
                .isInstanceOf(ForbiddenException.class);

        verify(taskService, never())
            .createWorkflowChat(anyLong(), anyLong(), anyInt(), anyString(), anyLong(), any());
    }

    @Test
    void testListDefaultsStatusToActive() {
        UserService userService = mock(UserService.class);
        AiHubTaskArtifactService artifactService = mock(AiHubTaskArtifactService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));
        when(taskService.list(7L, 10L, 0, AiHubTaskStatus.ACTIVE)).thenReturn(List.of());

        AiHubTaskGraphQlController controller = new AiHubTaskGraphQlController(
            artifactService, taskService, titleGenerationService, userService, workspaceFacade);

        controller.aiHubTasks(7L, 0, null);

        verify(taskService).list(7L, 10L, 0, AiHubTaskStatus.ACTIVE);
    }

    @Test
    void testTaskWorkspaceIdResolvesViaService() {
        // Schema declares AiHubTask.workspaceId: Long! but the entity is workspace-agnostic
        // (the relation lives in workspace_ai_hub_task). The @SchemaMapping resolver must
        // delegate to AiHubTaskService.getWorkspaceId(taskId), otherwise GraphQL returns null
        // and trips the non-null contract on every list/create/update response.
        UserService userService = mock(UserService.class);
        AiHubTaskArtifactService artifactService = mock(AiHubTaskArtifactService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        AiHubTask task = mock(AiHubTask.class);

        when(task.getId()).thenReturn(42L);
        when(taskService.getWorkspaceId(42L)).thenReturn(7L);

        AiHubTaskGraphQlController controller = new AiHubTaskGraphQlController(
            artifactService, taskService, titleGenerationService, userService, workspaceFacade);

        long workspaceId = controller.taskWorkspaceId(task);

        assertThat(workspaceId).isEqualTo(7L);
        verify(taskService).getWorkspaceId(42L);
    }

    private static Workspace buildWorkspace(long id) {
        Workspace workspace = mock(Workspace.class);

        when(workspace.getId()).thenReturn(id);

        return workspace;
    }
}
