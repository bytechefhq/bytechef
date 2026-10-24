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
 * Pins the parity contract of the AI Hub tool sets (spec D3): ASK is read-only, BUILD authors workflow content only
 * through {@code buildWorkflow} / {@code importWorkflow}, and every tool passes through the one approval wrap site.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubConfigurationToolSetTest {

    @Test
    void testAskToolsAreReadOnly() throws AGUIException {
        AiHubSpringAIAgent askAgent = AiHubAgentFixtures.withBuildWorkflow()
            .askAgent();

        assertThat(toolNames(askAgent))
            .contains(
                "listProjects", "getWorkflow", "listProjectDeployments", "listConnectionsForComponent",
                "askUserQuestion", "openWorkflowTab", "openWorkflowChatTab", "listWorkflowExecutions",
                "getWorkflowExecution", "getAction", "getTrigger", "getClusterElement")
            .doesNotContain(
                "createProject", "deleteProject", "createProjectDeployment", "selectConnection", "buildWorkflow",
                "updateWorkflow", "listChatWorkflows", "createWorkflowChat", "listComponents", "getActionDefinition");
    }

    @Test
    void testBuildToolsAuthorThroughBuildWorkflowOnly() throws AGUIException {
        AiHubSpringAIAgent buildAgent = AiHubAgentFixtures.withBuildWorkflow()
            .buildAgent();

        assertThat(toolNames(buildAgent))
            .contains(
                "createProject", "createProjectWorkflow", "deleteWorkflow", "createProjectDeployment",
                "promoteWorkflow", "selectConnection", "createConnection", "buildWorkflow", "importWorkflow",
                "openWorkflowTab", "openWorkflowChatTab", "listChatWorkflows", "createWorkflowChat")
            .doesNotContain("updateWorkflow");
    }

    @Test
    void testBuildToolsIncludeEveryAskTool() throws AGUIException {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        assertThat(toolNames(fixtures.buildAgent())).containsAll(toolNames(fixtures.askAgent()));
    }

    @Test
    void testToolNamesAreUniquePerAgent() throws AGUIException {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        assertThat(toolNames(fixtures.askAgent())).doesNotHaveDuplicates();
        assertThat(toolNames(fixtures.buildAgent())).doesNotHaveDuplicates();
    }

    @Test
    void testIntelligentToolsAreWorkspaceScoped() throws AGUIException {
        AiHubSpringAIAgent buildAgent = AiHubAgentFixtures.withBuildWorkflow()
            .buildAgent();

        assertThat(buildAgent.pinnedToolCallbacks())
            .filteredOn(toolCallback -> Set.of("buildWorkflow", "importWorkflow")
                .contains(toolCallback.getToolDefinition()
                    .name()))
            .hasSize(2)
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
        return agent.pinnedToolCallbacks()
            .stream()
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
