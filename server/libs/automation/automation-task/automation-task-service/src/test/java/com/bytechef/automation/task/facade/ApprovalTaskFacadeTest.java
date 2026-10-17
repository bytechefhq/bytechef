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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ApprovalTaskFacadeTest {

    private final ApprovalTaskService approvalTaskService = mock(ApprovalTaskService.class);
    private final ApprovalTokens approvalTokens = mock(ApprovalTokens.class);
    private final JobService jobService = mock(JobService.class);
    private final PrincipalJobService principalJobService = mock(PrincipalJobService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);

    private ApprovalTaskFacade approvalTaskFacade;

    @BeforeEach
    void setUp() {
        approvalTaskFacade = new ApprovalTaskFacadeImpl(
            approvalTaskService, approvalTokens, jobService, principalJobService,
            projectDeploymentService, null, mock(TaskExecutionService.class), mock(WorkflowService.class));
    }

    @Nested
    class CreateApprovalTask {

        @Test
        void testSignedResumeTokenResolvesTheDeploymentEnvironment() {
            JobResumeId jobResumeId = JobResumeId.of("public", 42L);

            String signedToken = "signed." + jobResumeId;

            when(approvalTokens.resolveJobResumeId(signedToken)).thenReturn(Optional.of(jobResumeId));
            when(principalJobService.fetchJobPrincipalId(42L, PlatformType.AUTOMATION)).thenReturn(Optional.of(7L));
            when(projectDeploymentService.getProjectDeployment(7L))
                .thenReturn(projectDeployment(Environment.PRODUCTION));
            when(approvalTaskService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            ApprovalTask approvalTask = approvalTaskFacade.createApprovalTask(approvalTask(signedToken));

            assertEquals(Environment.PRODUCTION, approvalTask.getEnvironment());
        }

        @Test
        void testRunWithoutDeploymentIsDevelopment() {
            JobResumeId jobResumeId = JobResumeId.of("public", 42L);

            when(approvalTokens.resolveJobResumeId(jobResumeId.toString())).thenReturn(Optional.of(jobResumeId));
            when(principalJobService.fetchJobPrincipalId(42L, PlatformType.AUTOMATION)).thenReturn(Optional.empty());
            when(approvalTaskService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            ApprovalTask approvalTask = approvalTaskFacade.createApprovalTask(approvalTask(jobResumeId.toString()));

            assertEquals(Environment.DEVELOPMENT, approvalTask.getEnvironment());
        }

        @Test
        void testUnresolvableResumeTokenFailsTheCreate() {
            when(approvalTokens.resolveJobResumeId("forged")).thenReturn(Optional.empty());

            assertThrows(
                IllegalArgumentException.class,
                () -> approvalTaskFacade.createApprovalTask(approvalTask("forged")));

            verify(approvalTaskService, never()).create(any());
        }
    }

    @Nested
    class GetPendingApprovals {

        @Test
        void testEnvironmentFilterKeepsOnlyRunsOfThatEnvironment() {
            Job productionJob = suspendedJob(1L);
            Job developmentJob = suspendedJob(2L);

            when(jobService.getStaleJobs(eq(Job.Status.STOPPED), any())).thenReturn(
                List.of(productionJob, developmentJob));
            when(principalJobService.fetchJobPrincipalId(1L, PlatformType.AUTOMATION)).thenReturn(Optional.of(11L));
            when(principalJobService.fetchJobPrincipalId(2L, PlatformType.AUTOMATION)).thenReturn(Optional.empty());
            when(projectDeploymentService.getProjectDeployment(11L))
                .thenReturn(projectDeployment(Environment.PRODUCTION));

            List<PendingApproval> pendingApprovals = approvalTaskFacade.getPendingApprovals(
                Environment.PRODUCTION.ordinal());

            assertThat(pendingApprovals).extracting(PendingApproval::jobId)
                .containsExactly(1L);
        }

        @Test
        void testPendingApprovalCarriesItsExpiryAndAFormLinkThatExpiresWithIt() {
            TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);

            ApprovalTaskFacade publicUrlApprovalTaskFacade = new ApprovalTaskFacadeImpl(
                approvalTaskService, approvalTokens, jobService, principalJobService, projectDeploymentService,
                "https://example.com", taskExecutionService, mock(WorkflowService.class));

            Job job = new Job(1L);

            job.setStatus(Job.Status.STOPPED);
            job.setWorkflowId("workflow1");

            JobResumeId jobResumeId = JobResumeId.of("public", 1L);

            job.setMetadata(
                Map.of(
                    MetadataConstants.APPROVAL_RESUME, true, MetadataConstants.JOB_RESUME_ID, jobResumeId.toString(),
                    MetadataConstants.TASK_EXECUTION_RESUME_ID, 7L));

            Instant expiresAt = Instant.now()
                .plus(Duration.ofDays(2));

            TaskExecution taskExecution = new TaskExecution();

            taskExecution.putMetadata(MetadataConstants.SUSPEND, new Suspend(Map.of(), expiresAt));

            when(jobService.getStaleJobs(eq(Job.Status.STOPPED), any())).thenReturn(List.of(job));
            when(taskExecutionService.getTaskExecution(7L)).thenReturn(taskExecution);
            when(approvalTokens.toSignedTokenIfConfigured(eq(jobResumeId.toString()), any(Duration.class)))
                .thenReturn(Optional.of("v1.signed"));

            List<PendingApproval> pendingApprovals = publicUrlApprovalTaskFacade.getPendingApprovals(null);

            assertThat(pendingApprovals).hasSize(1);

            PendingApproval pendingApproval = pendingApprovals.getFirst();

            assertThat(pendingApproval.expiresAt()).isEqualTo(expiresAt);
            assertThat(pendingApproval.formUrl()).isEqualTo("https://example.com/resume/v1.signed");

            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

            verify(approvalTokens).toSignedTokenIfConfigured(eq(jobResumeId.toString()), ttlCaptor.capture());
            verify(approvalTokens, never()).toSignedTokenIfConfigured(jobResumeId.toString());

            assertThat(ttlCaptor.getValue()).isLessThanOrEqualTo(Duration.ofDays(2)
                .plusHours(1));
        }

        @Test
        void testOnlyRunsWaitingOnAnApprovalAreListed() {
            Job approvalJob = suspendedJob(1L);
            Job waitJob = suspendedJob(2L, false);

            when(jobService.getStaleJobs(eq(Job.Status.STOPPED), any())).thenReturn(List.of(approvalJob, waitJob));

            List<PendingApproval> pendingApprovals = approvalTaskFacade.getPendingApprovals(null);

            assertThat(pendingApprovals).extracting(PendingApproval::jobId)
                .containsExactly(1L);
        }

        private static Job suspendedJob(long id) {
            return suspendedJob(id, true);
        }

        private static Job suspendedJob(long id, boolean approvalResume) {
            Job job = new Job(id);

            job.setStatus(Job.Status.STOPPED);
            job.setWorkflowId("workflow" + id);

            JobResumeId jobResumeId = JobResumeId.of("public", id);

            job.setMetadata(
                Map.of(
                    MetadataConstants.APPROVAL_RESUME, approvalResume, MetadataConstants.JOB_RESUME_ID,
                    jobResumeId.toString()));

            return job;
        }
    }

    private static ApprovalTask approvalTask(String jobResumeId) {
        ApprovalTask approvalTask = new ApprovalTask();

        approvalTask.setJobResumeId(jobResumeId);

        return approvalTask;
    }

    private static ProjectDeployment projectDeployment(Environment environment) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(environment);

        return projectDeployment;
    }
}
