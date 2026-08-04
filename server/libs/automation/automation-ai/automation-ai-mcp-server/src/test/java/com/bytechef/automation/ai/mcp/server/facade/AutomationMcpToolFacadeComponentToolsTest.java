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

package com.bytechef.automation.ai.mcp.server.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins call-time enforcement of {@link McpTool#isEnabled()}: a client calling with a stale tool list must not be able
 * to execute a tool that has been disabled (or deleted) since listing.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SuppressWarnings("unchecked")
class AutomationMcpToolFacadeComponentToolsTest {

    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade =
        mock(ClusterElementDefinitionFacade.class);
    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final McpComponentService mcpComponentService = mock(McpComponentService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final McpToolService mcpToolService = mock(McpToolService.class);
    private final ToolExecutionRecorder toolExecutionRecorder = mock(ToolExecutionRecorder.class);
    private final WorkspaceMcpServerService workspaceMcpServerService = mock(WorkspaceMcpServerService.class);

    private final AutomationMcpToolFacade facade = new AutomationMcpToolFacade(
        (ObjectProvider<ApprovalTokens>) mock(ObjectProvider.class), clusterElementDefinitionFacade,
        clusterElementDefinitionService, mock(Evaluator.class), mock(JobCompletionAwaiter.class),
        mock(JobResumeFacade.class), mock(JobService.class), mcpComponentService, mock(McpProjectService.class),
        mock(McpProjectWorkflowService.class), mcpServerService, mcpToolService,
        (ObjectProvider<PlanLimitsProvider>) mock(ObjectProvider.class), mock(PrincipalJobFacade.class),
        mock(ProjectDeploymentWorkflowService.class), "https://example.com", mock(TaskExecutionService.class),
        mock(TaskFileStorage.class), toolExecutionRecorder, mock(WorkflowService.class), workspaceMcpServerService);

    @Test
    void testCallOfToolDisabledAfterListingIsRejected() {
        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback = givenListedTool();

        givenEnabledMcpServer();

        when(mcpToolService.fetchMcpTool(3L)).thenReturn(Optional.of(mcpTool(false)));

        assertThatThrownBy(() -> functionToolCallback.call("{}"))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessageContaining("disabled")
            .hasCauseInstanceOf(ConfigurationException.class);

        verify(clusterElementDefinitionFacade, never())
            .executeTool(anyString(), anyInt(), anyString(), any(), any());
    }

    @Test
    void testCallOfToolDeletedAfterListingIsRejected() {
        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback = givenListedTool();

        givenEnabledMcpServer();

        when(mcpToolService.fetchMcpTool(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> functionToolCallback.call("{}"))
            .isInstanceOf(ToolExecutionException.class)
            .hasCauseInstanceOf(ConfigurationException.class);

        verify(clusterElementDefinitionFacade, never())
            .executeTool(anyString(), anyInt(), anyString(), any(), any());
    }

    @Test
    void testCallOfEnabledToolExecutes() {
        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback = givenListedTool();

        givenEnabledMcpServer();

        when(mcpToolService.fetchMcpTool(3L)).thenReturn(Optional.of(mcpTool(true)));
        when(toolExecutionRecorder.record(any(), any(Supplier.class)))
            .thenAnswer(invocation -> {
                Supplier<Object> execution = invocation.getArgument(1);

                return execution.get();
            });
        when(clusterElementDefinitionFacade.executeTool(eq("slack"), eq(1), eq("sendMessage"), any(), isNull()))
            .thenReturn(Map.of("ok", true));

        functionToolCallback.call("{}");

        verify(clusterElementDefinitionFacade).executeTool(eq("slack"), eq(1), eq("sendMessage"), any(), isNull());
    }

    private FunctionToolCallback<Map<String, Object>, Object> givenListedTool() {
        McpTool mcpTool = mcpTool(true);

        McpComponent mcpComponent = mock(McpComponent.class);

        when(mcpComponent.getComponentName()).thenReturn("slack");
        when(mcpComponent.getComponentVersion()).thenReturn(1);
        when(mcpComponent.getMcpServerId()).thenReturn(30L);
        when(mcpComponent.getConnectionId()).thenReturn(null);

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getComponentName()).thenReturn("slack");
        when(clusterElementDefinition.getComponentVersion()).thenReturn(1);
        when(clusterElementDefinition.getName()).thenReturn("sendMessage");
        when(clusterElementDefinition.getDescription()).thenReturn("Send a message");

        when(mcpComponentService.getMcpComponent(2L)).thenReturn(mcpComponent);
        when(clusterElementDefinitionService.getClusterElementDefinition("slack", 1, "sendMessage"))
            .thenReturn(clusterElementDefinition);
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(30L)).thenReturn(Optional.empty());

        return facade.getFunctionToolCallback(mcpTool);
    }

    private void givenEnabledMcpServer() {
        when(mcpServerService.getMcpServer(30L)).thenReturn(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true));
    }

    private static McpTool mcpTool(boolean enabled) {
        McpTool mcpTool = new McpTool("sendMessage", Map.of(), 2L);

        mcpTool.setId(3L);
        mcpTool.setEnabled(enabled);

        return mcpTool;
    }
}
