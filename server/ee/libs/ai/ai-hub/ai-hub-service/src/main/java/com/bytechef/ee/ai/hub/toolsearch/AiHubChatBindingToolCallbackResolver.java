/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent.ChatToolBindingResolver;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolBinding;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade;
import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpToolCallbackProvider;
import com.bytechef.ee.ai.hub.skill.AiHubSkillsToolProvider;
import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.ee.ai.hub.util.ToolNameNormalizer;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.util.JsonSchemaGeneratorUtils;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;

/**
 * Per-request bridge between the persisted {@link AiHubChatToolBinding}s and Spring AI's tool callback list. Looks up
 * the chat by thread id, fetches its attached tools, and synthesizes one {@link ClusterElementToolCallback} per binding
 * — with the binding's pinned connection and pre-set parameters baked in so each invocation runs against exactly what
 * the user attached.
 *
 * <p>
 * Defensive in three ways:
 * </p>
 * <ul>
 * <li>Returns empty list when the workspace context lacks a thread id (no chat to resolve against).</li>
 * <li>Returns empty list when the chat row isn't found (race or stale id).</li>
 * <li>Skips individual bindings whose cluster element no longer exists in the catalog (component upgrade removed the
 * action) — log + continue so the rest still register.</li>
 * </ul>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiHubChatBindingToolCallbackResolver implements ChatToolBindingResolver {

    private static final Logger log =
        LoggerFactory.getLogger(AiHubChatBindingToolCallbackResolver.class);

    private final AiHubChatService chatService;
    private final AiHubChatToolFacade chatToolFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ConnectionService connectionService;
    private final AiHubMcpToolCallbackProvider mcpToolCallbackProvider;
    private final AiHubSkillsToolProvider skillsToolCallbackProvider;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubChatBindingToolCallbackResolver(
        AiHubChatService chatService,
        AiHubChatToolFacade chatToolFacade,
        ClusterElementDefinitionService clusterElementDefinitionService, ConnectionService connectionService,
        AiHubMcpToolCallbackProvider mcpToolCallbackProvider, AiHubSkillsToolProvider skillsToolCallbackProvider) {

        this.chatService = chatService;
        this.chatToolFacade = chatToolFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.connectionService = connectionService;
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
        this.skillsToolCallbackProvider = skillsToolCallbackProvider;
    }

    @Override
    public List<ToolCallback> resolve(AiHubToolInvocationContext invocationContext) {
        if (invocationContext == null || invocationContext.threadId() == null) {
            return List.of();
        }

        Optional<AiHubChat> chat =
            chatService.findByThreadId(invocationContext.threadId());

        if (chat.isEmpty()) {
            return List.of();
        }

        AiHubChat aiHubChat = chat.get();

        long userId = aiHubChat.getUserId();
        long workspaceId = chatService.getWorkspaceId(aiHubChat.getId());

        // The agent sees the UNION of two tool sets: the chat's own attached tools and the user's globally
        // "added connectors" (the Connectors page). A tool configured at chat scope overrides the user-global
        // one for the same (component, version, clusterElement) — more specific wins.
        List<AiHubChatToolBinding> chatBindings = chatToolFacade.listChatTools(aiHubChat.getId());
        List<AiHubChatToolBinding> userBindings = chatToolFacade.listUserTools(userId, workspaceId);

        // …minus whatever this chat has switched off. The subtraction has to be explicit: listUserTools answers
        // "what has this user made available", which knows nothing about one chat, so without this the composer's
        // per-chat switch would look like it worked and change nothing the agent sees.
        Set<String> disabledConnectors = chatToolFacade.listChatDisabledConnectors(aiHubChat.getId());

        List<ToolCallback> callbacks = new ArrayList<>(chatBindings.size() + userBindings.size());
        Set<String> seen = new HashSet<>();

        for (AiHubChatToolBinding binding : chatBindings) {
            // Chat-scoped tools are always live (the user attached them to this chat explicitly); user-global
            // bindings are already enabled-filtered by listUserTools (component/tool enabled flags).
            seen.add(bindingKey(binding));

            ToolCallback callback = bindingToCallback(binding);

            if (callback != null) {
                callbacks.add(callback);
            }
        }

        for (AiHubChatToolBinding binding : userBindings) {
            if (seen.contains(bindingKey(binding)) || disabledConnectors.contains(binding.componentName())) {
                continue;
            }

            ToolCallback callback = bindingToCallback(binding);

            if (callback != null) {
                callbacks.add(callback);
            }
        }

        // External MCP server tools (the Connectors page "Custom MCP" section). Resolved defensively — a failure
        // here must never break the turn, so the agent still gets the component-backed tools above.
        try {
            callbacks.addAll(mcpToolCallbackProvider.resolve(userId, workspaceId));
        } catch (RuntimeException exception) {
            log.warn(
                "MCP server tool resolution failed for user={}, workspace={}; continuing without MCP tools", userId,
                workspaceId, exception);
        }

        // The user's AI skills, exposed as a single SkillsTool the agent can invoke (the composer slash-menu picks
        // which). Same defensive guard — a skills failure must not break the turn.
        try {
            callbacks.addAll(skillsToolCallbackProvider.resolve(userId));
        } catch (RuntimeException exception) {
            log.warn("Skill tool resolution failed for user={}; continuing without skill tools", userId, exception);
        }

        return callbacks;
    }

    private static String bindingKey(AiHubChatToolBinding binding) {
        return binding.componentName() + "#" + binding.componentVersion() + "#" + binding.clusterElementName();
    }

    /**
     * Builds one callback per binding. Returns null and logs when the cluster element can no longer be resolved
     * (component upgraded / action removed) so the agent still gets the rest of the chat's tools instead of failing the
     * whole turn.
     */
    private ToolCallback bindingToCallback(AiHubChatToolBinding binding) {
        ClusterElementDefinition definition;

        try {
            definition = clusterElementDefinitionService.getClusterElementDefinition(
                binding.componentName(), binding.componentVersion(), binding.clusterElementName());
        } catch (RuntimeException exception) {
            // Most likely cause: component author removed or renamed the action between attach time and now.
            // Surface as a WARN so ops can correlate with the chat; the user-facing remediation is
            // to detach the dead binding via removeChatTool. v2 follow-up could auto-detach here.
            log.warn(
                "Skipping chat tool binding {} — cluster element {}/{} v{} no longer in catalog",
                binding.chatToolId(), binding.componentName(), binding.clusterElementName(),
                binding.componentVersion(), exception);

            return null;
        }

        String toolName = ToolNameNormalizer.toToolName(binding.componentName(), binding.clusterElementName());

        // Description for the LLM tool definition. Mirrors the search-discovery path's formatting so the
        // chat model sees the same shape regardless of whether the tool came from a search hit or a
        // pre-attachment.
        String description = formatDescription(definition);

        String inputSchema = JsonSchemaGeneratorUtils.generateInputSchema(definition.getProperties());

        return new ClusterElementToolCallback(
            toolName, description, inputSchema, binding.componentName(), binding.componentVersion(),
            binding.clusterElementName(), clusterElementDefinitionService, connectionService,
            binding.connectionId(), binding.parameters());
    }

    private static String formatDescription(ClusterElementDefinition definition) {
        String description = definition.getDescription();
        String title = definition.getTitle();

        if (description != null && !description.isBlank()) {
            return title != null && !title.isBlank() ? title + ": " + description : description;
        }

        return title != null && !title.isBlank() ? title : "(no description)";
    }
}
