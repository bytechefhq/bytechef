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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatComponent;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolBinding;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolConnectionValidator;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade;
import com.bytechef.ee.ai.hub.exception.ForbiddenException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.web.graphql.AiHubChatToolGraphQlController.AttachAiHubChatToolInput;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 * @author Ivica Cardic
 */
class AiHubChatToolGraphQlControllerTest {

    private static final long CHAT_ID = 42L;
    private static final long OWNER_USER_ID = 10L;
    private static final long WORKSPACE_ID = 7L;

    private final AiHubChatService chatService = mock(AiHubChatService.class);
    private final AiHubChatToolFacade chatToolFacade = mock(AiHubChatToolFacade.class);
    private final AiHubChatToolConnectionValidator connectionValidator = mock(AiHubChatToolConnectionValidator.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final UserService userService = mock(UserService.class);
    private final AiHubChatToolGraphQlController controller = new AiHubChatToolGraphQlController(
        chatService, chatToolFacade, mock(ClusterElementDefinitionService.class),
        mock(ComponentDefinitionService.class), connectionValidator, permissionService, userService);

    @BeforeEach
    void beforeEach() {
        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).thenReturn(true);

        AiHubChat chat = mock(AiHubChat.class);

        when(chat.getId()).thenReturn(CHAT_ID);
        when(chat.getUserId()).thenReturn(OWNER_USER_ID);
        when(chat.getEnvironmentId()).thenReturn(1L);
        when(chatService.findNotDeletedByThreadId("thread-1")).thenReturn(Optional.of(chat));
        when(chatService.getWorkspaceId(CHAT_ID)).thenReturn(WORKSPACE_ID);
        when(chatService.getById(CHAT_ID, WORKSPACE_ID, OWNER_USER_ID)).thenReturn(chat);
        when(chatService.findByThreadIdViewable("thread-1", OWNER_USER_ID)).thenReturn(Optional.of(chat));
    }

    @Test
    void testSetAiHubChatToolRequiresApprovalUpdatesFlagAndReturnsRereadBinding() {
        signIn(OWNER_USER_ID);

        when(chatToolFacade.fetchChatTool(5L)).thenReturn(
            Optional.of(binding(false)), Optional.of(binding(true)));

        AiHubChatToolBinding result = controller.setAiHubChatToolRequiresApproval(WORKSPACE_ID, 5L, true);

        verify(chatToolFacade).setToolRequiresApproval(5L, true);
        assertThat(result.requiresApproval()).isTrue();
    }

    @Test
    void testSetAiHubChatToolRequiresApprovalRefusesAToolOnAChatTheCallerDoesNotOwn() {
        signIn(20L);

        when(chatToolFacade.fetchChatTool(5L)).thenReturn(Optional.of(binding(false)));
        when(chatService.getById(CHAT_ID, WORKSPACE_ID, 20L)).thenThrow(new ForbiddenException("not yours"));

        assertThatThrownBy(() -> controller.setAiHubChatToolRequiresApproval(WORKSPACE_ID, 5L, true))
            .isInstanceOf(ForbiddenException.class);

        verify(chatToolFacade, never()).setToolRequiresApproval(anyLong(), anyBoolean());
    }

    @Test
    void testRemoveAiHubChatToolRefusesAToolOnASharedChatTheCallerOnlyViews() {
        signIn(20L);

        AiHubChat sharedChat = mock(AiHubChat.class);

        when(sharedChat.getUserId()).thenReturn(OWNER_USER_ID);
        when(chatToolFacade.fetchChatTool(5L)).thenReturn(Optional.of(binding(false)));
        when(chatService.getById(CHAT_ID, WORKSPACE_ID, 20L)).thenReturn(sharedChat);

        assertThatThrownBy(() -> controller.removeAiHubChatTool(WORKSPACE_ID, 5L))
            .isInstanceOf(ForbiddenException.class);

        verify(chatToolFacade, never()).removeTool(anyLong());
    }

    @Test
    void testCallerWithoutAWorkspaceRoleIsRejected() {
        signIn(OWNER_USER_ID);

        assertThatThrownBy(() -> controller.setAiHubChatToolRequiresApproval(99L, 5L, true))
            .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> controller.aiHubUserConnectors(99L, null))
            .isInstanceOf(ForbiddenException.class);

        verify(chatToolFacade, never()).setToolRequiresApproval(anyLong(), anyBoolean());
        verify(chatToolFacade, never()).listUserComponents(anyLong(), anyLong());
    }

    @Test
    void testAiHubChatToolsListsAViewableChatsTools() {
        signIn(OWNER_USER_ID);

        when(chatToolFacade.listChatTools(CHAT_ID)).thenReturn(List.of(binding(false)));

        assertThat(controller.aiHubChatTools(WORKSPACE_ID, "thread-1")).containsExactly(binding(false));
    }

    @Test
    void testAiHubChatToolsReturnsEmptyForAThreadTheCallerCannotView() {
        signIn(12L);

        when(chatService.findByThreadIdViewable("thread-1", 12L)).thenReturn(Optional.empty());

        assertThat(controller.aiHubChatTools(WORKSPACE_ID, "thread-1")).isEmpty();

        verify(chatToolFacade, never()).listChatTools(anyLong());
    }

    @Test
    void testAttachAiHubChatToolUsesTheChatEnvironmentAndValidatesTheConnection() {
        signIn(OWNER_USER_ID);

        when(chatToolFacade.attachComponent(CHAT_ID, "slack", 1, 42L, 1)).thenReturn(99L);
        when(chatToolFacade.addTool(99L, "sendMessage", Map.of())).thenReturn(5L);
        when(chatToolFacade.listChatTools(CHAT_ID)).thenReturn(List.of(binding(false)));

        AiHubChatToolBinding result = controller.attachAiHubChatTool(attachInput(42L));

        assertThat(result.chatToolId()).isEqualTo(5L);

        verify(connectionValidator).validate(WORKSPACE_ID, "slack", 42L, 1);
    }

    @Test
    void testAttachAiHubChatToolRejectsAConnectionOutsideTheWorkspace() {
        signIn(OWNER_USER_ID);

        doThrow(new IllegalArgumentException("Connection 42 is not available"))
            .when(connectionValidator)
            .validate(WORKSPACE_ID, "slack", 42L, 1);

        assertThatThrownBy(() -> controller.attachAiHubChatTool(attachInput(42L)))
            .isInstanceOf(IllegalArgumentException.class);

        verify(chatToolFacade, never()).attachComponent(anyLong(), anyString(), anyInt(), any(), anyInt());
    }

    @Test
    void testAttachAiHubChatToolRejectsAChatTheCallerDoesNotOwn() {
        signIn(20L);

        assertThatThrownBy(() -> controller.attachAiHubChatTool(attachInput(null)))
            .isInstanceOf(NotFoundException.class);

        verify(chatToolFacade, never()).attachComponent(anyLong(), anyString(), anyInt(), any(), anyInt());
    }

    @Test
    void testDetachAiHubChatComponentRefusesAUserConnectorWithNoChat() {
        signIn(OWNER_USER_ID);

        AiHubChatComponent userConnector = new AiHubChatComponent();

        userConnector.setId(55L);
        userConnector.setUserId(OWNER_USER_ID);

        when(chatToolFacade.fetchChatComponent(55L)).thenReturn(Optional.of(userConnector));

        assertThat(controller.detachAiHubChatComponent(WORKSPACE_ID, 55L)).isFalse();

        verify(chatToolFacade, never()).detachComponent(anyLong());
    }

    @Test
    void testSetAiHubUserConnectorToolRequiresApprovalUpdatesOwnedConnectorTool() {
        signIn(OWNER_USER_ID);

        AiHubChatComponent ownedConnector = new AiHubChatComponent();

        ownedConnector.setId(55L);

        when(chatToolFacade.listUserComponents(OWNER_USER_ID, WORKSPACE_ID)).thenReturn(List.of(ownedConnector));

        assertThat(controller.setAiHubUserConnectorToolRequiresApproval(WORKSPACE_ID, 55L, "sendMessage", true))
            .isTrue();

        verify(chatToolFacade).setToolRequiresApproval(55L, "sendMessage", true);
    }

    @Test
    void testUserConnectorMutationsRefuseAnotherUsersConnector() {
        signIn(OWNER_USER_ID);

        when(chatToolFacade.listUserComponents(OWNER_USER_ID, WORKSPACE_ID)).thenReturn(List.of());

        assertThat(controller.setAiHubUserConnectorToolRequiresApproval(WORKSPACE_ID, 55L, "sendMessage", true))
            .isFalse();
        assertThat(controller.removeAiHubUserConnector(WORKSPACE_ID, 55L)).isFalse();
        assertThat(controller.setAiHubUserConnectorEnabled(WORKSPACE_ID, 55L, false)).isFalse();
        assertThat(controller.setAiHubChatConnectorEnabled(WORKSPACE_ID, "thread-1", 55L, false)).isFalse();

        verify(chatToolFacade, never()).setToolRequiresApproval(anyLong(), anyString(), anyBoolean());
        verify(chatToolFacade, never()).detachComponent(anyLong());
        verify(chatToolFacade, never()).setComponentEnabled(anyLong(), anyBoolean());
        verify(chatToolFacade, never()).setChatConnectorEnabled(anyLong(), anyLong(), anyBoolean());
    }

    private void signIn(long userId) {
        User user = mock(User.class);

        when(user.getId()).thenReturn(userId);
        when(userService.getCurrentUser()).thenReturn(user);
    }

    private static AiHubChatToolBinding binding(boolean requiresApproval) {
        return new AiHubChatToolBinding(
            5L, 99L, CHAT_ID, "slack", 1, "sendMessage", 42L, 1, Map.of(), requiresApproval);
    }

    private static AttachAiHubChatToolInput attachInput(Long connectionId) {
        return new AttachAiHubChatToolInput(
            WORKSPACE_ID, "thread-1", "slack", 1, "sendMessage", connectionId, 0, null);
    }
}
