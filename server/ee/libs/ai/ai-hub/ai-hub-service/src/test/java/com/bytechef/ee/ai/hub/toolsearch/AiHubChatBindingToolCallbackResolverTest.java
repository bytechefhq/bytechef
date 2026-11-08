/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolBinding;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade;
import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpToolCallback;
import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpToolCallbackProvider;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 * @author Ivica Cardic
 */
class AiHubChatBindingToolCallbackResolverTest {

    private static final long CHAT_ID = 7L;
    private static final long OWNER_USER_ID = 10L;
    private static final long WORKSPACE_ID = 1L;

    private final AiHubChatService chatService = mock(AiHubChatService.class);
    private final AiHubChatToolFacade chatToolFacade = mock(AiHubChatToolFacade.class);
    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final AiHubMcpToolCallbackProvider mcpToolCallbackProvider = mock(AiHubMcpToolCallbackProvider.class);
    private final AiHubChatBindingToolCallbackResolver resolver = new AiHubChatBindingToolCallbackResolver(
        chatService, chatToolFacade, clusterElementDefinitionService, mock(ClusterElementDefinitionFacade.class),
        mock(ConnectionDefinitionService.class), mock(CopilotConnectionLister.class), mcpToolCallbackProvider);

    @BeforeEach
    void beforeEach() {
        AiHubChat chat = mock(AiHubChat.class);

        when(chat.getId()).thenReturn(CHAT_ID);
        when(chat.getUserId()).thenReturn(OWNER_USER_ID);
        when(chatService.findNotDeletedByThreadId("thread-1")).thenReturn(Optional.of(chat));
        when(chatService.getWorkspaceId(CHAT_ID)).thenReturn(WORKSPACE_ID);
        when(chatToolFacade.listChatDisabledConnectors(CHAT_ID)).thenReturn(Set.of());

        stubDefinition("slack", "sendMessage", "Send a message");
        stubDefinition("slack", "listChannels", "List channels");
        stubDefinition("github", "createIssue", "Create an issue");
    }

    @Test
    void testResolveReturnsEmptyWhenChatNotFound() {
        when(chatService.findNotDeletedByThreadId("missing-thread")).thenReturn(Optional.empty());

        assertThat(resolver.resolve("missing-thread", OWNER_USER_ID, Mode.BUILD)).isEmpty();
    }

    @Test
    void testResolveBuildsPinnedCallbacksWithConnectionParametersAndApprovalFlag() {
        when(chatToolFacade.listChatTools(CHAT_ID)).thenReturn(
            List.of(
                binding(11L, "slack", "sendMessage", 42L, Map.of("channel", "#engineering"), true),
                binding(12L, "github", "createIssue", 50L, Map.of("repo", "acme/site"), false)));

        List<ToolCallback> toolCallbacks = resolver.resolve("thread-1", OWNER_USER_ID, Mode.BUILD);

        assertThat(toolCallbacks).extracting(AiHubChatBindingToolCallbackResolverTest::toolName)
            .containsExactly("slack_sendMessage", "github_createIssue");

        ClusterElementToolCallback slackToolCallback = (ClusterElementToolCallback) toolCallbacks.get(0);

        assertThat(slackToolCallback.getPinnedConnectionId()).isEqualTo(42L);
        assertThat(slackToolCallback.isRequiresApproval()).isTrue();
        assertThat(((ClusterElementToolCallback) toolCallbacks.get(1)).isRequiresApproval()).isFalse();
    }

    @Test
    void testResolveKeepsOnlyReadOnlyComponentToolsInAskMode() {
        when(chatToolFacade.listChatTools(CHAT_ID)).thenReturn(
            List.of(
                binding(11L, "slack", "sendMessage", 42L, Map.of(), false),
                binding(12L, "slack", "listChannels", 42L, Map.of(), false)));

        List<ToolCallback> toolCallbacks = resolver.resolve("thread-1", OWNER_USER_ID, Mode.ASK);

        assertThat(toolCallbacks).extracting(AiHubChatBindingToolCallbackResolverTest::toolName)
            .containsExactly("slack_listChannels");
    }

    @Test
    void testResolveSkipsBindingsWhoseClusterElementVanished() {
        when(clusterElementDefinitionService.getClusterElementDefinition("github", 1, "createIssue"))
            .thenThrow(new IllegalArgumentException("gone"));
        when(chatToolFacade.listChatTools(CHAT_ID)).thenReturn(
            List.of(
                binding(11L, "slack", "sendMessage", 42L, Map.of(), false),
                binding(12L, "github", "createIssue", 50L, Map.of(), false)));

        assertThat(resolver.resolve("thread-1", OWNER_USER_ID, Mode.BUILD))
            .extracting(AiHubChatBindingToolCallbackResolverTest::toolName)
            .containsExactly("slack_sendMessage");
    }

    @Test
    void testResolveLetsChatBindingsOverrideUserConnectorsAndDropsConnectorsTheChatSwitchedOff() {
        when(chatToolFacade.listChatTools(CHAT_ID))
            .thenReturn(List.of(binding(11L, "slack", "sendMessage", 42L, Map.of(), false)));
        when(chatToolFacade.listUserTools(OWNER_USER_ID, WORKSPACE_ID)).thenReturn(
            List.of(
                binding(21L, "slack", "sendMessage", 99L, Map.of(), false),
                binding(22L, "github", "createIssue", 50L, Map.of(), false)));
        when(chatToolFacade.listChatDisabledConnectors(CHAT_ID)).thenReturn(Set.of("github"));

        List<ToolCallback> toolCallbacks = resolver.resolve("thread-1", OWNER_USER_ID, Mode.BUILD);

        assertThat(toolCallbacks).extracting(AiHubChatBindingToolCallbackResolverTest::toolName)
            .containsExactly("slack_sendMessage");
        assertThat(((ClusterElementToolCallback) toolCallbacks.get(0)).getPinnedConnectionId()).isEqualTo(42L);
    }

    @Test
    void testResolveOmitsUserConnectorsAndMcpToolsForANonOwnerSender() {
        when(chatToolFacade.listChatTools(CHAT_ID))
            .thenReturn(List.of(binding(11L, "slack", "sendMessage", 42L, Map.of(), false)));

        List<ToolCallback> toolCallbacks = resolver.resolve("thread-1", 20L, Mode.BUILD);

        assertThat(toolCallbacks).extracting(AiHubChatBindingToolCallbackResolverTest::toolName)
            .containsExactly("slack_sendMessage");

        verify(chatToolFacade, never()).listUserTools(anyLong(), anyLong());
        verify(mcpToolCallbackProvider, never()).resolve(anyLong(), anyLong());
    }

    @Test
    void testResolveIncludesTheOwnersMcpToolsAndKeepsOnlyReadOnlyOnesInAskMode() {
        when(chatToolFacade.listChatTools(CHAT_ID)).thenReturn(List.of());
        List<AiHubMcpToolCallback> mcpToolCallbacks =
            List.of(mcpToolCallback("search_docs", true), mcpToolCallback("delete_page", false));

        when(mcpToolCallbackProvider.resolve(OWNER_USER_ID, WORKSPACE_ID)).thenReturn(mcpToolCallbacks);

        assertThat(resolver.resolve("thread-1", OWNER_USER_ID, Mode.BUILD))
            .extracting(AiHubChatBindingToolCallbackResolverTest::toolName)
            .containsExactly("search_docs", "delete_page");
        assertThat(resolver.resolve("thread-1", OWNER_USER_ID, Mode.ASK))
            .extracting(AiHubChatBindingToolCallbackResolverTest::toolName)
            .containsExactly("search_docs");
    }

    @Test
    void testResolveContinuesWithoutMcpToolsWhenTheirResolutionFails() {
        when(chatToolFacade.listChatTools(CHAT_ID))
            .thenReturn(List.of(binding(11L, "slack", "sendMessage", 42L, Map.of(), false)));
        when(mcpToolCallbackProvider.resolve(OWNER_USER_ID, WORKSPACE_ID))
            .thenThrow(new IllegalStateException("unreachable"));

        assertThat(resolver.resolve("thread-1", OWNER_USER_ID, Mode.BUILD))
            .extracting(AiHubChatBindingToolCallbackResolverTest::toolName)
            .containsExactly("slack_sendMessage");
    }

    private void stubDefinition(String componentName, String clusterElementName, String description) {
        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getDescription()).thenReturn(description);
        when(clusterElementDefinitionService.getClusterElementDefinition(componentName, 1, clusterElementName))
            .thenReturn(clusterElementDefinition);
    }

    private static AiHubChatToolBinding binding(
        long chatToolId, String componentName, String clusterElementName, Long connectionId,
        Map<String, ?> parameters, boolean requiresApproval) {

        return new AiHubChatToolBinding(
            chatToolId, 99L, CHAT_ID, componentName, 1, clusterElementName, connectionId, 0, parameters,
            requiresApproval);
    }

    private static AiHubMcpToolCallback mcpToolCallback(String toolName, boolean readOnly) {
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name(toolName)
                .description(toolName)
                .inputSchema("{}")
                .build());

        return new AiHubMcpToolCallback(toolCallback, 5L, readOnly);
    }

    private static String toolName(ToolCallback toolCallback) {
        ToolDefinition toolDefinition = toolCallback.getToolDefinition();

        return toolDefinition.name();
    }
}
