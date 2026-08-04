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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.plan.domain.PlanLimits;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the MCP approval re-await contract: a resumed run yields its outputs, a still-paused (or re-paused) run yields
 * an {@code approval_required} descriptor, and a resume that can no longer be resolved yields {@code
 * approval_unavailable} instead of throwing.
 *
 * @author Ivica Cardic
 */
@SuppressWarnings("unchecked")
class AutomationMcpToolFacadeTest {

    private final JobCompletionAwaiter jobCompletionAwaiter = mock(JobCompletionAwaiter.class);
    private final JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);
    private final JobService jobService = mock(JobService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TaskFileStorage taskFileStorage = mock(TaskFileStorage.class);

    private final ObjectProvider<ApprovalTokens> approvalTokensObjectProvider =
        (ObjectProvider<ApprovalTokens>) mock(ObjectProvider.class);
    private final ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider =
        (ObjectProvider<PlanLimitsProvider>) mock(ObjectProvider.class);

    private final AutomationMcpToolFacade facade = new AutomationMcpToolFacade(
        approvalTokensObjectProvider, mock(ClusterElementDefinitionFacade.class),
        mock(ClusterElementDefinitionService.class), mock(Evaluator.class), jobCompletionAwaiter, jobResumeFacade,
        jobService, mock(McpComponentService.class), mock(McpProjectService.class),
        mock(McpProjectWorkflowService.class), mock(McpServerService.class), mock(McpToolService.class),
        planLimitsProviderObjectProvider, mock(PrincipalJobFacade.class),
        mock(ProjectDeploymentWorkflowService.class), "https://example.com", taskExecutionService, taskFileStorage,
        mock(ToolExecutionRecorder.class), mock(WorkflowService.class), mock(WorkspaceMcpServerService.class));

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
    void testAwaitApprovedWorkflowRunReturnsDescriptorWhenDeadlinePassesWhileStopped() {
        givenTinySyncTimeout();

        Job job = mock(Job.class);

        when(job.getId()).thenReturn(2L);
        when(job.getStatus()).thenReturn(Job.Status.STOPPED);
        when(jobService.getJob(2L)).thenReturn(job);

        Object result = facade.awaitApprovedWorkflowRun(2L);

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
        // STARTED for the poll-loop guard and the pre-awaiter check (the resume flipped it off STOPPED), then STOPPED
        // once the awaiter hands it back: the run paused again on a second approval.
        when(job.getStatus()).thenReturn(Job.Status.STARTED, Job.Status.STARTED, Job.Status.STOPPED);
        when(jobService.getJob(3L)).thenReturn(job);
        when(jobCompletionAwaiter.await(anyLong(), any())).thenReturn(CompletableFuture.completedFuture(job));

        Object result = facade.awaitApprovedWorkflowRun(3L);

        assertThat((Map<String, Object>) result).containsEntry("status", "approval_required");
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

    private void givenTinySyncTimeout() {
        PlanLimitsProvider planLimitsProvider = mock(PlanLimitsProvider.class);
        PlanLimits planLimits = mock(PlanLimits.class);

        when(planLimits.syncRunTimeout()).thenReturn(Duration.ofMillis(1));
        when(planLimitsProvider.getPlanLimits(any())).thenReturn(planLimits);
        when(planLimitsProviderObjectProvider.getIfAvailable()).thenReturn(planLimitsProvider);
    }
}
