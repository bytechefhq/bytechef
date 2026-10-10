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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.McpServerToolCallbacksFactory;
import com.bytechef.automation.ai.tool.WorkspaceScopeResolver;
import com.bytechef.automation.ai.tool.knowledgebase.KnowledgeBaseToolCallbacksFactory;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.mcp.domain.McpServer;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class ToolCallbackContributorConfigurationTest {

    private final ToolCallbackContributorConfiguration configuration =
        new ToolCallbackContributorConfiguration();

    @Test
    void contributesAgentCallbacksWhenChatClientsPresent() {
        IntelligentToolCatalog intelligentToolCatalog = catalogOf(
            intelligentDefinition("buildWorkflow"), intelligentDefinition("writeScript"),
            intelligentDefinition("configureClusterElement"), intelligentDefinition("authorSkill"),
            intelligentDefinition("debugWorkflowExecution"), intelligentDefinition("importWorkflow"));

        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), intelligentToolCatalog, mock(WorkspaceScopeResolver.class));

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
            emptyProvider(), catalogOf(), mock(WorkspaceScopeResolver.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void contributedAgentToolsAcceptWorkspaceId() {
        IntelligentToolCatalog intelligentToolCatalog = catalogOf(
            intelligentDefinition("buildWorkflow"), intelligentDefinition("writeScript"),
            intelligentDefinition("configureClusterElement"), intelligentDefinition("authorSkill"),
            intelligentDefinition("debugWorkflowExecution"), intelligentDefinition("importWorkflow"));

        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), intelligentToolCatalog, mock(WorkspaceScopeResolver.class));

        assertThat(contributor.getToolCallbacks())
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId"));
    }

    @Test
    void contributesTheSevenFlatKnowledgeBaseToolsWhenFactoryPresent() {
        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory = new KnowledgeBaseToolCallbacksFactory(
            mock(WorkspaceKnowledgeBaseFacade.class), mock(KnowledgeBaseFacade.class),
            mock(KnowledgeBaseDocumentFacade.class),
            mock(KnowledgeBaseDocumentService.class));

        McpServerToolCallbackContributor contributor = configuration.knowledgeBaseFlatCrudMcpContributor(
            presentKnowledgeBaseFactory(knowledgeBaseToolCallbacksFactory), mock(WorkspaceScopeResolver.class));

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
            absentKnowledgeBaseFactory(), mock(WorkspaceScopeResolver.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void everyKnowledgeBaseToolAcceptsWorkspaceId() {
        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory = new KnowledgeBaseToolCallbacksFactory(
            mock(WorkspaceKnowledgeBaseFacade.class), mock(KnowledgeBaseFacade.class),
            mock(KnowledgeBaseDocumentFacade.class),
            mock(KnowledgeBaseDocumentService.class));

        McpServerToolCallbackContributor contributor = configuration.knowledgeBaseFlatCrudMcpContributor(
            presentKnowledgeBaseFactory(knowledgeBaseToolCallbacksFactory), mock(WorkspaceScopeResolver.class));

        assertThat(contributor.getToolCallbacks())
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId"));
    }

    @Test
    void contributesTheSevenFlatDeploymentToolsWhenFactoryPresent() {
        DeploymentToolCallbacksFactory deploymentToolCallbacksFactory = new DeploymentToolCallbacksFactory(
            mock(ProjectDeploymentFacade.class));

        McpServerToolCallbackContributor contributor = configuration.deploymentFlatCrudMcpContributor(
            presentFactory(deploymentToolCallbacksFactory), mock(WorkspaceScopeResolver.class));

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
            absentFactory(), mock(WorkspaceScopeResolver.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void onlyListProjectDeploymentsAcceptsWorkspaceId() {
        DeploymentToolCallbacksFactory deploymentToolCallbacksFactory = new DeploymentToolCallbacksFactory(
            mock(ProjectDeploymentFacade.class));

        McpServerToolCallbackContributor contributor = configuration.deploymentFlatCrudMcpContributor(
            presentFactory(deploymentToolCallbacksFactory), mock(WorkspaceScopeResolver.class));

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

    @Nested
    class McpServerCrudContributorTest {

        private final McpProjectService mcpProjectService = mock(McpProjectService.class);
        private final WorkspaceMcpServerFacade workspaceMcpServerFacade = mock(WorkspaceMcpServerFacade.class);
        private final WorkspaceScopeResolver workspaceScopeResolver = mock(WorkspaceScopeResolver.class);

        @Test
        void testListMcpProjectWorkflowsAcceptsWorkspaceId() {
            ToolCallback listMcpProjectWorkflowsToolCallback = getListMcpProjectWorkflowsToolCallback();

            assertThat(listMcpProjectWorkflowsToolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId");
        }

        @Test
        void testListMcpProjectWorkflowsRejectsAnMcpServerOutsideTheResolvedWorkspace() {
            McpServer workspaceMcpServer = mock(McpServer.class);

            when(workspaceMcpServer.getId()).thenReturn(6L);
            when(workspaceMcpServerFacade.getWorkspaceMcpServers(7L)).thenReturn(List.of(workspaceMcpServer));
            when(workspaceScopeResolver.resolve(7L, null)).thenReturn(new WorkspaceScopeResolver.Resolved(7L, 0L));

            String result = getListMcpProjectWorkflowsToolCallback().call("{\"mcpServerId\": 5, \"workspaceId\": 7}");

            assertThat(result).contains("MCP server 5 not found in the current workspace");
            verify(mcpProjectService, never()).getMcpServerMcpProjects(anyLong());
        }

        @Test
        void testListMcpProjectWorkflowsListsAnMcpServerOfTheResolvedWorkspace() {
            McpServer workspaceMcpServer = mock(McpServer.class);

            when(workspaceMcpServer.getId()).thenReturn(5L);
            when(workspaceMcpServerFacade.getWorkspaceMcpServers(7L)).thenReturn(List.of(workspaceMcpServer));
            when(workspaceScopeResolver.resolve(7L, null)).thenReturn(new WorkspaceScopeResolver.Resolved(7L, 0L));
            when(mcpProjectService.getMcpServerMcpProjects(5L)).thenReturn(List.of());

            String result = getListMcpProjectWorkflowsToolCallback().call("{\"mcpServerId\": 5, \"workspaceId\": 7}");

            assertThat(result).isEqualTo("[]");
            verify(mcpProjectService).getMcpServerMcpProjects(5L);
        }

        @SuppressWarnings("unchecked")
        private ToolCallback getListMcpProjectWorkflowsToolCallback() {
            McpServerToolCallbacksFactory mcpServerToolCallbacksFactory = new McpServerToolCallbacksFactory(
                mock(McpProjectFacade.class), mcpProjectService, mock(McpProjectWorkflowService.class),
                mock(ProjectDeploymentWorkflowService.class), mock(WorkflowService.class), workspaceMcpServerFacade);

            ObjectProvider<McpServerToolCallbacksFactory> provider = mock(ObjectProvider.class);

            when(provider.getIfAvailable()).thenReturn(mcpServerToolCallbacksFactory);

            McpServerToolCallbackContributor contributor = configuration.mcpServerCrudMcpContributor(
                provider, workspaceScopeResolver);

            return contributor.getToolCallbacks()
                .stream()
                .filter(toolCallback -> "listMcpProjectWorkflows".equals(
                    toolCallback.getToolDefinition()
                        .name()))
                .findFirst()
                .orElseThrow();
        }
    }

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
            name, Map.of(IntelligentToolVariant.BUILD, (IntelligentToolChatClientFactory) () -> chatClient),
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
