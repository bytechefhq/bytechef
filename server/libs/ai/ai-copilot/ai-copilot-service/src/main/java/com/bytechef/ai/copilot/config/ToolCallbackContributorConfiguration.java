/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.copilot.config;

import com.bytechef.ai.copilot.tool.AiAgentAgentToolCallback;
import com.bytechef.ai.copilot.tool.AssetFileAgentToolCallback;
import com.bytechef.ai.copilot.tool.DataTableAgentToolCallback;
import com.bytechef.ai.copilot.tool.KnowledgeBaseAgentToolCallback;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.McpServerToolCallbacksFactory;
import com.bytechef.automation.ai.tool.SkillsTools;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.service.WorkspaceService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class ToolCallbackContributorConfiguration {

    /**
     * Names of the {@link com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition}s this contributor owns on the
     * management MCP surface, contributed by {@link CopilotIntelligentToolContributor}. Filtered with
     * {@link IntelligentToolCatalog#getByNames} over its own name partition, never the whole catalog, because later EE
     * intelligent-tool contributors (custom_component, code_workflow, integration_workflow) register their own
     * management-MCP contributor configs against the same catalog — {@code getAll} here would double-register their
     * delegates.
     *
     * <p>
     * Public so {@code IntelligentToolSurfaceParityTest} (ai-hub-service) can assert this set is disjoint from, and
     * unions with, the other management-MCP surfaces' name sets to equal the full catalog.
     * </p>
     *
     * <p>
     * {@code configureMcpServer} is contributed by the automation-owned {@code McpServerIntelligentToolContributor}
     * (automation-ai-tool), not by this module's own {@code CopilotIntelligentToolContributor} —
     * {@link IntelligentToolCatalog#getByNames} filters the whole catalog by name regardless of which contributor
     * supplied a given definition, so it still belongs in this CE surface's owned partition alongside the six
     * CE-contributed names.
     * </p>
     */
    public static final Set<String> INTELLIGENT_TOOL_NAMES = Set.of(
        "buildWorkflow", "importWorkflow", "configureClusterElement", "writeScript", "authorSkill",
        "debugWorkflowExecution", "configureMcpServer");

    /**
     * Stays OFF this surface (ticket 732, Task 3): the tool-mapping mutation lives exclusively inside the
     * {@code configureMcpServer} intelligent tool's inner two-tool ChatClient (see
     * {@code McpServerSubAgentConfiguration}), so flattening it here too would duplicate the mapping capability on two
     * paths with different judgment behind them.
     */
    private static final String MCP_PROJECT_WORKFLOW_PARAMETERS_TOOL_NAME = "updateMcpProjectWorkflowParameters";

    /**
     * The two of the six flat MCP server CRUD tools that read {@code AutomationToolInvocationContext.workspaceId()}
     * (see {@link WorkspaceScopedFlatToolCallback}'s javadoc for why {@code listMcpServers}/{@code createMcpServer}
     * specifically). The other four ({@code updateMcpServer}, {@code createMcpProject}, {@code cloneMcpProject},
     * {@code listMcpProjectWorkflows}) resolve everything from an id already in their own input and need no wrapping.
     */
    private static final Set<String> WORKSPACE_SCOPED_MCP_TOOL_NAMES = Set.of("listMcpServers", "createMcpServer");

    @Bean
    McpServerToolCallbackContributor copilotAgentToolCallbackContributor(
        ObjectProvider<SkillsTools> skillsToolsProvider,
        @Qualifier("knowledgeBaseBuildSubAgentChatClient") ObjectProvider<ChatClient> knowledgeBaseProvider,
        @Qualifier("dataTableBuildSubAgentChatClient") ObjectProvider<ChatClient> dataTableProvider,
        @Qualifier("aiAgentBuildSubAgentChatClient") ObjectProvider<ChatClient> aiAgentProvider,
        @Qualifier("assetFileBuildSubAgentChatClient") ObjectProvider<ChatClient> assetFileProvider,
        IntelligentToolCatalog intelligentToolCatalog, WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            skillsToolsProvider.ifAvailable(
                skillsTools -> toolCallbacks.addAll(List.of(ToolCallbacks.from(skillsTools))));

            toolCallbacks.addAll(
                intelligentToolCatalog.getByNames(
                    INTELLIGENT_TOOL_NAMES, IntelligentToolVariant.BUILD, (chatClient, definition) -> chatClient,
                    (toolCallback, definition) -> new WorkspaceScopedSubAgentToolCallback(
                        toolCallback, workspaceService)));

            knowledgeBaseProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new KnowledgeBaseAgentToolCallback(chatClient), workspaceService)));
            dataTableProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new DataTableAgentToolCallback(chatClient), workspaceService)));
            aiAgentProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new AiAgentAgentToolCallback(chatClient), workspaceService)));
            // MCP has no ASK/BUILD concept and always injects the write-capable chat client, so the tool
            // description must always advertise the write tool set.
            assetFileProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new AssetFileAgentToolCallback(chatClient, true), workspaceService)));

            return toolCallbacks;
        };
    }

    /**
     * Flattens the MCP server CRUD tool set (ticket 732, Task 3) onto the management MCP surface — the
     * create/attach/enable capability the {@code mcp_agent} -> {@code configureMcpServer} promotion removed from this
     * surface (see {@code McpServerIntelligentToolContributor}'s javadoc for that history).
     * {@link McpServerToolCallbacksFactory#writeToolCallbacks()} supplies all six by construction
     * ({@code listMcpServers}, {@code listMcpProjectWorkflows}, {@code createMcpServer}, {@code updateMcpServer},
     * {@code createMcpProject}, {@code cloneMcpProject}) plus a seventh,
     * {@value #MCP_PROJECT_WORKFLOW_PARAMETERS_TOOL_NAME}, which is filtered back out — see that constant's javadoc.
     *
     * <p>
     * Unlike the AI Hub chat surface, no ToolContext exists here to carry {@code AutomationToolInvocationContext}'s
     * workspace scope, so the two tools that actually read it ({@code listMcpServers}/{@code createMcpServer}, see
     * {@link #WORKSPACE_SCOPED_MCP_TOOL_NAMES}) are wrapped in {@link WorkspaceScopedFlatToolCallback}, which merges an
     * optional {@code workspaceId}/{@code environment} into their existing schema without discarding their other fields
     * — {@link WorkspaceScopedSubAgentToolCallback} cannot be reused here since it collapses the input to
     * {@code {request: string}}, which would drop {@code createMcpServer}'s {@code name}/{@code environment}/
     * {@code enabled} fields. The other four resolve everything from an id already in their own input and need no
     * wrapping.
     * </p>
     *
     * <p>
     * Absent factory bean (Copilot disabled, or the MCP facades not on the classpath) resolves to an empty list.
     * </p>
     */
    @Bean
    McpServerToolCallbackContributor mcpServerCrudMcpContributor(
        ObjectProvider<McpServerToolCallbacksFactory> mcpServerToolCallbacksFactoryProvider,
        WorkspaceService workspaceService) {

        return () -> {
            McpServerToolCallbacksFactory mcpServerToolCallbacksFactory = mcpServerToolCallbacksFactoryProvider
                .getIfAvailable();

            if (mcpServerToolCallbacksFactory == null) {
                return List.of();
            }

            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : mcpServerToolCallbacksFactory.writeToolCallbacks()) {
                String name = toolCallback.getToolDefinition()
                    .name();

                if (MCP_PROJECT_WORKFLOW_PARAMETERS_TOOL_NAME.equals(name)) {
                    continue;
                }

                toolCallbacks.add(
                    WORKSPACE_SCOPED_MCP_TOOL_NAMES.contains(name)
                        ? new WorkspaceScopedFlatToolCallback(toolCallback, workspaceService)
                        : toolCallback);
            }

            return toolCallbacks;
        };
    }
}
