/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent.ChatToolBindingResolver;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolBinding;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade;
import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpToolCallback;
import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpToolCallbackProvider;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.ee.ai.hub.util.ToolNameNormalizer;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiHubChatBindingToolCallbackResolver implements ChatToolBindingResolver {

    private static final Logger log = LoggerFactory.getLogger(AiHubChatBindingToolCallbackResolver.class);

    private final AiHubChatService chatService;
    private final AiHubChatToolFacade chatToolFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final CopilotConnectionLister connectionLister;
    private final @Nullable AiHubMcpToolCallbackProvider mcpToolCallbackProvider;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubChatBindingToolCallbackResolver(
        AiHubChatService chatService, AiHubChatToolFacade chatToolFacade,
        ClusterElementDefinitionService clusterElementDefinitionService,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ConnectionDefinitionService connectionDefinitionService, CopilotConnectionLister connectionLister,
        @Nullable AiHubMcpToolCallbackProvider mcpToolCallbackProvider) {

        this.chatService = chatService;
        this.chatToolFacade = chatToolFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionLister = connectionLister;
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
    }

    @Override
    public List<ToolCallback> resolve(String threadId, long senderUserId, Mode mode) {
        Optional<AiHubChat> chatOptional = chatService.findNotDeletedByThreadId(threadId);

        if (chatOptional.isEmpty()) {
            return List.of();
        }

        AiHubChat chat = chatOptional.get();

        long ownerUserId = chat.getUserId();
        long workspaceId = chatService.getWorkspaceId(chat.getId());

        boolean senderIsOwner = ownerUserId == senderUserId;

        List<AiHubChatToolBinding> chatBindings = chatToolFacade.listChatTools(chat.getId());
        List<AiHubChatToolBinding> userBindings =
            senderIsOwner ? chatToolFacade.listUserTools(ownerUserId, workspaceId) : List.of();

        Set<String> disabledConnectors =
            userBindings.isEmpty() ? Set.of() : chatToolFacade.listChatDisabledConnectors(chat.getId());

        List<ToolCallback> toolCallbacks = new ArrayList<>(chatBindings.size() + userBindings.size());
        Set<String> toolNames = new HashSet<>();

        for (AiHubChatToolBinding binding : chatBindings) {
            addBinding(binding, mode, toolNames, toolCallbacks);
        }

        for (AiHubChatToolBinding binding : userBindings) {
            if (disabledConnectors.contains(binding.componentName())) {
                continue;
            }

            addBinding(binding, mode, toolNames, toolCallbacks);
        }

        if (senderIsOwner && mcpToolCallbackProvider != null) {
            try {
                for (AiHubMcpToolCallback mcpToolCallback : mcpToolCallbackProvider.resolve(ownerUserId, workspaceId)) {
                    if (mode == Mode.BUILD || mcpToolCallback.isReadOnly()) {
                        toolCallbacks.add(mcpToolCallback);
                    }
                }
            } catch (RuntimeException exception) {
                log.warn(
                    "MCP server tool resolution failed for user={}, workspace={}; continuing without MCP tools",
                    ownerUserId, workspaceId, exception);
            }
        }

        return toolCallbacks;
    }

    private void addBinding(
        AiHubChatToolBinding binding, Mode mode, Set<String> toolNames, List<ToolCallback> toolCallbacks) {

        if (mode != Mode.BUILD && !AiHubClusterElementToolCallbacks.isReadOnly(binding.clusterElementName())) {
            return;
        }

        String toolName = ToolNameNormalizer.toToolName(binding.componentName(), binding.clusterElementName());

        if (!toolNames.add(toolName)) {
            return;
        }

        ClusterElementToolCallback toolCallback = toToolCallback(binding, toolName);

        if (toolCallback != null) {
            toolCallbacks.add(toolCallback);
        }
    }

    private @Nullable ClusterElementToolCallback toToolCallback(AiHubChatToolBinding binding, String toolName) {
        ClusterElementDefinition clusterElementDefinition;

        try {
            clusterElementDefinition = clusterElementDefinitionService.getClusterElementDefinition(
                binding.componentName(), binding.componentVersion(), binding.clusterElementName());
        } catch (RuntimeException exception) {
            log.warn(
                "Skipping chat tool binding {}: cluster element {}/{} v{} is no longer available", binding.chatToolId(),
                binding.componentName(), binding.clusterElementName(), binding.componentVersion(), exception);

            return null;
        }

        String summary = ToolSearchCatalogFeeder.buildSummary(clusterElementDefinition);

        return new ClusterElementToolCallback(
            toolName, summary == null ? toolName : summary, binding.componentName(), binding.componentVersion(),
            binding.clusterElementName(), clusterElementDefinitionService, clusterElementDefinitionFacade,
            connectionDefinitionService, connectionLister, binding.connectionId(), binding.parameters(),
            binding.requiresApproval());
    }
}
