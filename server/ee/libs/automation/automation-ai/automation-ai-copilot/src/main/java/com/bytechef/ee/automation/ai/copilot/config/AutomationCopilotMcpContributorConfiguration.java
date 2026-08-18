/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.automation.ai.copilot.tool.ContextStoreAgentToolCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes the EE Copilot domain subagents ({@code context_store_agent}, {@code buildCustomComponent},
 * {@code buildCodeWorkflow}) to the management MCP server through the {@link McpServerToolCallbackContributor} SPI —
 * the EE counterpart of the CE {@code ToolCallbackContributorConfiguration}, which contributes only CE-domain
 * subagents. {@code buildCustomComponent} and {@code buildCodeWorkflow} are contributed via
 * {@link IntelligentToolCatalog#getByNames}, over the definitions registered by
 * {@link AutomationIntelligentToolContributor}; {@code context_store_agent} is contributed directly (it is a CRUD
 * delegate, not yet migrated onto the catalog). Each domain exposes its BUILD subagent {@link ChatClient} (defined in
 * {@link AutomationCopilotConfiguration} / {@link ContextStoreAgentConfiguration}), whose tool set includes the
 * read-only tools, so the single delegate tool serves both ask-style and build-style requests. An absent chat client
 * bean (surface toggles off, context-store feature disabled) skips silently. Each delegate is wrapped in
 * {@link WorkspaceScopedSubAgentToolCallback} because the management MCP surface has no AI Hub chat state to supply
 * workspace scope.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class AutomationCopilotMcpContributorConfiguration {

    /**
     * Names of the {@link com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition}s this contributor owns on the
     * management MCP surface, contributed by {@link AutomationIntelligentToolContributor}. Filtered with
     * {@link IntelligentToolCatalog#getByNames} over its own name partition, never the whole catalog, because the CE
     * {@code ToolCallbackContributorConfiguration} and the embedded {@code EmbeddedCopilotMcpContributorConfiguration}
     * register their own management-MCP contributor configs against the same catalog — {@code getAll} here would
     * double-register their delegates.
     *
     * <p>
     * Public so {@code IntelligentToolSurfaceParityTest} (ai-hub-service) can assert this set is disjoint from, and
     * unions with, the other management-MCP surfaces' name sets to equal the full catalog.
     * </p>
     */
    public static final Set<String> INTELLIGENT_TOOL_NAMES = Set.of("buildCustomComponent", "buildCodeWorkflow");

    @Bean
    McpServerToolCallbackContributor automationCopilotAgentToolCallbackContributor(
        @Qualifier("contextStoreBuildSubAgentChatClient") ObjectProvider<ChatClient> contextStoreProvider,
        IntelligentToolCatalog intelligentToolCatalog, WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            contextStoreProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new ContextStoreAgentToolCallback(chatClient), workspaceService)));

            toolCallbacks.addAll(
                intelligentToolCatalog.getByNames(
                    INTELLIGENT_TOOL_NAMES, IntelligentToolVariant.BUILD, (chatClient, definition) -> chatClient,
                    (toolCallback, definition) -> new WorkspaceScopedSubAgentToolCallback(
                        toolCallback, workspaceService)));

            return toolCallbacks;
        };
    }
}
