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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubChatGraphQlControllerTest {

    private AiHubChatArtifactService artifactService;
    private AiHubChatService chatService;
    private AiHubChatGraphQlController controller;
    private PermissionService permissionService;
    private TitleGenerationService titleGenerationService;

    @BeforeEach
    void setUp() {
        artifactService = mock(AiHubChatArtifactService.class);
        chatService = mock(AiHubChatService.class);
        permissionService = mock(PermissionService.class);
        titleGenerationService = mock(TitleGenerationService.class);

        UserService userService = mock(UserService.class);
        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(permissionService.hasWorkspaceRole(7L, "VIEWER")).thenReturn(true);

        controller = new AiHubChatGraphQlController(
            artifactService, chatService, permissionService, titleGenerationService, userService);
    }

    @Test
    void testGenerateTitleShortCircuitsWhenNotAutoTitled() {
        AiHubChat existing = mock(AiHubChat.class);

        when(existing.isAutoTitled()).thenReturn(false);
        when(chatService.getById(42L, 7L, 10L)).thenReturn(existing);

        AiHubChat result = controller.generateAiHubChatTitle(7L, 42L);

        assertThat(result).isSameAs(existing);
        verify(titleGenerationService, never()).generateTitle(any());
        verify(chatService, never()).patch(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void testGenerateTitlePersistsModelOutput() {
        AiHubChat existing = mock(AiHubChat.class);
        AiHubChat patched = mock(AiHubChat.class);

        when(existing.isAutoTitled()).thenReturn(true);
        when(chatService.getById(42L, 7L, 10L)).thenReturn(existing);
        when(chatService.loadMessages(42L, 7L, 10L)).thenReturn(List.of());
        when(titleGenerationService.generateTitle(List.of())).thenReturn("Fresh title");
        when(chatService.patch(eq(42L), eq(7L), eq(10L), any(AiHubChatPatch.class))).thenReturn(patched);

        assertThat(controller.generateAiHubChatTitle(7L, 42L)).isSameAs(patched);
    }

    @Test
    void testGenerateTitleSwallowsBlankModelOutputWithoutPatch() {
        AiHubChat existing = mock(AiHubChat.class);

        when(existing.isAutoTitled()).thenReturn(true);
        when(chatService.getById(42L, 7L, 10L)).thenReturn(existing);
        when(chatService.loadMessages(42L, 7L, 10L)).thenReturn(List.of());
        when(titleGenerationService.generateTitle(List.of())).thenReturn("");

        assertThat(controller.generateAiHubChatTitle(7L, 42L)).isSameAs(existing);
        verify(chatService, never()).patch(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void testGenerateTitleReturnsTheExistingRowWhenTheChatIsDeletedMidFlight() {
        AiHubChat existing = mock(AiHubChat.class);

        when(existing.isAutoTitled()).thenReturn(true);
        when(chatService.getById(42L, 7L, 10L)).thenReturn(existing);
        when(chatService.loadMessages(42L, 7L, 10L)).thenReturn(List.of());
        when(titleGenerationService.generateTitle(List.of())).thenReturn("Fresh title");
        when(chatService.patch(eq(42L), eq(7L), eq(10L), any(AiHubChatPatch.class)))
            .thenThrow(new NotFoundException("gone"));

        assertThat(controller.generateAiHubChatTitle(7L, 42L)).isSameAs(existing);
    }

    @Test
    void testListRejectsCallerWithoutWorkspaceRole() {
        when(permissionService.hasWorkspaceRole(99L, "VIEWER")).thenReturn(false);

        assertThatThrownBy(() -> controller.aiHubChats(99L, 0, AiHubChatStatus.ACTIVE))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Workspace is not accessible");

        verify(chatService, never()).list(anyLong(), anyLong(), anyInt(), any());
    }

    @Test
    void testListDefaultsStatusToActive() {
        AiHubChat chat = mock(AiHubChat.class);

        when(chatService.list(7L, 10L, 0, AiHubChatStatus.ACTIVE)).thenReturn(List.of(chat));

        assertThat(controller.aiHubChats(7L, 0, null)).containsExactly(chat);
    }

    @Test
    void testCancelAiHubRunDelegatesWithTheVerifiedUserAndRunId() {
        when(chatService.cancelAiHubRun(42L, 7L, 10L, "run-1")).thenReturn(true);

        assertThat(controller.cancelAiHubRun(7L, 42L, "run-1")).isTrue();
        verify(chatService).cancelAiHubRun(42L, 7L, 10L, "run-1");
    }

    @Test
    void testCancelAiHubRunForwardsANullRunId() {
        when(chatService.cancelAiHubRun(42L, 7L, 10L, null)).thenReturn(false);

        assertThat(controller.cancelAiHubRun(7L, 42L, null)).isFalse();
    }

    @Test
    void testCancelAiHubRunRejectsCallerWithoutWorkspaceRole() {
        when(permissionService.hasWorkspaceRole(99L, "VIEWER")).thenReturn(false);

        assertThatThrownBy(() -> controller.cancelAiHubRun(99L, 42L, "run-1"))
            .isInstanceOf(ForbiddenException.class);

        verify(chatService, never()).cancelAiHubRun(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void testChatWorkspaceIdResolvesFromTheEntityColumn() {
        AiHubChat chat = new AiHubChat(10L);

        chat.setWorkspaceId(7L);

        assertThat(controller.chatWorkspaceId(chat)).isEqualTo(7L);
    }

    @Test
    void testChatWorkspaceIdFailsLoudlyForWorkspaceLessChat() {
        AiHubChat chat = new AiHubChat(10L);

        assertThatThrownBy(() -> controller.chatWorkspaceId(chat)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void testChatIsOwnerTrueForTheOwningUser() {
        assertThat(controller.chatIsOwner(new AiHubChat(10L))).isTrue();
    }

    @Test
    void testChatIsOwnerFalseForANonOwner() {
        assertThat(controller.chatIsOwner(new AiHubChat(11L))).isFalse();
    }
}
