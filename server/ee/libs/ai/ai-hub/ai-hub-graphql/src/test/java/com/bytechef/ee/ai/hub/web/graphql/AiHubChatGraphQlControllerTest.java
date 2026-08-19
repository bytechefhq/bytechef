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
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatPatch;
import com.bytechef.ee.ai.hub.chat.AiHubChatStatus;
import com.bytechef.ee.ai.hub.chat.TitleGenerationService;
import com.bytechef.ee.ai.hub.exception.ForbiddenException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubChatGraphQlControllerTest {

    @Test
    void testGenerateTitleShortCircuitsWhenAlreadyTitled() {
        // Idempotency invariant: the client fires generate-title on every untitled turn; once a title lands,
        // every subsequent call must short-circuit before calling the LLM. Without this guard the model bill
        // grows linearly with chat length even though the title never changes.
        UserService userService = mock(UserService.class);
        AiHubChatArtifactService artifactService = mock(AiHubChatArtifactService.class);
        AiHubChatService chatService = mock(AiHubChatService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubChat existing = mock(AiHubChat.class);

        // isAutoTitled false — the chat has been authoritatively titled (LLM regenerated, or user
        // renamed). The handler should short-circuit before loading messages or calling the LLM.
        when(existing.isAutoTitled()).thenReturn(false);
        when(chatService.getById(42L, 7L, 10L)).thenReturn(existing);

        AiHubChatGraphQlController controller = new AiHubChatGraphQlController(
            artifactService, chatService, titleGenerationService, userService, workspaceFacade);

        AiHubChat result = controller.generateAiHubChatTitle(7L, 42L);

        assertThat(result).isSameAs(existing);
        verify(titleGenerationService, never()).generateTitle(any());
        verify(chatService, never()).patch(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void testGenerateTitlePersistsModelOutput() {
        UserService userService = mock(UserService.class);
        AiHubChatArtifactService artifactService = mock(AiHubChatArtifactService.class);
        AiHubChatService chatService = mock(AiHubChatService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubChat existing = mock(AiHubChat.class);

        // isAutoTitled true — the LLM is free to regenerate. Mockito's default for primitive boolean is
        // false (which would short-circuit the handler), so we have to stub it explicitly.
        when(existing.isAutoTitled()).thenReturn(true);
        when(chatService.getById(42L, 7L, 10L)).thenReturn(existing);
        when(chatService.loadMessages(42L, 7L, 10L)).thenReturn(List.of());
        when(titleGenerationService.generateTitle(List.of())).thenReturn("Fresh title");

        AiHubChat patched = mock(AiHubChat.class);

        when(chatService.patch(eq(42L), eq(7L), eq(10L), any(AiHubChatPatch.class)))
            .thenReturn(patched);

        AiHubChatGraphQlController controller = new AiHubChatGraphQlController(
            artifactService, chatService, titleGenerationService, userService, workspaceFacade);

        AiHubChat result = controller.generateAiHubChatTitle(7L, 42L);

        assertThat(result).isSameAs(patched);
    }

    @Test
    void testGenerateTitleSwallowsBlankModelOutputWithoutPatch() {
        // Defensive invariant: when the upstream model returns a blank/over-length title the service returns
        // empty string, and we must NOT round-trip an empty patch — that would burn a write and emit a
        // misleading audit row. Pin: the existing row is returned untouched and patch is never called.
        UserService userService = mock(UserService.class);
        AiHubChatArtifactService artifactService = mock(AiHubChatArtifactService.class);
        AiHubChatService chatService = mock(AiHubChatService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubChat existing = mock(AiHubChat.class);

        when(existing.isAutoTitled()).thenReturn(true);
        when(chatService.getById(42L, 7L, 10L)).thenReturn(existing);
        when(chatService.loadMessages(42L, 7L, 10L)).thenReturn(List.of());
        when(titleGenerationService.generateTitle(List.of())).thenReturn("");

        AiHubChatGraphQlController controller = new AiHubChatGraphQlController(
            artifactService, chatService, titleGenerationService, userService, workspaceFacade);

        AiHubChat result = controller.generateAiHubChatTitle(7L, 42L);

        assertThat(result).isSameAs(existing);
        verify(chatService, never()).patch(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void testListRejectsCallerOutsideWorkspace() {
        UserService userService = mock(UserService.class);
        AiHubChatArtifactService artifactService = mock(AiHubChatArtifactService.class);
        AiHubChatService chatService = mock(AiHubChatService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace foreignWorkspace = buildWorkspace(99L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(foreignWorkspace));

        AiHubChatGraphQlController controller = new AiHubChatGraphQlController(
            artifactService, chatService, titleGenerationService, userService, workspaceFacade);

        assertThatThrownBy(() -> controller.aiHubChats(7L, 0, AiHubChatStatus.ACTIVE))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Workspace is not accessible");

        verify(chatService, never()).list(anyLong(), anyLong(), any(Integer.class), any());
    }

    @Test
    void testCreateWorkflowChatChatDelegatesWithTitle() {
        UserService userService = mock(UserService.class);
        AiHubChatArtifactService artifactService = mock(AiHubChatArtifactService.class);
        AiHubChatService chatService = mock(AiHubChatService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubChat created = mock(AiHubChat.class);

        when(chatService.createWorkflowChat(7L, 10L, 0, "exec-id", 99L, "Project A — Reply Bot"))
            .thenReturn(created);

        AiHubChatGraphQlController controller = new AiHubChatGraphQlController(
            artifactService, chatService, titleGenerationService, userService, workspaceFacade);

        AiHubChat result =
            controller.createWorkflowChatAiHubChat(7L, 0, "exec-id", 99L, "Project A — Reply Bot");

        // Pin the controller as a thin pass-through. The service-layer tests cover the always-new
        // semantics (every call inserts a new row); this test pins that the GraphQL surface forwards every
        // argument straight through (including the optional title) without filtering or mutation, and that
        // the verified workspace id flows from the membership check rather than the request body.
        assertThat(result).isSameAs(created);
        verify(chatService).createWorkflowChat(7L, 10L, 0, "exec-id", 99L, "Project A — Reply Bot");
    }

    @Test
    void testCreateWorkflowChatChatForwardsNullTitle() {
        UserService userService = mock(UserService.class);
        AiHubChatArtifactService artifactService = mock(AiHubChatArtifactService.class);
        AiHubChatService chatService = mock(AiHubChatService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubChat created = mock(AiHubChat.class);

        when(chatService.createWorkflowChat(7L, 10L, 0, "exec-id", 99L, null)).thenReturn(created);

        AiHubChatGraphQlController controller = new AiHubChatGraphQlController(
            artifactService, chatService, titleGenerationService, userService, workspaceFacade);

        AiHubChat result =
            controller.createWorkflowChatAiHubChat(7L, 0, "exec-id", 99L, null);

        // Null title is a valid input (the schema marks the argument optional). The controller MUST forward
        // null verbatim — substituting an empty string would change the service-layer call signature and
        // could cause a future find-or-create-style branch to misbehave.
        assertThat(result).isSameAs(created);
        verify(chatService).createWorkflowChat(7L, 10L, 0, "exec-id", 99L, null);
    }

    @Test
    void testCreateWorkflowChatChatRejectsNonMemberWorkspace() {
        UserService userService = mock(UserService.class);
        AiHubChatArtifactService artifactService = mock(AiHubChatArtifactService.class);
        AiHubChatService chatService = mock(AiHubChatService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        // User is a member of workspace 7 but is asking to create in workspace 99 — the membership guard
        // must throw before the service layer ever sees the request. Without this test, a regression that
        // skipped the WorkspaceAccessGuard call would silently let cross-tenant chat creation
        // through.
        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));

        AiHubChatGraphQlController controller = new AiHubChatGraphQlController(
            artifactService, chatService, titleGenerationService, userService, workspaceFacade);

        assertThatThrownBy(
            () -> controller.createWorkflowChatAiHubChat(99L, 0, "exec-id", 1L, "Title"))
                .isInstanceOf(ForbiddenException.class);

        verify(chatService, never())
            .createWorkflowChat(anyLong(), anyLong(), anyInt(), anyString(), anyLong(), any());
    }

    @Test
    void testListDefaultsStatusToActive() {
        UserService userService = mock(UserService.class);
        AiHubChatArtifactService artifactService = mock(AiHubChatArtifactService.class);
        AiHubChatService chatService = mock(AiHubChatService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace7 = buildWorkspace(7L);

        when(workspaceFacade.getUserWorkspaces(10L)).thenReturn(List.of(workspace7));
        when(chatService.list(7L, 10L, 0, AiHubChatStatus.ACTIVE)).thenReturn(List.of());

        AiHubChatGraphQlController controller = new AiHubChatGraphQlController(
            artifactService, chatService, titleGenerationService, userService, workspaceFacade);

        controller.aiHubChats(7L, 0, null);

        verify(chatService).list(7L, 10L, 0, AiHubChatStatus.ACTIVE);
    }

    @Test
    void testChatWorkspaceIdResolvesFromTheEntityColumn() {
        // Schema declares AiHubChat.workspaceId: Long!. The @SchemaMapping resolver reads the loaded row's own
        // workspace_id column, otherwise GraphQL returns null and trips the non-null contract on every
        // list/create/update response.
        UserService userService = mock(UserService.class);
        AiHubChatArtifactService artifactService = mock(AiHubChatArtifactService.class);
        AiHubChatService chatService = mock(AiHubChatService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        AiHubChat chat = new AiHubChat(10L);

        chat.setId(42L);
        chat.setWorkspaceId(7L);

        AiHubChatGraphQlController controller = new AiHubChatGraphQlController(
            artifactService, chatService, titleGenerationService, userService, workspaceFacade);

        long workspaceId = controller.chatWorkspaceId(chat);

        assertThat(workspaceId).isEqualTo(7L);
    }

    @Test
    void testChatWorkspaceIdFailsLoudlyForWorkspaceLessChat() {
        // A null workspace_id is the state the missing membership row used to represent: the chat is unreachable
        // through every workspace-scoped path, so the resolver must fail rather than break the non-null contract.
        UserService userService = mock(UserService.class);
        AiHubChatArtifactService artifactService = mock(AiHubChatArtifactService.class);
        AiHubChatService chatService = mock(AiHubChatService.class);
        TitleGenerationService titleGenerationService = mock(TitleGenerationService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        AiHubChat chat = new AiHubChat(10L);

        chat.setId(42L);

        AiHubChatGraphQlController controller = new AiHubChatGraphQlController(
            artifactService, chatService, titleGenerationService, userService, workspaceFacade);

        assertThatThrownBy(() -> controller.chatWorkspaceId(chat))
            .isInstanceOf(NotFoundException.class);
    }

    private static Workspace buildWorkspace(long id) {
        Workspace workspace = mock(Workspace.class);

        when(workspace.getId()).thenReturn(id);

        return workspace;
    }
}
