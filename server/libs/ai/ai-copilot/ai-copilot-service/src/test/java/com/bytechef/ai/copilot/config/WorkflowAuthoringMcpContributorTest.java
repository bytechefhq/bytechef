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

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.ConnectionTools;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.ai.tool.WorkflowInstructionTools;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowAuthoringMcpContributorTest {

    private static final List<String> AUTHORING_TOOL_NAMES = List.of(
        "getComponent", "getTrigger", "getTriggerDefinition", "listActions", "listComponents", "listTriggers",
        "searchComponents", "searchTriggers", "getAction", "searchClusterElements", "getClusterElement",
        "searchActions", "getActionDefinition", "getOutputProperty", "getProperties", "getTask",
        "getTaskProperties", "getTaskOutputProperty", "getTaskDefinition", "getTaskDispatcherBuildInstructions",
        "listTasks", "searchTasks", "getClusterElementsInstructions", "getScriptCodeInstructions",
        "getWorkflowBuildInstructions", "listConnections");

    private final ToolCallbackContributorConfiguration configuration = new ToolCallbackContributorConfiguration();

    @Test
    void testContributesExactlyTheReadOnlyAuthoringTools() {
        List<String> toolNames = getToolNames(getToolCallbacks());

        assertThat(toolNames)
            .hasSize(26)
            .doesNotHaveDuplicates()
            .containsExactlyInAnyOrderElementsOf(AUTHORING_TOOL_NAMES);
    }

    @Test
    void testDoesNotContributeWritesOrValidators() {
        assertThat(getToolNames(getToolCallbacks())).doesNotContain(
            "validateTask", "validateWorkflow", "updateScriptComponentCode", "updateClusterElementTask",
            "updateWorkflowRootProperties", "lookupPropertyOptions");
    }

    @Test
    void testListConnectionsIsWorkspaceScoped() {
        ToolCallback listConnectionsToolCallback = getToolCallbacks().stream()
            .filter(toolCallback -> "listConnections".equals(
                toolCallback.getToolDefinition()
                    .name()))
            .findFirst()
            .orElseThrow();

        assertThat(listConnectionsToolCallback).isInstanceOf(WorkspaceScopedFlatToolCallback.class);
        assertThat(listConnectionsToolCallback.getToolDefinition()
            .inputSchema()).contains("workspaceId");
    }

    @Test
    void testOmitsListConnectionsWhenConnectionToolsAreAbsent() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(
            Map.of(
                "componentTools", mock(ComponentTools.class), "taskTools", mock(TaskTools.class),
                "workflowInstructionTools", mock(WorkflowInstructionTools.class)));

        List<String> toolNames = getToolNames(getToolCallbacks(beanFactory));

        assertThat(toolNames)
            .hasSize(25)
            .doesNotContain("listConnections");
    }

    @Test
    void testContributesOnlyListConnectionsWhenAnAuthoringToolBeanIsAbsent() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(
            Map.of(
                "componentTools", mock(ComponentTools.class), "taskTools", mock(TaskTools.class),
                "connectionTools", mock(ConnectionTools.class)));

        assertThat(getToolNames(getToolCallbacks(beanFactory))).containsExactly("listConnections");
    }

    @Test
    void testContributesNothingWithoutAnyToolBeans() {
        assertThat(getToolCallbacks(new StaticListableBeanFactory())).isEmpty();
    }

    private List<ToolCallback> getToolCallbacks() {
        return getToolCallbacks(
            new StaticListableBeanFactory(
                Map.of(
                    "componentTools", mock(ComponentTools.class), "taskTools", mock(TaskTools.class),
                    "workflowInstructionTools", mock(WorkflowInstructionTools.class),
                    "connectionTools", mock(ConnectionTools.class))));
    }

    private List<ToolCallback> getToolCallbacks(StaticListableBeanFactory beanFactory) {
        McpServerToolCallbackContributor contributor = configuration.workflowAuthoringMcpContributor(
            beanFactory.getBeanProvider(ComponentTools.class), beanFactory.getBeanProvider(TaskTools.class),
            beanFactory.getBeanProvider(WorkflowInstructionTools.class),
            beanFactory.getBeanProvider(ConnectionTools.class), mock(WorkspaceFacade.class));

        return contributor.getToolCallbacks();
    }

    private static List<String> getToolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }
}
