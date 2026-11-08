/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.agui.core.exception.AGUIException;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.hub.agent.NonEmptyToolCallback;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGateToolCallback;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalDefaults;
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
            "askUserQuestion", "checkWorkflow", "explainCodeWorkflow", "explainCustomComponent", "getWorkflow",
            "listProjects", "listWorkflows", "openAiAgentTab", "openCodeWorkflowTab", "openCustomComponentTab",
            "openFileTab", "openWorkflowChatTab", "openWorkflowTab", "searchProjects", "searchWorkflows");
    }

    @Test
    void testAskCatalogHoldsTheDemotedReadTools() {
        AiHubModeToolCallbacks askToolCallbacks = AiHubAgentFixtures.withBuildWorkflow()
            .askToolCallbacks();

        assertThat(toolNames(askToolCallbacks.unpinned())).containsExactlyInAnyOrder(
            "getProject", "getProjectStatus", "listWorkflowExecutions", "getWorkflowExecution", "getAction",
            "getTrigger", "getClusterElement", "searchDocs", "listProjectDeployments", "listConnectionsForComponent",
            "lookupActionPropertyOptions", "lookupTriggerPropertyOptions", "lookupClusterElementPropertyOptions",
            "listAiAgents", "getAiAgent", "searchAiAgentTools", "searchAiAgentReferences", "listAssetFiles",
            "getAssetFileContent", "listDataTables", "queryDataTable", "aggregateDataTable",
            "listKnowledgeBases", "queryKnowledgeBase", "listContextSources", "searchContextStore",
            "getContextStoreRecord", "listAvailableSourceComponents", "describeSourceComponentEntities",
            "semanticSearchContextStore", "listApiCollections", "listComponentRules",
            "describeComponentToolParameters", "proposeComponentRuleCondition", "listAiHubChats");
    }

    @Test
    void testAskToolsAreReadOnly() {
        AiHubModeToolCallbacks askToolCallbacks = AiHubAgentFixtures.withBuildWorkflow()
            .askToolCallbacks();

        assertThat(toolNames(askToolCallbacks.toolCallbacks())).doesNotContain(
            "createProject", "deleteProject", "createProjectDeployment", "selectConnection", "buildWorkflow",
            "updateWorkflow", "listChatWorkflows", "createWorkflowChat", "runChatWorkflow", "listComponents",
            "getActionDefinition",
            "createAiAgent", "addAiAgentElement", "deleteAiAgentChannel", "importWorkflow", "buildCustomComponent",
            "buildCodeWorkflow", "createAssetFile", "updateAssetFileContent", "createDataTable", "addDataTableRow",
            "dropDataTable", "createKnowledgeBase", "deleteKnowledgeBase", "createApiCollection",
            "createComponentRule", "createContextStoreSource", "deleteContextStoreSource", "deleteContextStore",
            "promoteApiCollection", "promoteMcpServer", "promoteA2aServer", "promoteProjectDeployment",
            "data_analyst", "image_generator", "slide_builder", "generateImage", "createSlideDeck");
    }

    @Test
    void testAskRequestsOnlyTheReadOnlyDelegatesFromTheAskSubAgents() {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        fixtures.askToolCallbacks();

        verify(fixtures.intelligentToolCatalog).getByNames(
            eq(Set.of("explainCodeWorkflow", "explainCustomComponent")), eq(IntelligentToolVariant.ASK), any(),
            any());
        verify(fixtures.intelligentToolCatalog, never()).getByNames(
            anySet(), eq(IntelligentToolVariant.BUILD), any(), any());
    }

    @Test
    void testAskDelegatesAreWorkspaceScoped() throws AGUIException {
        AiHubSpringAIAgent askAgent = AiHubAgentFixtures.withBuildWorkflow()
            .askAgent();

        assertThat(askAgent.pinnedToolCallbacks())
            .filteredOn(
                toolCallback -> Set.of("explainCustomComponent", "explainCodeWorkflow")
                    .contains(toolCallback.getToolDefinition()
                        .name()))
            .hasSize(2)
            .allSatisfy(toolCallback -> assertThat(toolCallback).isInstanceOf(
                WorkspaceScopedSubAgentToolCallback.class));
    }

    @Test
    void testWritingSubAgentsAreBuildOnly() throws AGUIException {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        List<String> subAgentToolNames = List.of("data_analyst", "image_generator", "slide_builder");

        assertThat(toolNames(fixtures.askAgent())).doesNotContainAnyElementsOf(subAgentToolNames);
        assertThat(toolNames(fixtures.askToolCallbacks()
            .toolCallbacks())).doesNotContainAnyElementsOf(subAgentToolNames);
        assertThat(toolNames(fixtures.askGlobalToolCatalog()
            .toolCallbacks())).doesNotContainAnyElementsOf(subAgentToolNames);
        assertThat(AiHubConfiguration.ASK_PINNED_TOOL_NAMES).doesNotContainAnyElementsOf(subAgentToolNames);

        assertThat(toolNames(fixtures.buildAgent())).containsAll(subAgentToolNames);
        assertThat(toolNames(fixtures.buildGlobalToolCatalog()
            .toolCallbacks())).doesNotContainAnyElementsOf(subAgentToolNames);
    }

    @Test
    void testBuildPinsOnlyItsCoreTools() throws AGUIException {
        AiHubSpringAIAgent buildAgent = AiHubAgentFixtures.withBuildWorkflow()
            .buildAgent();

        assertThat(toolNames(buildAgent)).containsExactlyInAnyOrder(
            "askUserQuestion", "buildCodeWorkflow", "buildCustomComponent", "buildWorkflow", "checkWorkflow",
            "createConnection", "createProject", "createProjectWorkflow", "data_analyst", "getWorkflow",
            "image_generator", "importWorkflow",
            "listConnectionsForComponent", "listProjects", "listWorkflows", "openAiAgentTab", "openCodeWorkflowTab",
            "openCustomComponentTab", "openFileTab", "openWorkflowChatTab", "openWorkflowTab",
            "saveWorkflowTestConnection",
            "searchProjects", "searchWorkflows", "selectClusterElementPropertyOption", "selectConnection",
            "selectPropertyOption", "selectTriggerPropertyOption", "slide_builder");
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
            "listChatWorkflows", "createWorkflowChat", "runChatWorkflow", "listAiAgents", "getAiAgent",
            "searchAiAgentTools",
            "searchAiAgentReferences", "createAiAgent", "updateAiAgent", "updateAiAgentSettings",
            "addAiAgentElement", "updateAiAgentElement", "deleteAiAgentElement", "addAiAgentChannel",
            "deleteAiAgentChannel", "listAssetFiles", "getAssetFileContent", "createAssetFile", "createBinaryAssetFile",
            "createAssetFileFromUrl", "updateAssetFileContent", "cloneAssetFile", "listDataTables", "queryDataTable",
            "aggregateDataTable", "createDataTable", "createDataTableFromCsv", "cloneDataTable", "addDataTableColumn",
            "addDataTableRow", "updateDataTableRow", "deleteDataTableRow", "dropDataTable", "listKnowledgeBases",
            "queryKnowledgeBase", "createKnowledgeBase", "addKnowledgeBaseDocument", "deleteKnowledgeBaseDocument",
            "cloneKnowledgeBase", "deleteKnowledgeBase", "listContextSources", "searchContextStore",
            "getContextStoreRecord", "listAvailableSourceComponents", "describeSourceComponentEntities",
            "semanticSearchContextStore", "createContextStoreSource", "updateContextStoreSource",
            "deleteContextStoreSource", "refreshContextStoreSource", "setContextStoreSourceEnabled",
            "deleteContextStore", "listApiCollections", "createApiCollection", "listComponentRules",
            "describeComponentToolParameters", "proposeComponentRuleCondition", "createComponentRule",
            "promoteApiCollection", "promoteMcpServer", "promoteA2aServer", "promoteProjectDeployment",
            "listAiHubChats");
    }

    @Test
    void testBuildPromotionAndContextStoreDeleteToolsAreGatedByDefault() {
        AiHubModeToolCallbacks buildToolCallbacks = AiHubAgentFixtures.withBuildWorkflow()
            .buildToolCallbacks();

        assertThat(toolNames(buildToolCallbacks.unpinned()))
            .filteredOn(toolName -> toolName.startsWith("promote") || toolName.contains("ContextStore"))
            .filteredOn(AiHubToolApprovalDefaults::isGatedByDefault)
            .containsExactlyInAnyOrder(
                "promoteWorkflow", "promoteApiCollection", "promoteMcpServer", "promoteA2aServer",
                "promoteProjectDeployment", "deleteContextStoreSource", "deleteContextStore");
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
            .containsAll(withoutAskOnlyDelegates(toolNames(askToolCallbacks.toolCallbacks())));
        assertThat(toolNames(buildToolCallbacks.pinned()))
            .containsAll(withoutAskOnlyDelegates(toolNames(askToolCallbacks.pinned())));
    }

    @Test
    void testBuildReplacesEachAskOnlyDelegateWithItsBuildDelegate() {
        AiHubModeToolCallbacks buildToolCallbacks = AiHubAgentFixtures.withBuildWorkflow()
            .buildToolCallbacks();

        assertThat(toolNames(buildToolCallbacks.pinned()))
            .contains("buildCustomComponent", "buildCodeWorkflow")
            .doesNotContain("explainCustomComponent", "explainCodeWorkflow");
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

    private static List<String> withoutAskOnlyDelegates(List<String> toolNames) {
        return toolNames.stream()
            .filter(toolName -> !AiHubConfiguration.ASK_INTELLIGENT_TOOL_NAMES.contains(toolName))
            .toList();
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
