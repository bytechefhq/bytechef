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

package com.bytechef.platform.coordinator.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.coordinator.notification.NotificationThrottle;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationHandler;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSender;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings({
    "rawtypes", "unchecked"
})
class NotificationJobStatusApplicationEventListenerTest {

    private static final Instant START = Instant.parse("2026-10-19T10:00:00Z");

    private final Clock clock = mock(Clock.class);
    private final JobService jobService = mock(JobService.class);
    private final NotificationHandler notificationHandler = mock(NotificationHandler.class);
    private final NotificationHandlerRegistry notificationHandlerRegistry = mock(NotificationHandlerRegistry.class);
    private final NotificationSender notificationSender = mock(NotificationSender.class);
    private final NotificationSenderRegistry notificationSenderRegistry = mock(NotificationSenderRegistry.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final NotificationJobStatusApplicationEventListener notificationJobStatusApplicationEventListener =
        new NotificationJobStatusApplicationEventListener(
            null, jobService, notificationHandlerRegistry, notificationSenderRegistry, notificationService,
            new NotificationThrottle(Duration.ofHours(1), clock));

    @BeforeEach
    void beforeEach() {
        Notification notification = new Notification();

        notification.setId(1L);
        notification.setType(Notification.Type.EMAIL);

        when(notificationService.getNotifications(NotificationEvent.Type.JOB_COMPLETED))
            .thenReturn(List.of(notification));
        when(notificationService.getNotifications(NotificationEvent.Type.JOB_FAILED))
            .thenReturn(List.of(notification));
        when(notificationSenderRegistry.getNotificationSender(Notification.Type.EMAIL))
            .thenReturn(notificationSender);
        when(notificationHandlerRegistry.getNotificationHandler(
            NotificationEvent.Type.JOB_COMPLETED, Notification.Type.EMAIL))
                .thenReturn(notificationHandler);
        when(notificationHandlerRegistry.getNotificationHandler(
            NotificationEvent.Type.JOB_FAILED, Notification.Type.EMAIL))
                .thenReturn(notificationHandler);

        when(jobService.getJob(1L)).thenReturn(createJob(1L, "workflow1"));
        when(jobService.getJob(2L)).thenReturn(createJob(2L, "workflow1"));
        when(jobService.getJob(3L)).thenReturn(createJob(3L, "workflow1"));
        when(jobService.getJob(4L)).thenReturn(createJob(4L, "workflow2"));
    }

    @Test
    void testRepeatedFailuresOfTheSameWorkflowSendOneNotificationPerWindow() {
        failJobAt(1L, START);
        failJobAt(2L, START.plus(Duration.ofMinutes(5)));

        ArgumentCaptor<NotificationHandlerContext> notificationHandlerContextArgumentCaptor =
            ArgumentCaptor.forClass(NotificationHandlerContext.class);

        verify(notificationSender, times(1)).send(
            any(Notification.class), eq(notificationHandler), notificationHandlerContextArgumentCaptor.capture());

        assertThat(notificationHandlerContextArgumentCaptor.getValue()
            .getJobId()).isEqualTo(1L);
        assertThat(notificationHandlerContextArgumentCaptor.getValue()
            .getSuppressedCount()).isZero();
    }

    @Test
    void testFirstNotificationAfterTheWindowCarriesTheSuppressedCount() {
        failJobAt(1L, START);
        failJobAt(2L, START.plus(Duration.ofMinutes(5)));
        failJobAt(3L, START.plus(Duration.ofHours(1)));

        ArgumentCaptor<NotificationHandlerContext> notificationHandlerContextArgumentCaptor =
            ArgumentCaptor.forClass(NotificationHandlerContext.class);

        verify(notificationSender, times(2)).send(
            any(Notification.class), eq(notificationHandler), notificationHandlerContextArgumentCaptor.capture());

        NotificationHandlerContext notificationHandlerContext = notificationHandlerContextArgumentCaptor.getValue();

        assertThat(notificationHandlerContext.getJobId()).isEqualTo(3L);
        assertThat(notificationHandlerContext.getSuppressedCount()).isEqualTo(1);
    }

    @Test
    void testFailuresOfDifferentWorkflowsAreNotThrottledTogether() {
        failJobAt(1L, START);
        failJobAt(4L, START.plus(Duration.ofMinutes(5)));

        verify(notificationSender, times(2)).send(
            any(Notification.class), eq(notificationHandler), any(NotificationHandlerContext.class));
    }

    @Test
    void testRepeatedCompletionsOfTheSameWorkflowAreNotThrottled() {
        completeJobAt(1L, START);
        completeJobAt(2L, START.plus(Duration.ofMinutes(5)));
        completeJobAt(3L, START.plus(Duration.ofMinutes(10)));

        ArgumentCaptor<NotificationHandlerContext> notificationHandlerContextArgumentCaptor =
            ArgumentCaptor.forClass(NotificationHandlerContext.class);

        verify(notificationSender, times(3)).send(
            any(Notification.class), eq(notificationHandler), notificationHandlerContextArgumentCaptor.capture());

        assertThat(notificationHandlerContextArgumentCaptor.getAllValues())
            .extracting(NotificationHandlerContext::getSuppressedCount)
            .containsOnly(0L);
    }

    private void completeJobAt(long jobId, Instant instant) {
        changeJobStatusAt(jobId, Job.Status.COMPLETED, instant);
    }

    private void failJobAt(long jobId, Instant instant) {
        changeJobStatusAt(jobId, Job.Status.FAILED, instant);
    }

    private void changeJobStatusAt(long jobId, Job.Status status, Instant instant) {
        when(clock.instant()).thenReturn(instant);

        notificationJobStatusApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(jobId, status));
    }

    private static Job createJob(long id, String workflowId) {
        Job job = new Job(id);

        job.setLabel("Job " + id);
        job.setWorkflowId(workflowId);

        return job;
    }
}
