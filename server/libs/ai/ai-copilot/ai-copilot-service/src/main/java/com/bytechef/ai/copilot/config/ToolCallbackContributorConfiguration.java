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

import com.bytechef.ai.copilot.tool.ask.SubAgentQuestionRenderer;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.AssetFileToolCallbacksFactory;
import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.McpServerToolCallbacksFactory;
import com.bytechef.automation.ai.tool.SkillsTools;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.ai.tool.aiagent.AiAgentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.datatable.DataTableToolCallbacksFactory;
import com.bytechef.automation.ai.tool.knowledgebase.KnowledgeBaseToolCallbacksFactory;
import com.bytechef.automation.configuration.service.WorkspaceService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
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

    /**
     * The one of the seven flat project-deployment CRUD tools that reads
     * {@code AutomationToolInvocationContext.workspaceId()} (see {@link #deploymentFlatCrudMcpContributor}'s javadoc).
     * The six mutations ({@code createProjectDeployment}, {@code updateProjectDeployment},
     * {@code deleteProjectDeployment}, {@code rollbackProjectDeployment}, {@code toggleProjectDeployment},
     * {@code promoteWorkflow}) resolve everything from an id already in their own input and need no wrapping.
     */
    private static final Set<String> WORKSPACE_SCOPED_DEPLOYMENT_TOOL_NAMES = Set.of("listProjectDeployments");

    @Bean
    McpServerToolCallbackContributor copilotAgentToolCallbackContributor(
        ObjectProvider<SkillsTools> skillsToolsProvider,
        IntelligentToolCatalog intelligentToolCatalog, WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            skillsToolsProvider.ifAvailable(
                skillsTools -> toolCallbacks.addAll(List.of(ToolCallbacks.from(skillsTools))));

            toolCallbacks.addAll(
                intelligentToolCatalog.getByNames(
                    INTELLIGENT_TOOL_NAMES, IntelligentToolVariant.BUILD, (chatClient, definition) -> chatClient,
                    (toolCallback, definition) -> new WorkspaceScopedSubAgentToolCallback(
                        toolCallback, workspaceService),
                    SubAgentQuestionRenderer.PLAIN_TEXT));

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

    /**
     * Flattens the project-deployment CRUD tool set (ticket 732, CRUD-delegate-unwind plan, Task 3) onto the management
     * MCP surface, replacing the dissolved {@code project_deployment_agent} delegate's contribution — formerly a single
     * {@code SubAgentToolCallback} named {@code project_deployment_agent}, contributed from the now-deleted
     * {@code SubAgentMcpContributorConfiguration} in {@code automation-ai-tool}. This surface has no schema-count
     * pressure (MCP clients page {@code tools/list}), so all seven of
     * {@link DeploymentToolCallbacksFactory#writeToolCallbacks()} are registered flat — {@code listProjectDeployments},
     * {@code createProjectDeployment}, {@code updateProjectDeployment}, {@code deleteProjectDeployment},
     * {@code rollbackProjectDeployment}, {@code toggleProjectDeployment}, {@code promoteWorkflow}.
     *
     * <p>
     * Unlike the AI Hub chat surface, no {@code ToolContext} exists here to carry
     * {@code AutomationToolInvocationContext}'s workspace scope, so the one tool that reads it
     * ({@code listProjectDeployments}, see {@link #WORKSPACE_SCOPED_DEPLOYMENT_TOOL_NAMES}) is wrapped in
     * {@link WorkspaceScopedFlatToolCallback}, mirroring {@link #mcpServerCrudMcpContributor}. The six mutations need
     * no wrapping — they resolve everything from an id already in their own input, a pre-existing property of these
     * tool classes unaffected by dissolving the delegate that used to wrap them.
     * </p>
     *
     * <p>
     * Absent factory bean (both Copilot AND AI Hub disabled — {@link DeploymentToolCallbacksFactory} is registered
     * whenever either is, see {@code DeploymentAgentConfiguration}) resolves to an empty list.
     * </p>
     */
    @Bean
    McpServerToolCallbackContributor deploymentFlatCrudMcpContributor(
        ObjectProvider<DeploymentToolCallbacksFactory> deploymentToolCallbacksFactoryProvider,
        WorkspaceService workspaceService) {

        return () -> {
            DeploymentToolCallbacksFactory deploymentToolCallbacksFactory = deploymentToolCallbacksFactoryProvider
                .getIfAvailable();

            if (deploymentToolCallbacksFactory == null) {
                return List.of();
            }

            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : deploymentToolCallbacksFactory.writeToolCallbacks()) {
                String name = toolCallback.getToolDefinition()
                    .name();

                toolCallbacks.add(
                    WORKSPACE_SCOPED_DEPLOYMENT_TOOL_NAMES.contains(name)
                        ? new WorkspaceScopedFlatToolCallback(toolCallback, workspaceService)
                        : toolCallback);
            }

            return toolCallbacks;
        };
    }

    /**
     * Flattens the asset-file CRUD tool set (ticket 732, CRUD-delegate-unwind plan, Task 4) onto the management MCP
     * surface, replacing the dissolved {@code asset_file_agent} delegate's contribution — formerly built by hand in
     * {@link #copilotAgentToolCallbackContributor} from the {@code assetFileBuildSubAgentChatClient} bean. This surface
     * has no schema-count pressure (MCP clients page {@code tools/list}), so all seven of
     * {@link AssetFileToolCallbacksFactory#writeToolCallbacks()} are registered flat — {@code listAssetFiles},
     * {@code getAssetFileContent}, {@code createAssetFile}, {@code createBinaryAssetFile},
     * {@code updateAssetFileContent}, {@code cloneAssetFile}, {@code createAssetFileFromUrl}.
     *
     * <p>
     * Unlike {@link #deploymentFlatCrudMcpContributor} and {@link #mcpServerCrudMcpContributor}, EVERY one of the seven
     * tools reads {@code AutomationToolInvocationContext.workspaceId()} — none of them accept a workspace id as an
     * explicit input field — so all seven, not just the reads, are wrapped in {@link WorkspaceScopedFlatToolCallback}
     * (which also unconditionally supplies {@code sourceOrdinal} — see that class's Javadoc — so a file created through
     * this surface is attributed to the Files source, matching what the dissolved delegate's MCP wrapping used to do
     * via {@code WorkspaceScopedSubAgentToolCallback}).
     * </p>
     *
     * <p>
     * Absent factory bean (both Copilot AND AI Hub disabled — {@link AssetFileToolCallbacksFactory} is registered
     * whenever either is, see {@code AssetFileAgentConfiguration}) resolves to an empty list.
     * </p>
     */
    @Bean
    McpServerToolCallbackContributor assetFileFlatCrudMcpContributor(
        ObjectProvider<AssetFileToolCallbacksFactory> assetFileToolCallbacksFactoryProvider,
        WorkspaceService workspaceService) {

        return () -> {
            AssetFileToolCallbacksFactory assetFileToolCallbacksFactory = assetFileToolCallbacksFactoryProvider
                .getIfAvailable();

            if (assetFileToolCallbacksFactory == null) {
                return List.of();
            }

            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : assetFileToolCallbacksFactory.writeToolCallbacks()) {
                toolCallbacks.add(new WorkspaceScopedFlatToolCallback(toolCallback, workspaceService));
            }

            return toolCallbacks;
        };
    }

    /**
     * Flattens the data-table CRUD tool set (ticket 732, CRUD-delegate-unwind plan, Task 5) onto the management MCP
     * surface, replacing the dissolved {@code data_table_agent} delegate's contribution — formerly built by hand in
     * {@link #copilotAgentToolCallbackContributor} from the {@code dataTableBuildSubAgentChatClient} bean. This surface
     * has no schema-count pressure (MCP clients page {@code tools/list}), so all eleven of
     * {@link DataTableToolCallbacksFactory#writeToolCallbacks()} are registered flat — {@code listDataTables},
     * {@code queryDataTable}, {@code aggregateDataTable}, {@code addDataTableRow}, {@code updateDataTableRow},
     * {@code deleteDataTableRow}, {@code addDataTableColumn}, {@code createDataTable}, {@code createDataTableFromCsv},
     * {@code cloneDataTable}, {@code dropDataTable} — unlike the AI Hub chat surface (see
     * {@code AiHubConfiguration#dataTableFlatCrudToolCallbacks}/{@code #dataTableCatalogToolCallbacks}), which splits
     * the three reads (pinned) from the eight mutations (searchable catalog) to keep its pinned tool-schema list small.
     *
     * <p>
     * Every one of the eleven tools reads {@code com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext
     * .workspaceId()} — the OTHER key family from every domain flattened onto this surface before this task
     * ({@code AutomationToolInvocationContext}'s) — and none of them accept a workspace id as an explicit input field,
     * so all eleven are wrapped in {@link WorkspaceScopedFlatToolCallback}, which now writes BOTH key families
     * unconditionally (see that class's Javadoc) so this one wrapper still serves every flat domain on this surface.
     * </p>
     *
     * <p>
     * Absent factory bean (both Copilot AND AI Hub disabled — {@link DataTableToolCallbacksFactory} is registered
     * whenever either is, see {@code DataTableAgentConfiguration}) resolves to an empty list.
     * </p>
     */
    @Bean
    McpServerToolCallbackContributor dataTableFlatCrudMcpContributor(
        ObjectProvider<DataTableToolCallbacksFactory> dataTableToolCallbacksFactoryProvider,
        WorkspaceService workspaceService) {

        return () -> {
            DataTableToolCallbacksFactory dataTableToolCallbacksFactory = dataTableToolCallbacksFactoryProvider
                .getIfAvailable();

            if (dataTableToolCallbacksFactory == null) {
                return List.of();
            }

            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : dataTableToolCallbacksFactory.writeToolCallbacks()) {
                toolCallbacks.add(new WorkspaceScopedFlatToolCallback(toolCallback, workspaceService));
            }

            return toolCallbacks;
        };
    }

    /**
     * Flattens the knowledge-base CRUD tool set (ticket 732, CRUD-delegate-unwind plan, Task 6) onto the management MCP
     * surface, replacing the dissolved {@code knowledge_base_agent} delegate's contribution — formerly built by hand in
     * {@link #copilotAgentToolCallbackContributor} from the {@code knowledgeBaseBuildSubAgentChatClient} bean. This
     * surface has no schema-count pressure (MCP clients page {@code tools/list}), so all seven of
     * {@link KnowledgeBaseToolCallbacksFactory#writeToolCallbacks()} are registered flat — {@code listKnowledgeBases},
     * {@code queryKnowledgeBase}, {@code createKnowledgeBase}, {@code addKnowledgeBaseDocument},
     * {@code deleteKnowledgeBaseDocument}, {@code cloneKnowledgeBase}, {@code deleteKnowledgeBase} — unlike the AI Hub
     * chat surface (see {@code AiHubConfiguration#knowledgeBaseFlatCrudToolCallbacks}/
     * {@code #knowledgeBaseCatalogToolCallbacks}), which splits the two reads (pinned) from the five mutations
     * (searchable catalog) to keep its pinned tool-schema list small.
     *
     * <p>
     * Six of the seven tools read {@code com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext
     * .workspaceId()} — the OTHER key family from every domain flattened onto this surface before Task 5
     * ({@code AutomationToolInvocationContext}'s) — and none of them accept a workspace id as an explicit input field.
     * Only {@code deleteKnowledgeBase} does not read it (authorization is enforced by
     * {@code WorkspaceKnowledgeBaseFacade#deleteWorkspaceKnowledgeBase} itself, keyed off the id alone). All seven are
     * wrapped in {@link WorkspaceScopedFlatToolCallback} uniformly anyway — mirroring
     * {@link #dataTableFlatCrudMcpContributor} and {@link #assetFileFlatCrudMcpContributor} — since that wrapper writes
     * both key families unconditionally (see its Javadoc) and the extra, unused workspaceId/environment fields on
     * {@code deleteKnowledgeBase}'s schema are harmless.
     * </p>
     *
     * <p>
     * Absent factory bean (both Copilot AND AI Hub disabled, or the knowledge-base feature itself off —
     * {@link KnowledgeBaseToolCallbacksFactory} is registered only when {@code bytechef.ai.knowledge-base.enabled=true}
     * AND either surface is on, see {@code KnowledgeBaseAgentConfiguration}) resolves to an empty list.
     * </p>
     */
    @Bean
    McpServerToolCallbackContributor knowledgeBaseFlatCrudMcpContributor(
        ObjectProvider<KnowledgeBaseToolCallbacksFactory> knowledgeBaseToolCallbacksFactoryProvider,
        WorkspaceService workspaceService) {

        return () -> {
            KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory =
                knowledgeBaseToolCallbacksFactoryProvider
                    .getIfAvailable();

            if (knowledgeBaseToolCallbacksFactory == null) {
                return List.of();
            }

            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : knowledgeBaseToolCallbacksFactory.writeToolCallbacks()) {
                toolCallbacks.add(new WorkspaceScopedFlatToolCallback(toolCallback, workspaceService));
            }

            return toolCallbacks;
        };
    }

    /**
     * Flattens the AI Agent (agent-builder) CRUD tool set (ticket 732, CRUD-delegate-unwind plan, Task 8 — the LAST
     * delegate in the plan) onto the management MCP surface, replacing the dissolved {@code ai_agent_agent} delegate's
     * contribution — formerly built by hand in {@link #copilotAgentToolCallbackContributor} from the
     * {@code aiAgentBuildSubAgentChatClient} bean. This surface has no schema-count pressure (MCP clients page
     * {@code tools/list}), so all eleven of {@link AiAgentToolCallbacksFactory#writeToolCallbacks()} are registered
     * flat — {@code listAiAgents}, {@code getAiAgent}, {@code createAiAgent}, {@code updateAiAgent},
     * {@code addAiAgentChannel}, {@code deleteAiAgentChannel}, {@code addAiAgentElement}, {@code updateAiAgentElement},
     * {@code deleteAiAgentElement}, {@code updateAiAgentSettings}, {@code publishAiAgent} — unlike the AI Hub chat
     * surface (see {@code AiHubConfiguration#aiAgentFlatCrudToolCallbacks}/{@code #aiAgentCatalogToolCallbacks}), which
     * splits the two reads (pinned) from the nine mutations (searchable catalog) to keep its pinned tool-schema list
     * small.
     *
     * <p>
     * Only {@code listAiAgents} and {@code createAiAgent} read
     * {@code com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext.workspaceId()} — the OTHER key family from
     * every domain flattened onto this surface before Task 5 ({@code AutomationToolInvocationContext}'s). The other
     * nine resolve everything from an id already in their own input; authorization for those is the same
     * {@code isAuthenticated()}-only posture documented on {@code AiAgentFacadeImpl}, unaffected by dissolving the
     * delegate that used to wrap them. All eleven are wrapped in {@link WorkspaceScopedFlatToolCallback} uniformly
     * anyway — mirroring {@link #dataTableFlatCrudMcpContributor} and {@link #knowledgeBaseFlatCrudMcpContributor} —
     * since that wrapper writes both key families unconditionally (see its Javadoc) and the extra, unused
     * workspaceId/environment fields on the other nine tools' schemas are harmless.
     * </p>
     *
     * <p>
     * Absent factory bean (both Copilot AND AI Hub disabled — {@link AiAgentToolCallbacksFactory} is registered
     * whenever either is, see {@code AiAgentAgentConfiguration}) resolves to an empty list.
     * </p>
     */
    @Bean
    McpServerToolCallbackContributor aiAgentFlatCrudMcpContributor(
        ObjectProvider<AiAgentToolCallbacksFactory> aiAgentToolCallbacksFactoryProvider,
        WorkspaceService workspaceService) {

        return () -> {
            AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory = aiAgentToolCallbacksFactoryProvider
                .getIfAvailable();

            if (aiAgentToolCallbacksFactory == null) {
                return List.of();
            }

            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : aiAgentToolCallbacksFactory.writeToolCallbacks()) {
                toolCallbacks.add(new WorkspaceScopedFlatToolCallback(toolCallback, workspaceService));
            }

            return toolCallbacks;
        };
    }
}
