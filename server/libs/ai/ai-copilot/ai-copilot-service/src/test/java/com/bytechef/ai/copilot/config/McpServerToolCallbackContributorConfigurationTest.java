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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.ai.tool.AssetFileToolCallbacksFactory;
import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.aiagent.AiAgentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.datatable.DataTableToolCallbacksFactory;
import com.bytechef.automation.ai.tool.knowledgebase.KnowledgeBaseToolCallbacksFactory;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class McpServerToolCallbackContributorConfigurationTest {

    private final ToolCallbackContributorConfiguration configuration =
        new ToolCallbackContributorConfiguration();

    @Test
    void contributesAgentCallbacksWhenChatClientsPresent() {
        IntelligentToolCatalog intelligentToolCatalog = catalogOf(
            intelligentDefinition("buildWorkflow"), intelligentDefinition("writeScript"),
            intelligentDefinition("configureClusterElement"), intelligentDefinition("authorSkill"),
            intelligentDefinition("debugWorkflowExecution"), intelligentDefinition("importWorkflow"));

        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), intelligentToolCatalog, mock(WorkspaceService.class));

        // Pins that copilotAgentToolCallbackContributor's getByNames call correctly passes the fed-in fake
        // definitions through to its output; the real ChatClient-to-callback wiring for those six is
        // CopilotIntelligentToolContributorTest's concern, since the fakes here return a canned ToolCallback
        // regardless of how it was built. asset_file_agent (ticket 732, Task 4), data_table_agent (Task 5),
        // knowledge_base_agent (Task 6), context_store_agent (Task 7), and ai_agent_agent (Task 8 — the LAST
        // CRUD-delegate-unwind task) are all gone from this list — their tools are flattened onto
        // assetFileFlatCrudMcpContributor, dataTableFlatCrudMcpContributor, knowledgeBaseFlatCrudMcpContributor,
        // and aiAgentFlatCrudMcpContributor instead (context_store_agent flattens onto an EE-owned contributor).
        assertThat(contributor.getToolCallbacks())
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder(
                "buildWorkflow", "writeScript", "configureClusterElement", "authorSkill",
                "debugWorkflowExecution", "importWorkflow");
    }

    @Test
    void contributesNothingWhenAllAbsent() {
        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), catalogOf(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void contributedAgentToolsAcceptWorkspaceId() {
        IntelligentToolCatalog intelligentToolCatalog = catalogOf(
            intelligentDefinition("buildWorkflow"), intelligentDefinition("writeScript"),
            intelligentDefinition("configureClusterElement"), intelligentDefinition("authorSkill"),
            intelligentDefinition("debugWorkflowExecution"), intelligentDefinition("importWorkflow"));

        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), intelligentToolCatalog, mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId"));
    }

    @Test
    void contributesTheSevenFlatAssetFileToolsWhenFactoryPresent() {
        AssetFileToolCallbacksFactory assetFileToolCallbacksFactory = new AssetFileToolCallbacksFactory(
            mock(AssetFileFacade.class), null);

        McpServerToolCallbackContributor contributor = configuration.assetFileFlatCrudMcpContributor(
            presentAssetFileFactory(assetFileToolCallbacksFactory), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder(
                "listAssetFiles", "getAssetFileContent", "createAssetFile", "createBinaryAssetFile",
                "updateAssetFileContent", "cloneAssetFile", "createAssetFileFromUrl");
    }

    @Test
    void assetFileContributorSkipsWhenFactoryAbsent() {
        McpServerToolCallbackContributor contributor = configuration.assetFileFlatCrudMcpContributor(
            absentAssetFileFactory(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void everyAssetFileToolAcceptsWorkspaceId() {
        AssetFileToolCallbacksFactory assetFileToolCallbacksFactory = new AssetFileToolCallbacksFactory(
            mock(AssetFileFacade.class), null);

        McpServerToolCallbackContributor contributor = configuration.assetFileFlatCrudMcpContributor(
            presentAssetFileFactory(assetFileToolCallbacksFactory), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId"));
    }

    @Test
    void contributesTheElevenFlatDataTableToolsWhenFactoryPresent() {
        DataTableToolCallbacksFactory dataTableToolCallbacksFactory = new DataTableToolCallbacksFactory(
            mock(WorkspaceDataTableFacade.class), mock(DataTableService.class), mock(DataTableRowService.class),
            null);

        McpServerToolCallbackContributor contributor = configuration.dataTableFlatCrudMcpContributor(
            presentDataTableFactory(dataTableToolCallbacksFactory), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder(
                "listDataTables", "queryDataTable", "aggregateDataTable", "addDataTableRow", "updateDataTableRow",
                "deleteDataTableRow", "addDataTableColumn", "createDataTable", "createDataTableFromCsv",
                "cloneDataTable", "dropDataTable");
    }

    @Test
    void dataTableContributorSkipsWhenFactoryAbsent() {
        McpServerToolCallbackContributor contributor = configuration.dataTableFlatCrudMcpContributor(
            absentDataTableFactory(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void everyDataTableToolAcceptsWorkspaceId() {
        DataTableToolCallbacksFactory dataTableToolCallbacksFactory = new DataTableToolCallbacksFactory(
            mock(WorkspaceDataTableFacade.class), mock(DataTableService.class), mock(DataTableRowService.class),
            null);

        McpServerToolCallbackContributor contributor = configuration.dataTableFlatCrudMcpContributor(
            presentDataTableFactory(dataTableToolCallbacksFactory), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId"));
    }

    @Test
    void contributesTheSevenFlatKnowledgeBaseToolsWhenFactoryPresent() {
        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory = new KnowledgeBaseToolCallbacksFactory(
            mock(WorkspaceKnowledgeBaseFacade.class), mock(KnowledgeBaseFacade.class),
            mock(KnowledgeBaseService.class), mock(KnowledgeBaseDocumentFacade.class),
            mock(KnowledgeBaseDocumentService.class), null);

        McpServerToolCallbackContributor contributor = configuration.knowledgeBaseFlatCrudMcpContributor(
            presentKnowledgeBaseFactory(knowledgeBaseToolCallbacksFactory), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder(
                "listKnowledgeBases", "queryKnowledgeBase", "createKnowledgeBase", "addKnowledgeBaseDocument",
                "deleteKnowledgeBaseDocument", "cloneKnowledgeBase", "deleteKnowledgeBase");
    }

    @Test
    void knowledgeBaseContributorSkipsWhenFactoryAbsent() {
        McpServerToolCallbackContributor contributor = configuration.knowledgeBaseFlatCrudMcpContributor(
            absentKnowledgeBaseFactory(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void everyKnowledgeBaseToolAcceptsWorkspaceId() {
        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory = new KnowledgeBaseToolCallbacksFactory(
            mock(WorkspaceKnowledgeBaseFacade.class), mock(KnowledgeBaseFacade.class),
            mock(KnowledgeBaseService.class), mock(KnowledgeBaseDocumentFacade.class),
            mock(KnowledgeBaseDocumentService.class), null);

        McpServerToolCallbackContributor contributor = configuration.knowledgeBaseFlatCrudMcpContributor(
            presentKnowledgeBaseFactory(knowledgeBaseToolCallbacksFactory), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId"));
    }

    @Test
    void contributesTheElevenFlatAiAgentToolsWhenFactoryPresent() {
        AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory =
            new AiAgentToolCallbacksFactory(mock(AiAgentFacade.class));

        McpServerToolCallbackContributor contributor = configuration.aiAgentFlatCrudMcpContributor(
            presentAiAgentFactory(aiAgentToolCallbacksFactory), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder(
                "listAiAgents", "getAiAgent", "createAiAgent", "updateAiAgent", "addAiAgentChannel",
                "deleteAiAgentChannel", "addAiAgentElement", "updateAiAgentElement", "deleteAiAgentElement",
                "updateAiAgentSettings", "publishAiAgent");
    }

    @Test
    void aiAgentContributorSkipsWhenFactoryAbsent() {
        McpServerToolCallbackContributor contributor = configuration.aiAgentFlatCrudMcpContributor(
            absentAiAgentFactory(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void everyAiAgentToolAcceptsWorkspaceId() {
        AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory =
            new AiAgentToolCallbacksFactory(mock(AiAgentFacade.class));

        McpServerToolCallbackContributor contributor = configuration.aiAgentFlatCrudMcpContributor(
            presentAiAgentFactory(aiAgentToolCallbacksFactory), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId"));
    }

    @Test
    void contributesTheSevenFlatDeploymentToolsWhenFactoryPresent() {
        DeploymentToolCallbacksFactory deploymentToolCallbacksFactory = new DeploymentToolCallbacksFactory(
            mock(ProjectDeploymentFacade.class));

        McpServerToolCallbackContributor contributor = configuration.deploymentFlatCrudMcpContributor(
            presentFactory(deploymentToolCallbacksFactory), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder(
                "listProjectDeployments", "createProjectDeployment", "updateProjectDeployment",
                "deleteProjectDeployment", "rollbackProjectDeployment", "toggleProjectDeployment", "promoteWorkflow");
    }

    @Test
    void deploymentContributorSkipsWhenFactoryAbsent() {
        McpServerToolCallbackContributor contributor = configuration.deploymentFlatCrudMcpContributor(
            absentFactory(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void onlyListProjectDeploymentsAcceptsWorkspaceId() {
        DeploymentToolCallbacksFactory deploymentToolCallbacksFactory = new DeploymentToolCallbacksFactory(
            mock(ProjectDeploymentFacade.class));

        McpServerToolCallbackContributor contributor = configuration.deploymentFlatCrudMcpContributor(
            presentFactory(deploymentToolCallbacksFactory), mock(WorkspaceService.class));

        List<ToolCallback> toolCallbacks = contributor.getToolCallbacks();

        ToolCallback listToolCallback = toolCallbacks.stream()
            .filter(toolCallback -> "listProjectDeployments".equals(
                toolCallback.getToolDefinition()
                    .name()))
            .findFirst()
            .orElseThrow();

        assertThat(listToolCallback.getToolDefinition()
            .inputSchema()).contains("workspaceId");

        List<ToolCallback> otherToolCallbacks = toolCallbacks.stream()
            .filter(toolCallback -> !"listProjectDeployments".equals(
                toolCallback.getToolDefinition()
                    .name()))
            .toList();

        assertThat(otherToolCallbacks)
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).doesNotContain("workspaceId"));
    }

    /**
     * Separate from {@link #present}/{@link #emptyProvider}: {@code deploymentFlatCrudMcpContributor} resolves its
     * factory via {@code getIfAvailable()} (mirroring {@code mcpServerCrudMcpContributor}), not the {@code ifAvailable}
     * consumer callback the other beans in this class use.
     */
    @SuppressWarnings("unchecked")
    private static ObjectProvider<DeploymentToolCallbacksFactory> presentFactory(
        DeploymentToolCallbacksFactory deploymentToolCallbacksFactory) {

        ObjectProvider<DeploymentToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(deploymentToolCallbacksFactory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DeploymentToolCallbacksFactory> absentFactory() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<AssetFileToolCallbacksFactory> presentAssetFileFactory(
        AssetFileToolCallbacksFactory assetFileToolCallbacksFactory) {

        ObjectProvider<AssetFileToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(assetFileToolCallbacksFactory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<AssetFileToolCallbacksFactory> absentAssetFileFactory() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DataTableToolCallbacksFactory> presentDataTableFactory(
        DataTableToolCallbacksFactory dataTableToolCallbacksFactory) {

        ObjectProvider<DataTableToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(dataTableToolCallbacksFactory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DataTableToolCallbacksFactory> absentDataTableFactory() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<KnowledgeBaseToolCallbacksFactory> presentKnowledgeBaseFactory(
        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory) {

        ObjectProvider<KnowledgeBaseToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(knowledgeBaseToolCallbacksFactory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<KnowledgeBaseToolCallbacksFactory> absentKnowledgeBaseFactory() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<AiAgentToolCallbacksFactory> presentAiAgentFactory(
        AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory) {

        ObjectProvider<AiAgentToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(aiAgentToolCallbacksFactory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<AiAgentToolCallbacksFactory> absentAiAgentFactory() {
        return mock(ObjectProvider.class);
    }

    private static IntelligentToolDefinition intelligentDefinition(String name) {
        ChatClient chatClient = mock(ChatClient.class);
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition())
            .thenReturn(ToolDefinition.builder()
                .name(name)
                .description(name)
                .inputSchema("{}")
                .build());

        return new FakeIntelligentToolDefinition(
            name, Map.of(IntelligentToolVariant.BUILD, (IntelligentToolChatClientFactory) chatModel -> chatClient),
            toolCallback);
    }

    private static IntelligentToolCatalog catalogOf(IntelligentToolDefinition... definitions) {
        IntelligentToolContributor contributor = () -> List.of(definitions);

        return new IntelligentToolCatalog(fixedObjectProvider(contributor));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<IntelligentToolContributor> fixedObjectProvider(
        IntelligentToolContributor contributor) {

        ObjectProvider<IntelligentToolContributor> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.orderedStream()).thenReturn(Stream.of(contributor));

        return objectProvider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }

    private static final class FakeIntelligentToolDefinition implements IntelligentToolDefinition {

        private final String name;
        private final Map<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoriesByVariant;
        private final ToolCallback toolCallback;

        private FakeIntelligentToolDefinition(
            String name, Map<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoriesByVariant,
            ToolCallback toolCallback) {

            this.name = name;
            this.chatClientFactoriesByVariant = chatClientFactoriesByVariant;
            this.toolCallback = toolCallback;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String agentTypeKey() {
            return name;
        }

        @Override
        public Set<IntelligentToolScope> panelScopes() {
            return Set.of();
        }

        @Override
        @Nullable
        public IntelligentToolChatClientFactory chatClientFactory(IntelligentToolVariant variant) {
            return chatClientFactoriesByVariant.get(variant);
        }

        @Override
        public ToolCallback create(IntelligentToolChatClientFactory chatClientFactory) {
            return toolCallback;
        }
    }
}
