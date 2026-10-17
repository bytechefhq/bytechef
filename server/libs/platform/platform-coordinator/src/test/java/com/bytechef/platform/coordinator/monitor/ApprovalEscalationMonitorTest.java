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

package com.bytechef.platform.coordinator.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationHandler;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSender;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ApprovalEscalationMonitorTest {

    private final JobService jobService = mock(JobService.class);
    private final NotificationHandlerRegistry notificationHandlerRegistry = mock(NotificationHandlerRegistry.class);
    private final NotificationSenderRegistry notificationSenderRegistry = mock(NotificationSenderRegistry.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TenantService tenantService = mock(TenantService.class);

    @SuppressWarnings("rawtypes")
    private final NotificationSender notificationSender = mock(NotificationSender.class);
    private final NotificationHandler notificationHandler = mock(NotificationHandler.class);

    private ApprovalEscalationMonitor monitorWithAfter(Duration after) {
        when(tenantService.getTenantIds()).thenReturn(List.of("public"));

        return new ApprovalEscalationMonitor(
            after, null, jobService, notificationHandlerRegistry, notificationSenderRegistry, notificationService,
            "https://example.com", taskExecutionService, tenantService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testStalledApprovalSendsEscalationOnceAndMarksJob() {
        Instant now = Instant.now();

        Job job = stoppedApprovalJob(now.minus(Duration.ofHours(5)), now.plus(Duration.ofHours(10)));

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(job));

        Notification notification = new Notification();

        notification.setType(Notification.Type.EMAIL);

        when(notificationService.getNotifications(NotificationEvent.Type.JOB_APPROVAL_ESCALATED))
            .thenReturn(List.of(notification));
        when(notificationSenderRegistry.getNotificationSender(Notification.Type.EMAIL))
            .thenReturn(notificationSender);
        when(notificationHandlerRegistry.getNotificationHandler(
            NotificationEvent.Type.JOB_APPROVAL_ESCALATED, Notification.Type.EMAIL))
                .thenReturn(notificationHandler);

        monitorWithAfter(Duration.ofHours(4)).escalateStalledApprovals();

        ArgumentCaptor<NotificationHandlerContext> notificationHandlerContextArgumentCaptor =
            ArgumentCaptor.forClass(NotificationHandlerContext.class);

        verify(notificationSender).send(any(), any(), notificationHandlerContextArgumentCaptor.capture());

        NotificationHandlerContext notificationHandlerContext = notificationHandlerContextArgumentCaptor.getValue();

        assertEquals(NotificationEvent.Type.JOB_APPROVAL_ESCALATED, notificationHandlerContext.getEventType());
        assertNotNull(notificationHandlerContext.getApprovalFormUrl());

        ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobService).update(jobArgumentCaptor.capture());

        Job updatedJob = jobArgumentCaptor.getValue();

        assertNotNull(updatedJob.getMetadata("approvalEscalatedAt"));
    }

    @Test
    void testApprovalWithinEscalationWindowIsSkipped() {
        Instant now = Instant.now();

        Job job = stoppedApprovalJob(now.minus(Duration.ofHours(1)), now.plus(Duration.ofHours(10)));

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(job));

        monitorWithAfter(Duration.ofHours(4)).escalateStalledApprovals();

        verify(notificationService, never()).getNotifications(any());
        verify(jobService, never()).update(any());
    }

    @Test
    void testAlreadyEscalatedJobIsSkipped() {
        Instant now = Instant.now();

        Job job = stoppedApprovalJob(now.minus(Duration.ofHours(5)), now.plus(Duration.ofHours(10)));

        job.setMetadata(
            Map.of(
                "jobResumeId", "abc", "taskExecutionResumeId", 7L, "__bytechef_approval_resume__", true,
                "approvalEscalatedAt", now.toString()));

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(job));

        monitorWithAfter(Duration.ofHours(4)).escalateStalledApprovals();

        verify(notificationService, never()).getNotifications(any());
        verify(jobService, never()).update(any());
    }

    @Test
    void testExpiredApprovalIsSkipped() {
        Instant now = Instant.now();

        Job job = stoppedApprovalJob(now.minus(Duration.ofHours(5)), now.minus(Duration.ofHours(1)));

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(job));

        monitorWithAfter(Duration.ofHours(4)).escalateStalledApprovals();

        verify(notificationService, never()).getNotifications(any());
        verify(jobService, never()).update(any());
    }

    @Test
    void testNoAfterConfiguredIsNoOp() {
        monitorWithAfter(null).escalateStalledApprovals();

        verify(jobService, never()).getStaleJobs(any(), any());
        verify(notificationService, never()).getNotifications(any());
    }

    @Test
    void testEscalationIsNotSentWhenAnotherReplicaAlreadyClaimedIt() {
        Instant now = Instant.now();

        Job job = stoppedApprovalJob(now.minus(Duration.ofHours(5)), now.plus(Duration.ofHours(10)));

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(job));
        when(jobService.update(any())).thenThrow(new OptimisticLockingFailureException("claimed"));

        stubNotifications(notification(1L));

        monitorWithAfter(Duration.ofHours(4)).escalateStalledApprovals();

        verify(notificationSender, never()).send(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testOneFailingNotificationDoesNotSkipTheOthers() {
        Instant now = Instant.now();

        Job job = stoppedApprovalJob(now.minus(Duration.ofHours(5)), now.plus(Duration.ofHours(10)));

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(job));
        when(jobService.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Notification firstNotification = notification(1L);
        Notification secondNotification = notification(2L);

        stubNotifications(firstNotification, secondNotification);

        doThrow(new IllegalStateException("smtp down"))
            .when(notificationSender)
            .send(eq(firstNotification), any(), any());

        monitorWithAfter(Duration.ofHours(4)).escalateStalledApprovals();

        verify(notificationSender).send(eq(secondNotification), any(), any());
        verify(jobService).update(job);

        assertNotNull(job.getMetadata("approvalEscalatedAt"));
    }

    @Test
    void testNoConfiguredNotificationClearsTheEscalationMarkerForTheNextSweep() {
        Instant now = Instant.now();

        Job job = stoppedApprovalJob(now.minus(Duration.ofHours(5)), now.plus(Duration.ofHours(10)));

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(job));
        when(jobService.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationService.getNotifications(NotificationEvent.Type.JOB_APPROVAL_ESCALATED))
            .thenReturn(List.of());

        monitorWithAfter(Duration.ofHours(4)).escalateStalledApprovals();

        verify(jobService, times(2)).update(job);

        assertNull(job.getMetadata("approvalEscalatedAt"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testTotalSendFailureClearsTheEscalationMarkerForTheNextSweep() {
        Instant now = Instant.now();

        Job job = stoppedApprovalJob(now.minus(Duration.ofHours(5)), now.plus(Duration.ofHours(10)));

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(job));
        when(jobService.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        stubNotifications(notification(1L));

        doThrow(new IllegalStateException("smtp down"))
            .when(notificationSender)
            .send(any(), any(), any());

        monitorWithAfter(Duration.ofHours(4)).escalateStalledApprovals();

        verify(jobService, times(2)).update(job);

        assertNull(job.getMetadata("approvalEscalatedAt"));
    }

    @Test
    void testEscalationWindowStartsWhenTheApprovalWasRaised() {
        Instant now = Instant.now();

        Job job = stoppedApprovalJob(
            now.minus(Duration.ofHours(5)), now.minus(Duration.ofHours(1)), now.plus(Duration.ofHours(10)));

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(job));

        monitorWithAfter(Duration.ofHours(4)).escalateStalledApprovals();

        verify(notificationService, never()).getNotifications(any());
        verify(jobService, never()).update(any());
    }

    @Test
    void testEscalationSweepContinuesPastAJobWhoseClaimFailsUnexpectedly() {
        Instant now = Instant.now();

        Job failingJob = stoppedApprovalJob(now.minus(Duration.ofHours(5)), now.plus(Duration.ofHours(10)));
        Job escalatedJob = stoppedApprovalJob(now.minus(Duration.ofHours(5)), now.plus(Duration.ofHours(10)));

        escalatedJob.setId(43L);

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(failingJob, escalatedJob));
        when(jobService.update(any()))
            .thenThrow(new IllegalStateException("database unavailable"))
            .thenAnswer(invocation -> invocation.getArgument(0));

        stubNotifications(notification(1L));

        monitorWithAfter(Duration.ofHours(4)).escalateStalledApprovals();

        ArgumentCaptor<NotificationHandlerContext> notificationHandlerContextArgumentCaptor =
            ArgumentCaptor.forClass(NotificationHandlerContext.class);

        verify(notificationSender).send(any(), any(), notificationHandlerContextArgumentCaptor.capture());

        NotificationHandlerContext notificationHandlerContext = notificationHandlerContextArgumentCaptor.getValue();

        assertEquals(Long.valueOf(43L), notificationHandlerContext.getJobId());
    }

    private void stubNotifications(Notification... notifications) {
        when(notificationService.getNotifications(NotificationEvent.Type.JOB_APPROVAL_ESCALATED))
            .thenReturn(List.of(notifications));
        when(notificationSenderRegistry.getNotificationSender(Notification.Type.EMAIL))
            .thenReturn(notificationSender);
        when(notificationHandlerRegistry.getNotificationHandler(
            NotificationEvent.Type.JOB_APPROVAL_ESCALATED, Notification.Type.EMAIL))
                .thenReturn(notificationHandler);
    }

    private static Notification notification(long id) {
        Notification notification = new Notification();

        notification.setId(id);
        notification.setType(Notification.Type.EMAIL);

        return notification;
    }

    private Job stoppedApprovalJob(Instant startedAt, Instant expiresAt) {
        return stoppedApprovalJob(startedAt, null, expiresAt);
    }

    private Job stoppedApprovalJob(Instant startedAt, @Nullable Instant raisedAt, Instant expiresAt) {
        Job job = new Job();

        job.setId(42L);
        job.setLabel("Test Workflow");
        job.setStatus(Job.Status.STOPPED);
        job.setMetadata(
            Map.of("jobResumeId", "abc", "taskExecutionResumeId", 7L, "__bytechef_approval_resume__", true));

        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setStartDate(startedAt);

        Map<String, Object> suspend = new HashMap<>();

        suspend.put("expiresAt", expiresAt.toString());

        if (raisedAt != null) {
            suspend.put("continueParameters", Map.of("approvalRaisedAt", raisedAt.toString()));
        }

        taskExecution.setMetadata(Map.of("suspend", suspend));

        when(taskExecutionService.getTaskExecution(anyLong())).thenReturn(taskExecution);

        return job;
    }
}
