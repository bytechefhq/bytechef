/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

import com.bytechef.ai.copilot.tool.ask.SubAgentQuestionRenderer;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.automation.ai.tool.contextstore.ContextStoreToolCallbacksFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes the EE Copilot domain subagents ({@code buildCustomComponent}, {@code buildCodeWorkflow}) and the flat
 * context-store CRUD tool set to the management MCP server through the {@link McpServerToolCallbackContributor} SPI —
 * the EE counterpart of the CE {@code ToolCallbackContributorConfiguration}, which contributes only CE-domain
 * subagents/flat tool sets. {@code buildCustomComponent} and {@code buildCodeWorkflow} are contributed via
 * {@link IntelligentToolCatalog#getByNames}, over the definitions registered by
 * {@link AutomationIntelligentToolContributorConfiguration}. Each intelligent-tool domain exposes its BUILD subagent
 * {@code ChatClient} (defined in {@link AutomationCopilotConfiguration}), whose tool set includes the read-only tools,
 * so the single delegate tool serves both ask-style and build-style requests. An absent chat client bean (surface
 * toggles off) skips silently. Each delegate is wrapped in {@link WorkspaceScopedSubAgentToolCallback} because the
 * management MCP surface has no AI Hub chat state to supply workspace scope.
 *
 * <p>
 * {@code context_store_agent} — formerly a hand-rolled CRUD delegate, contributed directly rather than through the
 * catalog — is gone (ticket 732, CRUD-delegate-unwind Task 7). Its twelve tools are registered flat instead, straight
 * off {@link ContextStoreToolCallbacksFactory} (defined in {@link ContextStoreAgentConfiguration}), each wrapped in
 * {@link WorkspaceScopedFlatToolCallback} rather than {@link WorkspaceScopedSubAgentToolCallback} — see
 * {@link #contextStoreFlatCrudMcpContributor}'s javadoc for why the flat wrapper is the correct one here.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class AutomationCopilotMcpContributorConfiguration {

    /**
     * Names of the {@link com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition}s this contributor owns on the
     * management MCP surface, contributed by {@link AutomationIntelligentToolContributorConfiguration}. Filtered with
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
        IntelligentToolCatalog intelligentToolCatalog, WorkspaceService workspaceService) {

        return () -> intelligentToolCatalog.getByNames(
            INTELLIGENT_TOOL_NAMES, IntelligentToolVariant.BUILD, (chatClient, definition) -> chatClient,
            (toolCallback, definition) -> new WorkspaceScopedSubAgentToolCallback(toolCallback, workspaceService),
            SubAgentQuestionRenderer.PLAIN_TEXT);
    }

    /**
     * Flattens the context-store CRUD tool set (ticket 732, CRUD-delegate-unwind plan, Task 7) onto the management MCP
     * surface, replacing the dissolved {@code context_store_agent} delegate's contribution — formerly built by hand in
     * {@link #automationCopilotAgentToolCallbackContributor} from the {@code contextStoreBuildSubAgentChatClient} bean.
     * This surface has no schema-count pressure (MCP clients page {@code tools/list}), so all twelve of
     * {@link ContextStoreToolCallbacksFactory#writeToolCallbacks()} are registered flat — {@code listContextSources},
     * {@code searchContextStore}, {@code getContextStoreRecord}, {@code listAvailableSourceComponents},
     * {@code describeSourceComponentEntities}, {@code semanticSearchContextStore}, {@code createContextStoreSource},
     * {@code updateContextStoreSource}, {@code deleteContextStoreSource}, {@code refreshContextStoreSource},
     * {@code setContextStoreSourceEnabled}, {@code deleteContextStore} — unlike the AI Hub chat surface (see
     * {@code AiHubConfiguration
     * #contextStoreFlatCrudToolCallbacks}/{@code #contextStoreCatalogToolCallbacks}), which splits the six reads
     * (pinned) from the six mutations (searchable catalog) to keep its pinned tool-schema list small.
     *
     * <p>
     * Six of the twelve tools ({@code listContextSources}, {@code searchContextStore}, {@code getContextStoreRecord},
     * {@code semanticSearchContextStore}, {@code createContextStoreSource}, {@code deleteContextStore}) read
     * {@code com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext.workspaceId()}. Four
     * ({@code updateContextStoreSource}, {@code deleteContextStoreSource}, {@code refreshContextStoreSource},
     * {@code setContextStoreSourceEnabled}) instead resolve their owning workspace by looking up the source id already
     * in their own input — a pre-existing property of these tool classes unaffected by dissolving the delegate that
     * used to wrap them. The remaining two ({@code listAvailableSourceComponents},
     * {@code describeSourceComponentEntities}) describe component metadata and need no workspace scope at all. None of
     * the six workspace-scoped tools accept a workspace id as an explicit input field, so all twelve are wrapped in
     * {@link WorkspaceScopedFlatToolCallback} uniformly anyway — mirroring {@code ToolCallbackContributorConfiguration
     * #dataTableFlatCrudMcpContributor}/{@code #knowledgeBaseFlatCrudMcpContributor} — since that wrapper writes both
     * key families unconditionally (see its Javadoc) and the extra, unused workspaceId/environment fields on the other
     * six tools are harmless. {@link WorkspaceScopedSubAgentToolCallback} cannot be reused here since it collapses the
     * input to {@code {request: string}}, which would drop every one of these tools' multi-field schemas (e.g.
     * {@code createContextStoreSource}'s {@code name}/{@code sourceComponentName}/{@code indexedFields}/… fields).
     * </p>
     *
     * <p>
     * Absent factory bean (both Copilot AND AI Hub disabled, or the context-store feature itself off —
     * {@link ContextStoreToolCallbacksFactory} is registered only when {@code bytechef.context-store.enabled=true} AND
     * either surface is on, see {@link ContextStoreAgentConfiguration}) resolves to an empty list.
     * </p>
     */
    @Bean
    McpServerToolCallbackContributor contextStoreFlatCrudMcpContributor(
        ObjectProvider<ContextStoreToolCallbacksFactory> contextStoreToolCallbacksFactoryProvider,
        WorkspaceService workspaceService) {

        return () -> {
            ContextStoreToolCallbacksFactory contextStoreToolCallbacksFactory = contextStoreToolCallbacksFactoryProvider
                .getIfAvailable();

            if (contextStoreToolCallbacksFactory == null) {
                return List.of();
            }

            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : contextStoreToolCallbacksFactory.writeToolCallbacks()) {
                toolCallbacks.add(new WorkspaceScopedFlatToolCallback(toolCallback, workspaceService));
            }

            return toolCallbacks;
        };
    }
}
