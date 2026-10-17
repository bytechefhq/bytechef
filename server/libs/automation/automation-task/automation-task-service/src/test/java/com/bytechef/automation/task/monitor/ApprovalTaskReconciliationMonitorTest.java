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

package com.bytechef.automation.task.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.service.TenantService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * @author Ivica Cardic
 */
class ApprovalTaskReconciliationMonitorTest {

    private final ApprovalTaskService approvalTaskService = mock(ApprovalTaskService.class);
    private final ApprovalTokens approvalTokens = new SigningApprovalTokens();
    private final JobService jobService = mock(JobService.class);
    private final TenantService tenantService = mock(TenantService.class);

    private ApprovalTaskReconciliationMonitor approvalTaskReconciliationMonitor;

    @BeforeEach
    void setUp() {
        approvalTaskReconciliationMonitor = new ApprovalTaskReconciliationMonitor(
            approvalTaskService, approvalTokens, jobService, tenantService);

        when(tenantService.getTenantIds()).thenReturn(List.of("public"));
    }

    @Test
    void testExpiredRunMarksTaskExpired() {
        ApprovalTask approvalTask = approvalTask(42L);

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        when(jobService.fetchJob(42L)).thenReturn(Optional.of(job(Job.Status.FAILED)));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        ArgumentCaptor<ApprovalTask> approvalTaskArgumentCaptor = ArgumentCaptor.forClass(ApprovalTask.class);

        verify(approvalTaskService).resolve(approvalTaskArgumentCaptor.capture());

        ApprovalTask updatedApprovalTask = approvalTaskArgumentCaptor.getValue();

        assertEquals(ApprovalTask.Status.EXPIRED, updatedApprovalTask.getStatus());
    }

    @Test
    void testPurgedRunMarksTaskExpired() {
        ApprovalTask approvalTask = approvalTask(42L);

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        when(jobService.fetchJob(42L)).thenReturn(Optional.empty());

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        ArgumentCaptor<ApprovalTask> approvalTaskArgumentCaptor = ArgumentCaptor.forClass(ApprovalTask.class);

        verify(approvalTaskService).resolve(approvalTaskArgumentCaptor.capture());

        ApprovalTask updatedApprovalTask = approvalTaskArgumentCaptor.getValue();

        assertEquals(ApprovalTask.Status.EXPIRED, updatedApprovalTask.getStatus());
    }

    @Test
    void testTransientFetchFailureLeavesTaskUntouched() {
        ApprovalTask approvalTask = approvalTask(42L);

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        when(jobService.fetchJob(42L)).thenThrow(new IllegalStateException("connection reset"));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        verify(approvalTaskService, never()).resolve(any());
    }

    @Test
    void testCompletedRunMarksTaskCompleted() {
        ApprovalTask approvalTask = approvalTask(42L);

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        when(jobService.fetchJob(42L)).thenReturn(Optional.of(job(Job.Status.COMPLETED)));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        ArgumentCaptor<ApprovalTask> approvalTaskArgumentCaptor = ArgumentCaptor.forClass(ApprovalTask.class);

        verify(approvalTaskService).resolve(approvalTaskArgumentCaptor.capture());

        ApprovalTask updatedApprovalTask = approvalTaskArgumentCaptor.getValue();

        assertEquals(ApprovalTask.Status.COMPLETED, updatedApprovalTask.getStatus());
    }

    @Test
    void testRunSuspendedOnThisApprovalLeavesTaskUntouched() {
        ApprovalTask approvalTask = approvalTask(42L);

        Job job = job(Job.Status.STOPPED);

        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, approvalTask.getJobResumeId()));

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        when(jobService.fetchJob(42L)).thenReturn(Optional.of(job));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        verify(approvalTaskService, never()).resolve(any());
    }

    @Test
    void testStoppedRunWithoutResumeIdMarksTaskExpired() {
        ApprovalTask approvalTask = approvalTask(42L);

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        when(jobService.fetchJob(42L)).thenReturn(Optional.of(job(Job.Status.STOPPED)));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        verify(approvalTaskService).resolve(approvalTask);

        assertEquals(ApprovalTask.Status.EXPIRED, approvalTask.getStatus());
    }

    @Test
    void testRunningJobLeavesTaskUntouched() {
        ApprovalTask approvalTask = approvalTask(42L);

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        when(jobService.fetchJob(42L)).thenReturn(Optional.of(job(Job.Status.STARTED)));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        verify(approvalTaskService, never()).resolve(any());
    }

    @Test
    void testUnparseableResumeIdLeavesTaskUntouched() {
        ApprovalTask approvalTask = new ApprovalTask();

        approvalTask.setJobResumeId("not-a-resume-id");

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        verify(approvalTaskService, never()).resolve(any());
    }

    @Test
    void testSignedResumeTokenIsUnwrappedBeforeParsing() {
        JobResumeId jobResumeId = JobResumeId.of("public", 42L);

        ApprovalTask approvalTask = new ApprovalTask();

        approvalTask.setJobResumeId(approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofDays(1)));

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        when(jobService.fetchJob(42L)).thenReturn(Optional.of(job(Job.Status.COMPLETED)));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        ArgumentCaptor<ApprovalTask> approvalTaskArgumentCaptor = ArgumentCaptor.forClass(ApprovalTask.class);

        verify(approvalTaskService).resolve(approvalTaskArgumentCaptor.capture());

        ApprovalTask updatedApprovalTask = approvalTaskArgumentCaptor.getValue();

        assertEquals(ApprovalTask.Status.COMPLETED, updatedApprovalTask.getStatus());
    }

    @Test
    void testFailedUpdateDoesNotSkipTheRemainingTasks() {
        ApprovalTask firstApprovalTask = approvalTask(41L);
        ApprovalTask secondApprovalTask = approvalTask(42L);

        firstApprovalTask.setId(1L);
        secondApprovalTask.setId(2L);

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(
            List.of(firstApprovalTask, secondApprovalTask));
        when(jobService.fetchJob(41L)).thenReturn(Optional.of(job(Job.Status.COMPLETED)));
        when(jobService.fetchJob(42L)).thenReturn(Optional.of(job(Job.Status.COMPLETED)));
        doThrow(new OptimisticLockingFailureException("stale"))
            .when(approvalTaskService)
            .resolve(firstApprovalTask);

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        verify(approvalTaskService).resolve(secondApprovalTask);

        assertEquals(ApprovalTask.Status.COMPLETED, secondApprovalTask.getStatus());
    }

    @Test
    void testStoppedRunSuspendedElsewhereMarksTaskCompleted() {
        ApprovalTask approvalTask = approvalTask(42L);

        Job job = job(Job.Status.STOPPED);

        JobResumeId otherJobResumeId = JobResumeId.of("public", 42L);

        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, otherJobResumeId.toString()));

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        when(jobService.fetchJob(42L)).thenReturn(Optional.of(job));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        verify(approvalTaskService).resolve(approvalTask);

        assertEquals(ApprovalTask.Status.COMPLETED, approvalTask.getStatus());
    }

    @Test
    void testCancelledRunMarksTaskExpired() {
        ApprovalTask approvalTask = approvalTask(42L);

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        when(jobService.fetchJob(42L)).thenReturn(Optional.of(job(Job.Status.CANCELLED)));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        verify(approvalTaskService).resolve(approvalTask);

        assertEquals(ApprovalTask.Status.EXPIRED, approvalTask.getStatus());
    }

    private static ApprovalTask approvalTask(long jobId) {
        ApprovalTask approvalTask = new ApprovalTask();

        JobResumeId jobResumeId = JobResumeId.of("public", jobId);

        approvalTask.setJobResumeId(jobResumeId.toString());

        return approvalTask;
    }

    private static Job job(Job.Status status) {
        Job job = new Job();

        job.setStatus(status);

        return job;
    }

    private static final class SigningApprovalTokens implements ApprovalTokens {

        private static final String PREFIX = "signed.";

        @Override
        public String toSignedToken(String innerToken, Duration ttl) {
            return PREFIX + innerToken;
        }

        @Override
        public Optional<String> toSignedTokenIfConfigured(String innerToken) {
            return Optional.of(PREFIX + innerToken);
        }

        @Override
        public Optional<String> toSignedTokenIfConfigured(String innerToken, Duration ttl) {
            return Optional.of(PREFIX + innerToken);
        }

        @Override
        public Optional<String> parseSignedToken(String token) {
            return token.startsWith(PREFIX) ? Optional.of(token.substring(PREFIX.length())) : Optional.empty();
        }

        @Override
        public boolean looksLikeSignedToken(String token) {
            return token.startsWith(PREFIX);
        }

        @Override
        public boolean isSignedTokenRequired() {
            return false;
        }
    }
}
