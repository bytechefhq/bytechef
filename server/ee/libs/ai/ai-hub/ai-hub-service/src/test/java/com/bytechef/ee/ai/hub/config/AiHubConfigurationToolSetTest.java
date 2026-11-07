/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agui.core.exception.AGUIException;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.hub.agent.NonEmptyToolCallback;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGateToolCallback;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubConfigurationToolSetTest {

    @Test
    void testAskPinsOnlyItsCoreTools() throws AGUIException {
        AiHubSpringAIAgent askAgent = AiHubAgentFixtures.withBuildWorkflow()
            .askAgent();

        assertThat(toolNames(askAgent)).containsExactlyInAnyOrder(
            "aggregateDataTable", "askUserQuestion", "checkWorkflow", "describeComponentToolParameters", "getAiAgent",
            "getAssetFileContent", "getWorkflow", "listAiAgents", "listApiCollections", "listAssetFiles",
            "listComponentRules", "listDataTables", "listKnowledgeBases", "listProjects", "listWorkflows",
            "openAiAgentTab", "openWorkflowChatTab", "openWorkflowTab", "proposeComponentRuleCondition",
            "queryDataTable", "queryKnowledgeBase", "searchAiAgentReferences", "searchAiAgentTools", "searchProjects",
            "searchWorkflows");
    }

    @Test
    void testAskCatalogHoldsTheDemotedReadTools() {
        AiHubModeToolCallbacks askToolCallbacks = AiHubAgentFixtures.withBuildWorkflow()
            .askToolCallbacks();

        assertThat(toolNames(askToolCallbacks.unpinned())).containsExactlyInAnyOrder(
            "getProject", "getProjectStatus", "listWorkflowExecutions", "getWorkflowExecution", "getAction",
            "getTrigger", "getClusterElement", "searchDocs", "listProjectDeployments", "listConnectionsForComponent",
            "lookupActionPropertyOptions", "lookupTriggerPropertyOptions", "lookupClusterElementPropertyOptions");
    }

    @Test
    void testAskToolsAreReadOnly() {
        AiHubModeToolCallbacks askToolCallbacks = AiHubAgentFixtures.withBuildWorkflow()
            .askToolCallbacks();

        assertThat(toolNames(askToolCallbacks.toolCallbacks())).doesNotContain(
            "createProject", "deleteProject", "createProjectDeployment", "selectConnection", "buildWorkflow",
            "updateWorkflow", "listChatWorkflows", "createWorkflowChat", "listComponents", "getActionDefinition",
            "createAiAgent", "addAiAgentElement", "deleteAiAgentChannel");
    }

    @Test
    void testBuildPinsOnlyItsCoreTools() throws AGUIException {
        AiHubSpringAIAgent buildAgent = AiHubAgentFixtures.withBuildWorkflow()
            .buildAgent();

        assertThat(toolNames(buildAgent)).containsExactlyInAnyOrder(
            "addAiAgentChannel", "addAiAgentElement", "addDataTableColumn", "addDataTableRow",
            "addKnowledgeBaseDocument", "aggregateDataTable", "askUserQuestion", "buildCodeWorkflow",
            "buildCustomComponent", "buildWorkflow", "checkWorkflow",
            "cloneAssetFile", "cloneDataTable", "cloneKnowledgeBase", "createAiAgent", "createApiCollection",
            "createAssetFile", "createAssetFileFromUrl", "createBinaryAssetFile", "createComponentRule",
            "createConnection", "createDataTable", "createDataTableFromCsv", "createKnowledgeBase", "createProject",
            "createProjectWorkflow", "describeComponentToolParameters", "getAiAgent", "getAssetFileContent",
            "getWorkflow", "importWorkflow", "listAiAgents", "listApiCollections", "listAssetFiles",
            "listComponentRules", "listConnectionsForComponent", "listDataTables", "listKnowledgeBases", "listProjects",
            "listWorkflows", "openAiAgentTab", "openCodeWorkflowTab", "openCustomComponentTab", "openWorkflowChatTab",
            "openWorkflowTab", "proposeComponentRuleCondition", "queryDataTable", "queryKnowledgeBase",
            "saveWorkflowTestConnection",
            "searchAiAgentReferences", "searchAiAgentTools", "searchProjects", "searchWorkflows",
            "selectClusterElementPropertyOption", "selectConnection", "selectPropertyOption",
            "selectTriggerPropertyOption", "updateAiAgent", "updateAiAgentElement", "updateAiAgentSettings",
            "updateAssetFileContent", "updateDataTableRow");
    }

    @Test
    void testBuildCatalogHoldsTheDemotedTools() {
        AiHubModeToolCallbacks buildToolCallbacks = AiHubAgentFixtures.withBuildWorkflow()
            .buildToolCallbacks();

        assertThat(toolNames(buildToolCallbacks.unpinned())).containsExactlyInAnyOrder(
            "getProject", "getProjectStatus", "updateProject", "publishProject", "deleteProject", "deleteWorkflow",
            "listWorkflowExecutions", "getWorkflowExecution", "getAction", "getTrigger", "getClusterElement",
            "searchDocs", "listProjectDeployments", "createProjectDeployment", "updateProjectDeployment",
            "deleteProjectDeployment", "rollbackProjectDeployment", "toggleProjectDeployment", "promoteWorkflow",
            "lookupActionPropertyOptions", "lookupTriggerPropertyOptions", "lookupClusterElementPropertyOptions",
            "listChatWorkflows", "createWorkflowChat", "deleteAiAgentChannel", "deleteAiAgentElement",
            "deleteDataTableRow", "deleteKnowledgeBase", "deleteKnowledgeBaseDocument", "dropDataTable");
    }

    @Test
    void testBuildToolsAuthorThroughBuildWorkflowOnly() {
        AiHubModeToolCallbacks buildToolCallbacks = AiHubAgentFixtures.withBuildWorkflow()
            .buildToolCallbacks();

        assertThat(toolNames(buildToolCallbacks.toolCallbacks())).doesNotContain("updateWorkflow");
    }

    @Test
    void testBuildToolsIncludeEveryAskTool() {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        AiHubModeToolCallbacks askToolCallbacks = fixtures.askToolCallbacks();
        AiHubModeToolCallbacks buildToolCallbacks = fixtures.buildToolCallbacks();

        assertThat(toolNames(buildToolCallbacks.toolCallbacks()))
            .containsAll(toolNames(askToolCallbacks.toolCallbacks()));
        assertThat(toolNames(buildToolCallbacks.pinned())).containsAll(toolNames(askToolCallbacks.pinned()));
    }

    @Test
    void testToolNamesAreUniquePerAgent() {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        AiHubModeToolCallbacks askToolCallbacks = fixtures.askToolCallbacks();
        AiHubModeToolCallbacks buildToolCallbacks = fixtures.buildToolCallbacks();

        assertThat(toolNames(askToolCallbacks.toolCallbacks())).doesNotHaveDuplicates();
        assertThat(toolNames(buildToolCallbacks.toolCallbacks())).doesNotHaveDuplicates();
    }

    @Test
    void testEveryPinnedToolNameIsAToolOfItsMode() {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        assertThat(toolNames(fixtures.askToolCallbacks()
            .toolCallbacks())).containsAll(AiHubConfiguration.ASK_PINNED_TOOL_NAMES);
        assertThat(toolNames(fixtures.buildToolCallbacks()
            .toolCallbacks())).containsAll(AiHubConfiguration.BUILD_PINNED_TOOL_NAMES);
    }

    @Test
    void testIntelligentToolsAreWorkspaceScoped() throws AGUIException {
        AiHubSpringAIAgent buildAgent = AiHubAgentFixtures.withBuildWorkflow()
            .buildAgent();

        assertThat(buildAgent.pinnedToolCallbacks())
            .filteredOn(
                toolCallback -> Set.of("buildWorkflow", "importWorkflow", "buildCustomComponent", "buildCodeWorkflow")
                    .contains(toolCallback.getToolDefinition()
                        .name()))
            .hasSize(4)
            .allSatisfy(toolCallback -> assertThat(toolCallback).isInstanceOf(
                WorkspaceScopedSubAgentToolCallback.class));
    }

    @Test
    void testEveryBuildToolPassesThroughTheApprovalWrapper() throws Exception {
        AiHubSpringAIAgent buildAgent = AiHubAgentFixtures.withBuildWorkflow()
            .buildAgent();

        List<ToolCallback> toolCallbacks = buildAgent.toolCallbacks();

        assertThat(toolCallbacks).hasSameSizeAs(buildAgent.pinnedToolCallbacks());

        for (ToolCallback toolCallback : toolCallbacks) {
            assertThat(toolCallback).isInstanceOf(RehydrateContextToolCallback.class);

            ToolCallback gated = delegateOf(toolCallback);

            assertThat(gated).isInstanceOf(AiHubApprovalGateToolCallback.class);
            assertThat(((AiHubApprovalGateToolCallback) gated).getDelegate()).isInstanceOf(NonEmptyToolCallback.class);
        }
    }

    @Test
    void testEveryAskToolPassesThroughTheApprovalWrapper() throws Exception {
        AiHubSpringAIAgent askAgent = AiHubAgentFixtures.withBuildWorkflow()
            .askAgent();

        for (ToolCallback toolCallback : askAgent.toolCallbacks()) {
            assertThat(toolCallback).isInstanceOf(RehydrateContextToolCallback.class);
            assertThat(delegateOf(toolCallback)).isInstanceOf(AiHubApprovalGateToolCallback.class);
        }
    }

    @Test
    void testStartupFailsWhenBuildWorkflowIsMissing() {
        AiHubAgentFixtures fixtures = new AiHubAgentFixtures(Set.of("importWorkflow"));

        assertThatThrownBy(fixtures::buildAgent)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("buildWorkflow")
            .hasMessageContaining("bytechef.ai.hub.enabled")
            .hasMessageContaining("bytechef.ai.copilot.enabled");
    }

    private static List<String> toolNames(AiHubSpringAIAgent agent) {
        return toolNames(agent.pinnedToolCallbacks());
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }

    private static ToolCallback delegateOf(ToolCallback rehydrateContextToolCallback) throws Exception {
        Field field = RehydrateContextToolCallback.class.getDeclaredField("delegate");

        field.setAccessible(true);

        return (ToolCallback) field.get(rehydrateContextToolCallback);
    }
}
