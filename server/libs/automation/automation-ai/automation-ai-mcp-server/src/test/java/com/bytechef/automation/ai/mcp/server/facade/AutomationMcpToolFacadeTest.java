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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ExecutionException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.JobInputConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SuppressWarnings("unchecked")
class AutomationMcpToolFacadeTest {

    private static final String NEW_WORKFLOW_CALL_TYPE = "workflow/v1/newWorkflowCall";

    private final ObjectProvider<ApprovalTokens> approvalTokensObjectProvider =
        (ObjectProvider<ApprovalTokens>) mock(ObjectProvider.class);
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade =
        mock(ClusterElementDefinitionFacade.class);
    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final JobCompletionAwaiter jobCompletionAwaiter = mock(JobCompletionAwaiter.class);
    private final JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);
    private final JobService jobService = mock(JobService.class);
    private final McpComponentService mcpComponentService = mock(McpComponentService.class);
    private final McpProjectService mcpProjectService = mock(McpProjectService.class);
    private final McpProjectWorkflowService mcpProjectWorkflowService = mock(McpProjectWorkflowService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final McpToolService mcpToolService = mock(McpToolService.class);
    private final PrincipalJobFacade principalJobFacade = mock(PrincipalJobFacade.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TaskFileStorage taskFileStorage = mock(TaskFileStorage.class);
    private final ToolExecutionRecorder toolExecutionRecorder = mock(ToolExecutionRecorder.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final WorkspaceMcpServerService workspaceMcpServerService = mock(WorkspaceMcpServerService.class);

    private final AutomationMcpToolFacade facade = new AutomationMcpToolFacade(
        approvalTokensObjectProvider, clusterElementDefinitionFacade, clusterElementDefinitionService,
        mock(Evaluator.class), jobCompletionAwaiter, jobResumeFacade, jobService, mcpComponentService,
        mcpProjectService, mcpProjectWorkflowService, mcpServerService, mcpToolService, principalJobFacade,
        projectDeploymentWorkflowService, "https://example.com", taskExecutionService, taskFileStorage,
        toolExecutionRecorder, workflowService, workspaceMcpServerService);

    @Test
    void testAwaitApprovedWorkflowRunReturnsOutputsWhenRunCompletes() {
        FileEntry outputs = mock(FileEntry.class);
        Job job = mock(Job.class);

        when(job.getId()).thenReturn(1L);
        when(job.getStatus()).thenReturn(Job.Status.COMPLETED);
        when(job.getOutputs()).thenReturn(outputs);
        when(jobService.getJob(1L)).thenReturn(job);
        when(jobCompletionAwaiter.await(anyLong(), any())).thenReturn(CompletableFuture.completedFuture(job));
        Map<String, ?> jobOutputs = Map.of("result", "ok");

        when(taskExecutionService.fetchLastJobTaskExecution(1L)).thenReturn(Optional.empty());
        doReturn(jobOutputs).when(taskFileStorage)
            .readJobOutputs(outputs);

        Object result = facade.awaitApprovedWorkflowRun(1L);

        assertThat(result).isEqualTo(jobOutputs);
    }

    @Test
    void testAwaitApprovedWorkflowRunReturnsDescriptorWhenPollingIsInterruptedWhileStopped() {
        Job job = mock(Job.class);

        when(job.getId()).thenReturn(2L);
        when(job.getStatus()).thenReturn(Job.Status.STOPPED);
        when(job.getMetadata(MetadataConstants.JOB_RESUME_ID)).thenReturn("resume-2");
        when(jobService.getJob(2L)).thenReturn(job);

        Thread currentThread = Thread.currentThread();

        currentThread.interrupt();

        Object result;

        try {
            result = facade.awaitApprovedWorkflowRun(2L);
        } finally {
            Thread.interrupted();
        }

        assertThat(result).isInstanceOf(Map.class);
        assertThat((Map<String, Object>) result)
            .containsEntry("status", "approval_required")
            .containsEntry("jobId", 2L);
        verify(jobCompletionAwaiter, never()).await(anyLong(), any());
    }

    @Test
    void testAwaitApprovedWorkflowRunReturnsDescriptorWhenAwaiterHandsBackRepausedRun() {
        Job job = mock(Job.class);

        when(job.getId()).thenReturn(3L);
        when(job.getStatus()).thenReturn(Job.Status.STARTED, Job.Status.STARTED, Job.Status.STOPPED);
        when(job.getMetadata(MetadataConstants.JOB_RESUME_ID)).thenReturn("resume-3");
        when(jobService.getJob(3L)).thenReturn(job);
        when(jobCompletionAwaiter.await(anyLong(), any())).thenReturn(CompletableFuture.completedFuture(job));

        Object result = facade.awaitApprovedWorkflowRun(3L);

        assertThat((Map<String, Object>) result).containsEntry("status", "approval_required");
    }

    @Test
    void testAwaitApprovedWorkflowRunThrowsWhenRunWasStoppedWithoutPendingApproval() {
        Job job = mock(Job.class);

        when(job.getId()).thenReturn(8L);
        when(job.getStatus()).thenReturn(Job.Status.STOPPED);
        when(jobService.getJob(8L)).thenReturn(job);
        when(jobCompletionAwaiter.await(anyLong(), any())).thenReturn(CompletableFuture.completedFuture(job));
        when(taskExecutionService.fetchLastJobTaskExecution(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.awaitApprovedWorkflowRun(8L))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("was stopped");
    }

    @Test
    void testResolveApprovalAndAwaitReturnsUnavailableWhenResumeIsGone() {
        when(jobResumeFacade.resumeJob("token", Map.of())).thenReturn(JobResumeOutcome.GONE);

        Object result = facade.resolveApprovalAndAwait("token", Map.of(), 4L);

        assertThat((Map<String, Object>) result)
            .containsEntry("status", "approval_unavailable");
        assertThat((String) ((Map<String, Object>) result).get("message")).contains("GONE");
        verify(jobService, never()).getJob(anyLong());
    }

    @Test
    void testResolveApprovalAndAwaitReturnsUnavailableWhenResumeIdIsInvalid() {
        when(jobResumeFacade.resumeJob("token", Map.of())).thenReturn(JobResumeOutcome.INVALID_ID);

        Object result = facade.resolveApprovalAndAwait("token", Map.of(), 5L);

        assertThat((Map<String, Object>) result).containsEntry("status", "approval_unavailable");
        assertThat((String) ((Map<String, Object>) result).get("message")).contains("INVALID_ID");
    }

    @Test
    void testResolveApprovalAndAwaitAwaitsRunWhenResumeSucceeds() {
        FileEntry outputs = mock(FileEntry.class);
        Job job = mock(Job.class);

        when(job.getId()).thenReturn(6L);
        when(job.getStatus()).thenReturn(Job.Status.COMPLETED);
        when(job.getOutputs()).thenReturn(outputs);
        when(jobResumeFacade.resumeJob("token", Map.of())).thenReturn(JobResumeOutcome.OK);
        Map<String, ?> jobOutputs = Map.of("done", true);

        when(jobService.getJob(6L)).thenReturn(job);
        when(jobCompletionAwaiter.await(anyLong(), any())).thenReturn(CompletableFuture.completedFuture(job));
        when(taskExecutionService.fetchLastJobTaskExecution(6L)).thenReturn(Optional.empty());
        doReturn(jobOutputs).when(taskFileStorage)
            .readJobOutputs(outputs);

        Object result = facade.resolveApprovalAndAwait("token", Map.of(), 6L);

        assertThat(result).isEqualTo(jobOutputs);
    }

    @Test
    void testJobWorkflowExposureReadsMcpServerProjectsWithoutWorkspacePermissionChecks() {
        Job job = mock(Job.class);
        McpProject mcpProject = mock(McpProject.class);
        McpProjectWorkflow mcpProjectWorkflow = mock(McpProjectWorkflow.class);
        ProjectDeploymentWorkflow projectDeploymentWorkflow = mock(ProjectDeploymentWorkflow.class);

        when(job.getWorkflowId()).thenReturn("wf1");
        when(jobService.fetchJob(7L)).thenReturn(Optional.of(job));
        when(mcpProject.getId()).thenReturn(10L);
        when(mcpProjectService.getMcpServerMcpProjects(30L)).thenAnswer(invocation -> {
            if (!AutomationAuthorizationContext.isSkipChecks()) {
                throw new AccessDeniedException("Access Denied");
            }

            return List.of(mcpProject);
        });
        when(mcpProjectWorkflow.getProjectDeploymentWorkflowId()).thenReturn(21L);
        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(10L)).thenReturn(List.of(mcpProjectWorkflow));
        when(projectDeploymentWorkflow.getWorkflowId()).thenReturn("wf1");
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(21L)).thenReturn(projectDeploymentWorkflow);

        assertThat(facade.isJobWorkflowExposedByMcpServer(7L, 30L)).isTrue();
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testJobWorkflowExposureSkipsStaleRowAndMatchesLaterRow() {
        Job job = mock(Job.class);
        McpProject mcpProject = mock(McpProject.class);
        McpProjectWorkflow staleMcpProjectWorkflow = mock(McpProjectWorkflow.class);
        McpProjectWorkflow mcpProjectWorkflow = mock(McpProjectWorkflow.class);
        ProjectDeploymentWorkflow projectDeploymentWorkflow = mock(ProjectDeploymentWorkflow.class);

        when(job.getWorkflowId()).thenReturn("wf1");
        when(jobService.fetchJob(11L)).thenReturn(Optional.of(job));
        when(mcpProject.getId()).thenReturn(10L);
        when(mcpProjectService.getMcpServerMcpProjects(30L)).thenReturn(List.of(mcpProject));
        when(staleMcpProjectWorkflow.getProjectDeploymentWorkflowId()).thenReturn(20L);
        when(mcpProjectWorkflow.getProjectDeploymentWorkflowId()).thenReturn(21L);
        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(10L))
            .thenReturn(List.of(staleMcpProjectWorkflow, mcpProjectWorkflow));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(20L))
            .thenThrow(new NoSuchElementException("No value present"));
        when(projectDeploymentWorkflow.getWorkflowId()).thenReturn("wf1");
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(21L)).thenReturn(projectDeploymentWorkflow);

        assertThat(facade.isJobWorkflowExposedByMcpServer(11L, 30L)).isTrue();
    }

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

    @Test
    void testCallReadsMcpServerConfigurationWithoutWorkspacePermissionChecks() {
        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback = givenListedTool();

        McpServer mcpServer = new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);
        AtomicBoolean executedSkippingChecks = new AtomicBoolean(true);

        when(mcpServerService.getMcpServer(30L)).thenAnswer(invocation -> requireSkipChecks(mcpServer));
        when(mcpToolService.fetchMcpTool(3L)).thenAnswer(invocation -> requireSkipChecks(Optional.of(mcpTool(true))));
        when(toolExecutionRecorder.record(any(), any(Supplier.class)))
            .thenAnswer(invocation -> {
                Supplier<Object> execution = invocation.getArgument(1);

                return execution.get();
            });
        when(clusterElementDefinitionFacade.executeTool(eq("slack"), eq(1), eq("sendMessage"), any(), isNull()))
            .thenAnswer(invocation -> {
                executedSkippingChecks.set(AutomationAuthorizationContext.isSkipChecks());

                return Map.of("ok", true);
            });

        functionToolCallback.call("{}");

        verify(clusterElementDefinitionFacade).executeTool(eq("slack"), eq(1), eq("sendMessage"), any(), isNull());
        assertThat(executedSkippingChecks).isFalse();
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    // The tool name is optional, so a tool configured without one still has to reach the model under a callable
    // name derived from the component and the cluster element.
    @Test
    void testToolNameFallsBackToTheClusterElement() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of());

        assertThat(toolDefinition.name()).isEqualTo("HTTPCLIENT_POST");
    }

    @Test
    void testToolNameUsesTheConfiguredName() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolName", "postThing"));

        assertThat(toolDefinition.name()).isEqualTo("postThing");
    }

    // The description is optional, so a tool configured without one is described to the model by the cluster
    // element's own description.
    @Test
    void testToolDescriptionFallsBackToTheClusterElement() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of());

        assertThat(toolDefinition.description())
            .isEqualTo("The POST method submits an entity to the specified resource.");
    }

    @Test
    void testToolDescriptionUsesTheConfiguredDescription() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolDescription", "Posts a thing"));

        assertThat(toolDefinition.description()).isEqualTo("Posts a thing");
    }

    @Test
    void testToolDescriptionFallsBackWhenTheConfiguredDescriptionIsBlank() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolDescription", "   "));

        assertThat(toolDefinition.description())
            .isEqualTo("The POST method submits an entity to the specified resource.");
    }

    private ToolDefinition getToolDefinition(Map<String, Object> parameters) {
        McpTool mcpTool = new McpTool("post", parameters, 1L);

        mcpTool.setId(4L);

        McpComponent mcpComponent = new McpComponent("httpClient", 1, 1L, null);

        when(mcpComponentService.getMcpComponent(1L)).thenReturn(mcpComponent);

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getComponentName()).thenReturn("httpClient");
        when(clusterElementDefinition.getComponentVersion()).thenReturn(1);
        when(clusterElementDefinition.getName()).thenReturn("post");
        when(clusterElementDefinition.getDescription())
            .thenReturn("The POST method submits an entity to the specified resource.");
        when(clusterElementDefinitionService.getClusterElementDefinition("httpClient", 1, "post"))
            .thenReturn(clusterElementDefinition);
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(1L)).thenReturn(Optional.empty());

        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback =
            facade.getFunctionToolCallback(mcpTool);

        return functionToolCallback.getToolDefinition();
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

    private static <T> T requireSkipChecks(T value) {
        if (!AutomationAuthorizationContext.isSkipChecks()) {
            throw new AccessDeniedException("Access Denied");
        }

        return value;
    }

    private static McpTool mcpTool(boolean enabled) {
        McpTool mcpTool = new McpTool("sendMessage", Map.of(), 2L);

        mcpTool.setId(3L);
        mcpTool.setEnabled(enabled);

        return mcpTool;
    }

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

            ArgumentCaptor<JobParametersDTO> jobParametersDTOArgumentCaptor =
                ArgumentCaptor.forClass(JobParametersDTO.class);

            verify(principalJobFacade).createJob(jobParametersDTOArgumentCaptor.capture(), anyLong(), any());

            JobParametersDTO jobParametersDTO = jobParametersDTOArgumentCaptor.getValue();

            assertThat(jobParametersDTO.getInputs())
                .containsEntry(JobInputConstants.TRIGGER_NAME_INPUT, "newWorkflowCall_1")
                .containsKey("newWorkflowCall_1");
        }
    }

    @Test
    void testCallOfWorkflowToolReadsMcpServerWithoutWorkspacePermissionChecks() {
        McpProject mcpProject = mcpProject();

        McpProjectWorkflow mappedMcpProjectWorkflow = mcpProjectWorkflow(
            21L, Map.of("toolName", "mappedTool", "toolDescription", "A mapped tool"));

        stubCallableWorkflow(21L, "wf2");

        McpServer mcpServer = new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);
        AtomicBoolean jobCreatedSkippingChecks = new AtomicBoolean(true);

        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(10L))
            .thenReturn(List.of(mappedMcpProjectWorkflow));
        when(mcpServerService.getMcpServer(30L)).thenAnswer(invocation -> {
            if (!AutomationAuthorizationContext.isSkipChecks()) {
                throw new AccessDeniedException("Access Denied");
            }

            return mcpServer;
        });
        when(principalJobFacade.createJob(any(), anyLong(), any())).thenAnswer(invocation -> {
            jobCreatedSkippingChecks.set(AutomationAuthorizationContext.isSkipChecks());

            return 5L;
        });
        when(toolExecutionRecorder.record(any(), any(Supplier.class))).thenReturn(Map.of("ok", true));

        try (MockedStatic<WorkflowTrigger> workflowTriggerMockedStatic = mockStatic(WorkflowTrigger.class);
            MockedStatic<WorkflowNodeType> workflowNodeTypeMockedStatic = mockStatic(WorkflowNodeType.class)) {

            stubNewWorkflowCallTrigger(workflowTriggerMockedStatic, workflowNodeTypeMockedStatic);

            List<ToolCallback> toolCallbacks = facade.getFunctionToolCallbacks(mcpProject);

            ToolCallback toolCallback = toolCallbacks.getFirst();

            toolCallback.call("{}");

            verify(principalJobFacade).createJob(any(), anyLong(), any());
            assertThat(jobCreatedSkippingChecks).isFalse();
            assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
        }
    }

    @Test
    void testCallOfWorkflowToolReportsPendingApprovalWhenRunIsPausedOnApproval() {
        Job job = mock(Job.class);

        when(job.getId()).thenReturn(5L);
        when(job.getStatus()).thenReturn(Job.Status.STOPPED);
        when(job.getMetadata(MetadataConstants.JOB_RESUME_ID)).thenReturn("resume-5");

        ToolCallback toolCallback = getWorkflowToolCallbackAwaiting(job);

        assertThat(toolCallback.call("{}")).contains("approval_required");
    }

    @Test
    void testCallOfWorkflowToolFailsWhenRunWasStoppedWithoutPendingApproval() {
        Job job = mock(Job.class);

        when(job.getId()).thenReturn(5L);
        when(job.getStatus()).thenReturn(Job.Status.STOPPED);

        ToolCallback toolCallback = getWorkflowToolCallbackAwaiting(job);

        assertThatThrownBy(() -> toolCallback.call("{}"))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessageContaining("was stopped")
            .hasCauseInstanceOf(ExecutionException.class);
    }

    private ToolCallback getWorkflowToolCallbackAwaiting(Job job) {
        McpProject mcpProject = mcpProject();

        McpProjectWorkflow mappedMcpProjectWorkflow = mcpProjectWorkflow(
            21L, Map.of("toolName", "mappedTool", "toolDescription", "A mapped tool"));

        stubCallableWorkflow(21L, "wf2");

        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(10L))
            .thenReturn(List.of(mappedMcpProjectWorkflow));
        when(mcpServerService.getMcpServer(30L)).thenReturn(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true));
        when(principalJobFacade.createJob(any(), anyLong(), any())).thenReturn(5L);
        when(jobCompletionAwaiter.await(anyLong(), any())).thenReturn(CompletableFuture.completedFuture(job));
        when(taskExecutionService.fetchLastJobTaskExecution(5L)).thenReturn(Optional.empty());
        when(toolExecutionRecorder.record(any(), any(Supplier.class)))
            .thenAnswer(invocation -> invocation.getArgument(1, Supplier.class)
                .get());

        try (MockedStatic<WorkflowTrigger> workflowTriggerMockedStatic = mockStatic(WorkflowTrigger.class);
            MockedStatic<WorkflowNodeType> workflowNodeTypeMockedStatic = mockStatic(WorkflowNodeType.class)) {

            stubNewWorkflowCallTrigger(workflowTriggerMockedStatic, workflowNodeTypeMockedStatic);

            List<ToolCallback> toolCallbacks = facade.getFunctionToolCallbacks(mcpProject);

            return toolCallbacks.getFirst();
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
