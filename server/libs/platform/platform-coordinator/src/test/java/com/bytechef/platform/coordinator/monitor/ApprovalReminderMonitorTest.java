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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ApprovalReminderMonitorTest {

    private final JobService jobService = mock(JobService.class);
    private final NotificationHandlerRegistry notificationHandlerRegistry = mock(NotificationHandlerRegistry.class);
    private final NotificationSenderRegistry notificationSenderRegistry = mock(NotificationSenderRegistry.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TenantService tenantService = mock(TenantService.class);

    @SuppressWarnings("rawtypes")
    private final NotificationSender notificationSender = mock(NotificationSender.class);
    private final NotificationHandler notificationHandler = mock(NotificationHandler.class);

    private ApprovalReminderMonitor approvalReminderMonitor;

    @BeforeEach
    void setUp() {
        approvalReminderMonitor = new ApprovalReminderMonitor(
            null, jobService, Duration.ofHours(24), notificationHandlerRegistry, notificationSenderRegistry,
            notificationService, "https://example.com", taskExecutionService, tenantService);

        when(tenantService.getTenantIds()).thenReturn(List.of("public"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testExpiringApprovalSendsReminderOnceAndMarksJob() {
        Job job = stoppedApprovalJob(Instant.now()
            .plus(Duration.ofHours(4)));

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(job));

        Notification notification = new Notification();

        notification.setType(Notification.Type.EMAIL);

        when(notificationService.getNotifications(NotificationEvent.Type.JOB_APPROVAL_EXPIRING))
            .thenReturn(List.of(notification));
        when(notificationSenderRegistry.getNotificationSender(Notification.Type.EMAIL))
            .thenReturn(notificationSender);
        when(notificationHandlerRegistry.getNotificationHandler(
            NotificationEvent.Type.JOB_APPROVAL_EXPIRING, Notification.Type.EMAIL))
                .thenReturn(notificationHandler);

        approvalReminderMonitor.remindExpiringApprovals();

        ArgumentCaptor<NotificationHandlerContext> notificationHandlerContextArgumentCaptor =
            ArgumentCaptor.forClass(NotificationHandlerContext.class);

        verify(notificationSender).send(
            any(), any(), notificationHandlerContextArgumentCaptor.capture());

        NotificationHandlerContext notificationHandlerContext = notificationHandlerContextArgumentCaptor.getValue();

        assertEquals(NotificationEvent.Type.JOB_APPROVAL_EXPIRING, notificationHandlerContext.getEventType());
        assertNotNull(notificationHandlerContext.getApprovalExpiresAt());
        assertNotNull(notificationHandlerContext.getApprovalFormUrl());

        ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobService).update(jobArgumentCaptor.capture());

        Job updatedJob = jobArgumentCaptor.getValue();

        assertNotNull(updatedJob.getMetadata("approvalReminderSentAt"));
    }

    @Test
    void testReminderNotSentWhenAnotherReplicaAlreadyClaimedIt() {
        Job job = stoppedApprovalJob(Instant.now()
            .plus(Duration.ofHours(4)));

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(job));
        when(jobService.update(any()))
            .thenThrow(new OptimisticLockingFailureException("claimed by another replica"));

        approvalReminderMonitor.remindExpiringApprovals();

        // Losing the pre-send optimistic claim (another coordinator replica marked the reminder first) must skip the
        // send, so two replicas never emit duplicate JOB_APPROVAL_EXPIRING notifications.
        verify(notificationSender, never()).send(any(), any(), any());
    }

    @Test
    void testAlreadyRemindedJobIsSkipped() {
        Job job = stoppedApprovalJob(Instant.now()
            .plus(Duration.ofHours(4)));

        job.setMetadata(
            Map.of(
                "jobResumeId", "abc", "taskExecutionResumeId", 7L,
                "approvalReminderSentAt", Instant.now()
                    .toString()));

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(job));

        approvalReminderMonitor.remindExpiringApprovals();

        verify(notificationService, never()).getNotifications(any());
        verify(jobService, never()).update(any());
    }

    @Test
    void testApprovalOutsideLeadTimeIsSkipped() {
        Job job = stoppedApprovalJob(Instant.now()
            .plus(Duration.ofDays(10)));

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(job));

        approvalReminderMonitor.remindExpiringApprovals();

        verify(notificationService, never()).getNotifications(any());
        verify(jobService, never()).update(any());
    }

    @Test
    void testExpiredApprovalIsSkipped() {
        Job job = stoppedApprovalJob(Instant.now()
            .minus(Duration.ofHours(1)));

        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of(job));

        approvalReminderMonitor.remindExpiringApprovals();

        verify(notificationService, never()).getNotifications(any());
        verify(jobService, never()).update(any());
    }

    private Job stoppedApprovalJob(Instant expiresAt) {
        Job job = new Job();

        job.setId(42L);
        job.setLabel("Test Workflow");
        job.setStatus(Job.Status.STOPPED);
        job.setMetadata(Map.of("jobResumeId", "abc", "taskExecutionResumeId", 7L));

        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setMetadata(Map.of("suspend", Map.of("expiresAt", expiresAt.toString())));

        when(taskExecutionService.getTaskExecution(anyLong())).thenReturn(taskExecution);

        return job;
    }
}
