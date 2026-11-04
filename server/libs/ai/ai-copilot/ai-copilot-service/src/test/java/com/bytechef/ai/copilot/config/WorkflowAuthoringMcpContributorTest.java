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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.ScopedConnectionGuard;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.ConnectionTools;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.ai.tool.WorkflowInstructionTools;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
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
        "getWorkflowBuildInstructions", "listConnections", "lookupPropertyOptions");

    private static final List<String> LOOKUP_BEAN_NAMES = List.of(
        "actionDefinitionService", "actionDefinitionFacade", "triggerDefinitionService", "triggerDefinitionFacade",
        "propertyOptionsResolver", "scopedConnectionGuard", "workspaceConnectionFacade");

    private final ToolCallbackContributorConfiguration configuration = new ToolCallbackContributorConfiguration();

    @Test
    void testContributesExactlyTheReadOnlyAuthoringTools() {
        List<String> toolNames = getToolNames(getToolCallbacks());

        assertThat(toolNames)
            .hasSize(27)
            .doesNotHaveDuplicates()
            .containsExactlyInAnyOrderElementsOf(AUTHORING_TOOL_NAMES);
    }

    @Test
    void testLookupPropertyOptionsIsWorkspaceAndConnectionScoped() {
        ToolCallback lookupToolCallback = getToolCallback(getToolCallbacks(), "lookupPropertyOptions");

        assertThat(lookupToolCallback).isInstanceOf(WorkspaceScopedFlatToolCallback.class);
        assertThat(lookupToolCallback.getToolDefinition()
            .inputSchema()).contains("workspaceId", "environment", "connectionId");
    }

    @Test
    void testLookupPropertyOptionsRefusesAConnectionOutsideTheWorkspaceWithoutResolvingOptions() {
        Map<String, Object> beans = new HashMap<>(allBeans());

        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(42L, "Workspace 42")));

        WorkspaceConnectionFacade workspaceConnectionFacade = mock(WorkspaceConnectionFacade.class);

        when(workspaceConnectionFacade.getConnections(eq(42L), eq("slack"), isNull(), eq(0L), isNull()))
            .thenReturn(List.of());

        beans.put("workspaceConnectionFacade", workspaceConnectionFacade);

        ToolCallback lookupToolCallback = getToolCallback(
            getToolCallbacks(new StaticListableBeanFactory(beans), workspaceFacade), "lookupPropertyOptions");

        String result = lookupToolCallback.call(
            "{\"workspaceId\":42,\"kind\":\"ACTION\",\"componentName\":\"slack\",\"operationName\":"
                + "\"sendMessage\",\"propertyName\":\"channel\",\"connectionId\":99}");

        assertThat(result).contains("Connection 99", "listConnections");

        ActionDefinitionFacade actionDefinitionFacade = (ActionDefinitionFacade) beans.get("actionDefinitionFacade");
        ActionDefinitionService actionDefinitionService =
            (ActionDefinitionService) beans.get("actionDefinitionService");

        verifyNoInteractions(actionDefinitionFacade, actionDefinitionService);
    }

    @Test
    void testOmitsLookupPropertyOptionsWhenAnyOfItsDependenciesIsAbsent() {
        for (String beanName : LOOKUP_BEAN_NAMES) {
            Map<String, Object> beans = new HashMap<>(allBeans());

            beans.remove(beanName);

            assertThat(getToolNames(getToolCallbacks(new StaticListableBeanFactory(beans)))).as(beanName)
                .hasSize(26)
                .doesNotContain("lookupPropertyOptions");
        }
    }

    @Test
    void testDoesNotContributeWritesOrValidators() {
        assertThat(getToolNames(getToolCallbacks())).doesNotContain(
            "validateTask", "validateWorkflow", "updateScriptComponentCode", "updateClusterElementTask",
            "updateWorkflowRootProperties");
    }

    @Test
    void testListConnectionsIsWorkspaceScoped() {
        ToolCallback listConnectionsToolCallback = getToolCallback(getToolCallbacks(), "listConnections");

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
        return getToolCallbacks(new StaticListableBeanFactory(allBeans()));
    }

    private List<ToolCallback> getToolCallbacks(StaticListableBeanFactory beanFactory) {
        return getToolCallbacks(beanFactory, mock(WorkspaceFacade.class));
    }

    private List<ToolCallback> getToolCallbacks(
        StaticListableBeanFactory beanFactory, WorkspaceFacade workspaceFacade) {

        McpServerToolCallbackContributor contributor = configuration.workflowAuthoringMcpContributor(
            beanFactory.getBeanProvider(ComponentTools.class), beanFactory.getBeanProvider(TaskTools.class),
            beanFactory.getBeanProvider(WorkflowInstructionTools.class),
            beanFactory.getBeanProvider(ConnectionTools.class),
            beanFactory.getBeanProvider(ActionDefinitionService.class),
            beanFactory.getBeanProvider(ActionDefinitionFacade.class),
            beanFactory.getBeanProvider(TriggerDefinitionService.class),
            beanFactory.getBeanProvider(TriggerDefinitionFacade.class),
            beanFactory.getBeanProvider(PropertyOptionsResolver.class),
            beanFactory.getBeanProvider(ScopedConnectionGuard.class),
            beanFactory.getBeanProvider(WorkspaceConnectionFacade.class), workspaceFacade);

        return contributor.getToolCallbacks();
    }

    private static Map<String, Object> allBeans() {
        Map<String, Object> beans = new HashMap<>(
            Map.of(
                "componentTools", mock(ComponentTools.class), "taskTools", mock(TaskTools.class),
                "workflowInstructionTools", mock(WorkflowInstructionTools.class),
                "connectionTools", mock(ConnectionTools.class)));

        beans.putAll(lookupBeans());

        return beans;
    }

    private static Map<String, Object> lookupBeans() {
        return Map.of(
            "actionDefinitionService", mock(ActionDefinitionService.class),
            "actionDefinitionFacade", mock(ActionDefinitionFacade.class),
            "triggerDefinitionService", mock(TriggerDefinitionService.class),
            "triggerDefinitionFacade", mock(TriggerDefinitionFacade.class),
            "propertyOptionsResolver", mock(PropertyOptionsResolver.class),
            "scopedConnectionGuard", mock(ScopedConnectionGuard.class),
            "workspaceConnectionFacade", mock(WorkspaceConnectionFacade.class));
    }

    private static ToolCallback getToolCallback(List<ToolCallback> toolCallbacks, String name) {
        Stream<ToolCallback> stream = toolCallbacks.stream();

        return stream.filter(toolCallback -> name.equals(toolCallback.getToolDefinition()
            .name()))
            .findFirst()
            .orElseThrow();
    }

    private static List<String> getToolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }
}
