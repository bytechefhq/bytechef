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

package com.bytechef.automation.task.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.PendingApproval;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ApprovalTaskFacadeTest {

    private static final String PUBLIC_URL = "https://bytechef.example.com";

    @Mock
    private ApprovalTaskService approvalTaskService;

    @Mock
    private ApprovalTokens approvalTokens;

    @Mock
    private JobService jobService;

    @Mock
    private PrincipalJobService principalJobService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private TaskExecutionService taskExecutionService;

    @Mock
    private WorkflowService workflowService;

    @Test
    void testCreateApprovalTaskUnwrapsSignedTokenAndPersistsInnerToken() {
        String innerToken = Base64.getEncoder()
            .encodeToString(("public:123:" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));
        String signedToken = "v1.9999999999.payload.signature";

        when(approvalTokens.resolveInnerToken(signedToken)).thenReturn(Optional.of(innerToken));
        when(principalJobService.getJobPrincipalId(123L, PlatformType.AUTOMATION)).thenReturn(7L);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);

        when(projectDeploymentService.getProjectDeployment(7L)).thenReturn(projectDeployment);

        ApprovalTask approvalTask = ApprovalTask.builder()
            .jobResumeId(signedToken)
            .build();

        when(approvalTaskService.create(approvalTask)).thenReturn(approvalTask);

        ApprovalTask result = createApprovalTaskFacade().createApprovalTask(approvalTask);

        assertThat(result.getJobResumeId()).isEqualTo(innerToken);
        assertThat(result.getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testCreateApprovalTaskFallsBackToDevelopmentWhenEnvironmentCannotBeResolved() {
        String innerToken = Base64.getEncoder()
            .encodeToString(("public:789:" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));

        when(approvalTokens.resolveInnerToken(innerToken)).thenReturn(Optional.of(innerToken));
        // A run not backed by an automation project deployment has no principal-job row.
        when(principalJobService.getJobPrincipalId(789L, PlatformType.AUTOMATION))
            .thenThrow(new IllegalArgumentException("no principal job"));

        ApprovalTask approvalTask = ApprovalTask.builder()
            .jobResumeId(innerToken)
            .build();

        when(approvalTaskService.create(approvalTask)).thenReturn(approvalTask);

        ApprovalTask result = createApprovalTaskFacade().createApprovalTask(approvalTask);

        assertThat(result.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
    }

    @Test
    void testCreateApprovalTaskAcceptsLegacyUnsignedInnerToken() {
        String innerToken = Base64.getEncoder()
            .encodeToString(("public:456:" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));

        when(approvalTokens.resolveInnerToken(innerToken)).thenReturn(Optional.of(innerToken));
        when(principalJobService.getJobPrincipalId(456L, PlatformType.AUTOMATION)).thenReturn(9L);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.STAGING);

        when(projectDeploymentService.getProjectDeployment(9L)).thenReturn(projectDeployment);

        ApprovalTask approvalTask = ApprovalTask.builder()
            .jobResumeId(innerToken)
            .build();

        ArgumentCaptor<ApprovalTask> approvalTaskArgumentCaptor = ArgumentCaptor.forClass(ApprovalTask.class);

        when(approvalTaskService.create(approvalTaskArgumentCaptor.capture())).thenReturn(approvalTask);

        createApprovalTaskFacade().createApprovalTask(approvalTask);

        assertThat(approvalTaskArgumentCaptor.getValue()
            .getJobResumeId()).isEqualTo(innerToken);
    }

    @Test
    void testGetPendingApprovalsListsStoppedJobsWithResumeIdsNewestFirst() {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(3600);

        Job olderJob = pendingJob(1L, "tokenA", 7L, now.minusSeconds(3600), "workflow-a");
        Job newerJob = pendingJob(2L, "tokenB", null, now, "workflow-b");
        Job jobWithoutResumeId = mock(Job.class);

        when(jobWithoutResumeId.getMetadata("jobResumeId")).thenReturn(null);
        when(jobService.getStaleJobs(any(Job.Status.class), any(Instant.class)))
            .thenReturn(List.of(olderJob, newerJob, jobWithoutResumeId));

        Workflow labeledWorkflow = mock(Workflow.class);

        when(labeledWorkflow.getLabel()).thenReturn("Order Approval");
        when(workflowService.getWorkflow("workflow-a")).thenReturn(labeledWorkflow);
        when(workflowService.getWorkflow("workflow-b")).thenThrow(new IllegalArgumentException("missing"));

        TaskExecution suspendedTaskExecution = new TaskExecution();

        suspendedTaskExecution.putMetadata("suspend", Map.of("expiresAt", expiresAt.toEpochMilli()));

        when(taskExecutionService.getTaskExecution(7L)).thenReturn(suspendedTaskExecution);

        lenient().when(approvalTokens.toSignedTokenIfConfigured(any()))
            .thenReturn(Optional.empty());

        List<PendingApproval> pendingApprovals = createApprovalTaskFacade().getPendingApprovals(null);

        assertThat(pendingApprovals).hasSize(2);

        PendingApproval newest = pendingApprovals.get(0);

        assertThat(newest.jobId()).isEqualTo(2L);
        assertThat(newest.workflowLabel()).isEqualTo("workflow-b");
        assertThat(newest.formUrl()).isEqualTo(PUBLIC_URL + "/resume/tokenB");
        assertThat(newest.expiresAt()).isNull();

        PendingApproval oldest = pendingApprovals.get(1);

        assertThat(oldest.jobId()).isEqualTo(1L);
        assertThat(oldest.workflowLabel()).isEqualTo("Order Approval");
        assertThat(oldest.formUrl()).isEqualTo(PUBLIC_URL + "/resume/tokenA");
        assertThat(oldest.expiresAt()).isEqualTo(Instant.ofEpochMilli(expiresAt.toEpochMilli()));
    }

    @Test
    void testGetPendingApprovalsReturnsEmptyListWhenNoJobsAreStopped() {
        when(jobService.getStaleJobs(any(Job.Status.class), any(Instant.class))).thenReturn(List.of());

        assertThat(createApprovalTaskFacade().getPendingApprovals(null)).isEmpty();
    }

    @Test
    void testGetPendingApprovalsFiltersByEnvironment() {
        Job job = pendingJob(1L, "tokenA", null, Instant.now(), "workflow-a");

        when(jobService.getStaleJobs(any(Job.Status.class), any(Instant.class))).thenReturn(List.of(job));
        when(workflowService.getWorkflow("workflow-a")).thenThrow(new IllegalArgumentException("missing"));

        lenient().when(approvalTokens.toSignedTokenIfConfigured(any()))
            .thenReturn(Optional.empty());

        ApprovalTaskFacadeImpl approvalTaskFacade = createApprovalTaskFacade();

        // The stub token is not a resolvable principal-backed run, so its environment falls back to DEVELOPMENT: it is
        // included when filtering to DEVELOPMENT and excluded when filtering to another environment.
        assertThat(approvalTaskFacade.getPendingApprovals(Environment.DEVELOPMENT.ordinal())).hasSize(1);
        assertThat(approvalTaskFacade.getPendingApprovals(Environment.PRODUCTION.ordinal())).isEmpty();
    }

    private ApprovalTaskFacadeImpl createApprovalTaskFacade() {
        return new ApprovalTaskFacadeImpl(
            approvalTaskService, approvalTokens, jobService, principalJobService, projectDeploymentService, PUBLIC_URL,
            taskExecutionService, workflowService);
    }

    private Job pendingJob(
        long jobId, String jobResumeId, Long taskExecutionResumeId, Instant createdDate, String workflowId) {

        Job job = mock(Job.class);

        Map<String, Object> metadata = new HashMap<>();

        metadata.put("jobResumeId", jobResumeId);

        if (taskExecutionResumeId != null) {
            metadata.put("taskExecutionResumeId", taskExecutionResumeId);
        }

        lenient().when(job.getMetadata("jobResumeId"))
            .thenReturn(jobResumeId);
        lenient().doReturn(metadata)
            .when(job)
            .getMetadata();
        lenient().when(job.getId())
            .thenReturn(jobId);
        lenient().when(job.getCreatedDate())
            .thenReturn(createdDate);
        lenient().when(job.getWorkflowId())
            .thenReturn(workflowId);

        return job;
    }
}
