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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class ApprovalTaskReconciliationMonitorTest {

    private final ApprovalTaskService approvalTaskService = mock(ApprovalTaskService.class);
    private final JobService jobService = mock(JobService.class);
    private final TenantService tenantService = mock(TenantService.class);

    private ApprovalTaskReconciliationMonitor approvalTaskReconciliationMonitor;

    @BeforeEach
    void setUp() {
        approvalTaskReconciliationMonitor = new ApprovalTaskReconciliationMonitor(
            approvalTaskService, jobService, tenantService);

        when(tenantService.getTenantIds()).thenReturn(List.of("public"));
    }

    @Test
    void testExpiredRunMarksTaskExpired() {
        ApprovalTask approvalTask = approvalTask(42L);

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        when(jobService.fetchJob(42L)).thenReturn(java.util.Optional.of(job(Job.Status.FAILED)));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        ArgumentCaptor<ApprovalTask> approvalTaskArgumentCaptor = ArgumentCaptor.forClass(ApprovalTask.class);

        verify(approvalTaskService).update(approvalTaskArgumentCaptor.capture());

        ApprovalTask updatedApprovalTask = approvalTaskArgumentCaptor.getValue();

        assertEquals(ApprovalTask.Status.EXPIRED, updatedApprovalTask.getStatus());
    }

    @Test
    void testPurgedRunMarksTaskExpired() {
        ApprovalTask approvalTask = approvalTask(42L);

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        when(jobService.fetchJob(42L)).thenReturn(java.util.Optional.empty());

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        ArgumentCaptor<ApprovalTask> approvalTaskArgumentCaptor = ArgumentCaptor.forClass(ApprovalTask.class);

        verify(approvalTaskService).update(approvalTaskArgumentCaptor.capture());

        ApprovalTask updatedApprovalTask = approvalTaskArgumentCaptor.getValue();

        assertEquals(ApprovalTask.Status.EXPIRED, updatedApprovalTask.getStatus());
    }

    @Test
    void testTransientFetchFailureLeavesTaskUntouched() {
        ApprovalTask approvalTask = approvalTask(42L);

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        // A transient DB/REST failure must NOT be mistaken for a purged run — the still-resumable approval task must
        // be left untouched and retried on the next sweep, not permanently marked EXPIRED.
        when(jobService.fetchJob(42L)).thenThrow(new IllegalStateException("connection reset"));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        verify(approvalTaskService, never()).update(any());
    }

    @Test
    void testCompletedRunMarksTaskCompleted() {
        ApprovalTask approvalTask = approvalTask(42L);

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        when(jobService.fetchJob(42L)).thenReturn(java.util.Optional.of(job(Job.Status.COMPLETED)));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        ArgumentCaptor<ApprovalTask> approvalTaskArgumentCaptor = ArgumentCaptor.forClass(ApprovalTask.class);

        verify(approvalTaskService).update(approvalTaskArgumentCaptor.capture());

        ApprovalTask updatedApprovalTask = approvalTaskArgumentCaptor.getValue();

        assertEquals(ApprovalTask.Status.COMPLETED, updatedApprovalTask.getStatus());
    }

    @Test
    void testStoppedRunLeavesTaskUntouched() {
        ApprovalTask approvalTask = approvalTask(42L);

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));
        when(jobService.fetchJob(42L)).thenReturn(java.util.Optional.of(job(Job.Status.STOPPED)));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        verify(approvalTaskService, never()).update(any());
    }

    @Test
    void testUnparseableResumeIdLeavesTaskUntouched() {
        ApprovalTask approvalTask = new ApprovalTask();

        approvalTask.setJobResumeId("not-a-resume-id");

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        verify(approvalTaskService, never()).update(any());
    }

    private static ApprovalTask approvalTask(long jobId) {
        ApprovalTask approvalTask = new ApprovalTask();

        JobResumeId jobResumeId = JobResumeId.of(jobId);

        approvalTask.setJobResumeId(jobResumeId.toString());

        return approvalTask;
    }

    private static Job job(Job.Status status) {
        Job job = new Job();

        job.setStatus(status);

        return job;
    }
}
