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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.mcp.domain.McpServer;
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
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the tools/list resilience contract for workflow-backed tools: a workflow whose tool mapping has not been
 * completed yet (no {@code toolName} in its parameters) is skipped instead of breaking the whole tool listing.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SuppressWarnings("unchecked")
class AutomationMcpToolFacadeWorkflowToolsTest {

    private static final String NEW_WORKFLOW_CALL_TYPE = "workflow/v1/newWorkflowCall";

    private final McpProjectWorkflowService mcpProjectWorkflowService = mock(McpProjectWorkflowService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final PrincipalJobFacade principalJobFacade = mock(PrincipalJobFacade.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ToolExecutionRecorder toolExecutionRecorder = mock(ToolExecutionRecorder.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final AutomationMcpToolFacade facade = new AutomationMcpToolFacade(
        (ObjectProvider<ApprovalTokens>) mock(ObjectProvider.class), mock(ClusterElementDefinitionFacade.class),
        mock(ClusterElementDefinitionService.class), mock(Evaluator.class), mock(JobCompletionAwaiter.class),
        mock(JobResumeFacade.class), mock(JobService.class), mock(McpComponentService.class),
        mock(McpProjectService.class), mcpProjectWorkflowService, mcpServerService,
        mock(McpToolService.class),
        (ObjectProvider<PlanLimitsProvider>) mock(ObjectProvider.class), principalJobFacade,
        projectDeploymentWorkflowService, "https://example.com", mock(TaskExecutionService.class),
        mock(TaskFileStorage.class), toolExecutionRecorder, workflowService,
        mock(WorkspaceMcpServerService.class));

    @Test
    void testWorkflowWithoutToolNameMappingIsSkipped() {
        McpProject mcpProject = mcpProject();

        McpProjectWorkflow unmappedMcpProjectWorkflow = mcpProjectWorkflow(20L, Map.of());

        stubCallableWorkflow(20L, "wf1");

        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(10L))
            .thenReturn(List.of(unmappedMcpProjectWorkflow));

        try (MockedStatic<WorkflowTrigger> workflowTriggerMockedStatic = mockStatic(WorkflowTrigger.class);
            MockedStatic<WorkflowNodeType> workflowNodeTypeMockedStatic = mockStatic(WorkflowNodeType.class)) {

            stubNewWorkflowCallTrigger(workflowTriggerMockedStatic, workflowNodeTypeMockedStatic);

            List<ToolCallback> toolCallbacks = facade.getFunctionToolCallbacks(mcpProject);

            assertThat(toolCallbacks).isEmpty();
        }
    }

    @Test
    void testUnmappedWorkflowDoesNotHideMappedWorkflow() {
        McpProject mcpProject = mcpProject();

        McpProjectWorkflow unmappedMcpProjectWorkflow = mcpProjectWorkflow(20L, Map.of());
        McpProjectWorkflow mappedMcpProjectWorkflow = mcpProjectWorkflow(
            21L, Map.of("toolName", "mappedTool", "toolDescription", "A mapped tool"));

        stubCallableWorkflow(20L, "wf1");
        stubCallableWorkflow(21L, "wf2");

        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(10L))
            .thenReturn(List.of(unmappedMcpProjectWorkflow, mappedMcpProjectWorkflow));

        try (MockedStatic<WorkflowTrigger> workflowTriggerMockedStatic = mockStatic(WorkflowTrigger.class);
            MockedStatic<WorkflowNodeType> workflowNodeTypeMockedStatic = mockStatic(WorkflowNodeType.class)) {

            stubNewWorkflowCallTrigger(workflowTriggerMockedStatic, workflowNodeTypeMockedStatic);

            List<ToolCallback> toolCallbacks = facade.getFunctionToolCallbacks(mcpProject);

            assertThat(toolCallbacks).hasSize(1);
            assertThat(toolCallbacks.getFirst()
                .getToolDefinition()
                .name()).isEqualTo("mappedTool");
        }
    }

    @Test
    void testCallOfWorkflowToolDisabledAfterListingIsRejected() {
        McpProject mcpProject = mcpProject();

        McpProjectWorkflow mappedMcpProjectWorkflow = mcpProjectWorkflow(
            21L, Map.of("toolName", "mappedTool", "toolDescription", "A mapped tool"));

        stubCallableWorkflow(21L, "wf2");

        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(10L))
            .thenReturn(List.of(mappedMcpProjectWorkflow));
        when(mcpServerService.getMcpServer(30L)).thenReturn(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true));

        try (MockedStatic<WorkflowTrigger> workflowTriggerMockedStatic = mockStatic(WorkflowTrigger.class);
            MockedStatic<WorkflowNodeType> workflowNodeTypeMockedStatic = mockStatic(WorkflowNodeType.class)) {

            stubNewWorkflowCallTrigger(workflowTriggerMockedStatic, workflowNodeTypeMockedStatic);

            List<ToolCallback> toolCallbacks = facade.getFunctionToolCallbacks(mcpProject);

            ProjectDeploymentWorkflow disabledProjectDeploymentWorkflow = mock(ProjectDeploymentWorkflow.class);

            when(disabledProjectDeploymentWorkflow.isEnabled()).thenReturn(false);
            when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(21L))
                .thenReturn(disabledProjectDeploymentWorkflow);

            ToolCallback toolCallback = toolCallbacks.getFirst();

            assertThatThrownBy(() -> toolCallback.call("{}"))
                .isInstanceOf(ToolExecutionException.class)
                .hasMessageContaining("disabled")
                .hasCauseInstanceOf(ConfigurationException.class);

            verify(principalJobFacade, never()).createJob(any(), anyLong(), any());
        }
    }

    @Test
    void testCallOfEnabledWorkflowToolCreatesJob() {
        McpProject mcpProject = mcpProject();

        McpProjectWorkflow mappedMcpProjectWorkflow = mcpProjectWorkflow(
            21L, Map.of("toolName", "mappedTool", "toolDescription", "A mapped tool"));

        stubCallableWorkflow(21L, "wf2");

        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(10L))
            .thenReturn(List.of(mappedMcpProjectWorkflow));
        when(mcpServerService.getMcpServer(30L)).thenReturn(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true));
        when(principalJobFacade.createJob(any(), anyLong(), any())).thenReturn(5L);
        when(toolExecutionRecorder.record(any(), any(Supplier.class))).thenReturn(Map.of("ok", true));

        try (MockedStatic<WorkflowTrigger> workflowTriggerMockedStatic = mockStatic(WorkflowTrigger.class);
            MockedStatic<WorkflowNodeType> workflowNodeTypeMockedStatic = mockStatic(WorkflowNodeType.class)) {

            stubNewWorkflowCallTrigger(workflowTriggerMockedStatic, workflowNodeTypeMockedStatic);

            List<ToolCallback> toolCallbacks = facade.getFunctionToolCallbacks(mcpProject);

            ToolCallback toolCallback = toolCallbacks.getFirst();

            toolCallback.call("{}");

            verify(principalJobFacade).createJob(any(), anyLong(), any());
        }
    }

    private static McpProject mcpProject() {
        McpProject mcpProject = mock(McpProject.class);

        when(mcpProject.getId()).thenReturn(10L);
        when(mcpProject.getMcpServerId()).thenReturn(30L);

        return mcpProject;
    }

    private static McpProjectWorkflow mcpProjectWorkflow(
        long projectDeploymentWorkflowId, Map<String, Object> parameters) {

        McpProjectWorkflow mcpProjectWorkflow = mock(McpProjectWorkflow.class);

        when(mcpProjectWorkflow.getProjectDeploymentWorkflowId()).thenReturn(projectDeploymentWorkflowId);
        doReturn(parameters).when(mcpProjectWorkflow)
            .getParameters();

        return mcpProjectWorkflow;
    }

    private void stubCallableWorkflow(long projectDeploymentWorkflowId, String workflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = mock(ProjectDeploymentWorkflow.class);

        when(projectDeploymentWorkflow.getId()).thenReturn(projectDeploymentWorkflowId);
        when(projectDeploymentWorkflow.isEnabled()).thenReturn(true);
        when(projectDeploymentWorkflow.getWorkflowId()).thenReturn(workflowId);
        when(projectDeploymentWorkflow.getProjectDeploymentId()).thenReturn(40L);
        when(projectDeploymentWorkflow.getInputs()).thenReturn(Map.of());

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentWorkflowId))
            .thenReturn(projectDeploymentWorkflow);
        when(workflowService.getWorkflow(workflowId)).thenReturn(mock(Workflow.class));
    }

    private static void stubNewWorkflowCallTrigger(
        MockedStatic<WorkflowTrigger> workflowTriggerMockedStatic,
        MockedStatic<WorkflowNodeType> workflowNodeTypeMockedStatic) {

        WorkflowTrigger workflowTrigger = mock(WorkflowTrigger.class);
        WorkflowNodeType workflowNodeType = mock(WorkflowNodeType.class);

        when(workflowTrigger.getType()).thenReturn(NEW_WORKFLOW_CALL_TYPE);
        when(workflowTrigger.getName()).thenReturn("newWorkflowCall_1");
        when(workflowNodeType.name()).thenReturn(WorkflowConstants.WORKFLOW);
        when(workflowNodeType.operation()).thenReturn(WorkflowConstants.NEW_WORKFLOW_CALL);

        workflowTriggerMockedStatic.when(() -> WorkflowTrigger.of(any(Workflow.class)))
            .thenReturn(List.of(workflowTrigger));
        workflowNodeTypeMockedStatic.when(() -> WorkflowNodeType.ofType(NEW_WORKFLOW_CALL_TYPE))
            .thenReturn(workflowNodeType);
    }
}
