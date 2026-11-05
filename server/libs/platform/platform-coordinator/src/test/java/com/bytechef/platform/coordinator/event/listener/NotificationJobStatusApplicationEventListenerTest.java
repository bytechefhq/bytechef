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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.coordinator.metrics.JobExecutionCounter;
import com.bytechef.platform.coordinator.notification.NotificationHandlerContexts;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationHandler;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSender;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    private static final long JOB_ID = 7L;

    private final NotificationHandler emailNotificationHandler = mock(NotificationHandler.class);
    private final NotificationSender emailNotificationSender = mock(NotificationSender.class);
    private final Job job = new Job();
    private final JobExecutionCounter jobExecutionCounter = mock(JobExecutionCounter.class);
    private final JobService jobService = mock(JobService.class);
    private final NotificationHandlerContexts notificationHandlerContexts = mock(NotificationHandlerContexts.class);
    private final NotificationHandlerRegistry notificationHandlerRegistry = mock(NotificationHandlerRegistry.class);
    private final NotificationSenderRegistry notificationSenderRegistry = mock(NotificationSenderRegistry.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final NotificationHandler slackNotificationHandler = mock(NotificationHandler.class);
    private final NotificationSender slackNotificationSender = mock(NotificationSender.class);

    private final NotificationJobStatusApplicationEventListener notificationJobStatusApplicationEventListener =
        new NotificationJobStatusApplicationEventListener(
            jobExecutionCounter, jobService, notificationHandlerContexts, notificationHandlerRegistry,
            notificationSenderRegistry, notificationService);

    @BeforeEach
    void beforeEach() {
        job.setId(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(job);
        when(notificationHandlerContexts.builder(any(Job.class), any(Job.Status.class), any()))
            .thenAnswer(invocation -> new NotificationHandlerContext.Builder()
                .eventType(invocation.getArgument(2))
                .jobId(JOB_ID));
        when(notificationSenderRegistry.getNotificationSender(Notification.Type.EMAIL))
            .thenReturn((NotificationSender) emailNotificationSender);
        when(notificationSenderRegistry.getNotificationSender(Notification.Type.SLACK))
            .thenReturn((NotificationSender) slackNotificationSender);
        when(emailNotificationSender.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(slackNotificationSender.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void testOtherApplicationEventsAreIgnored() {
        notificationJobStatusApplicationEventListener.onApplicationEvent(mock(ApplicationEvent.class));

        verifyNoInteractions(jobService, jobExecutionCounter, notificationService, notificationSenderRegistry);
    }

    @Test
    void testJobStatusEventIsCountedByTheJobExecutionCounter() {
        when(notificationService.getNotifications(NotificationEvent.Type.JOB_COMPLETED)).thenReturn(List.of());

        JobStatusApplicationEvent jobStatusApplicationEvent = new JobStatusApplicationEvent(
            JOB_ID, Job.Status.COMPLETED);

        notificationJobStatusApplicationEventListener.onApplicationEvent(jobStatusApplicationEvent);

        verify(jobExecutionCounter).process(jobStatusApplicationEvent, job);
    }

    @Test
    void testJobStatusEventIsHandledWithoutAJobExecutionCounter() {
        NotificationJobStatusApplicationEventListener listenerWithoutCounter =
            new NotificationJobStatusApplicationEventListener(
                null, jobService, notificationHandlerContexts, notificationHandlerRegistry, notificationSenderRegistry,
                notificationService);

        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);

        when(notificationService.getNotifications(NotificationEvent.Type.JOB_FAILED))
            .thenReturn(List.of(emailNotification));
        when(notificationHandlerRegistry.getNotificationHandler(
            NotificationEvent.Type.JOB_FAILED, Notification.Type.EMAIL)).thenReturn(emailNotificationHandler);

        listenerWithoutCounter.onApplicationEvent(new JobStatusApplicationEvent(JOB_ID, Job.Status.FAILED));

        verify(emailNotificationSender).send(any(), any(), any());
    }

    @Test
    void testNoContextIsBuiltWhenNoNotificationIsSubscribedToTheStatus() {
        when(notificationService.getNotifications(NotificationEvent.Type.JOB_COMPLETED)).thenReturn(List.of());

        notificationJobStatusApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        verifyNoInteractions(notificationHandlerContexts, notificationSenderRegistry, notificationHandlerRegistry);
    }

    @Test
    void testEverySubscribedNotificationIsSentWithTheEventTypeOfTheJobStatus() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);
        Notification slackNotification = createNotification(2L, Notification.Type.SLACK);

        when(notificationService.getNotifications(NotificationEvent.Type.JOB_FAILED))
            .thenReturn(List.of(emailNotification, slackNotification));
        when(notificationHandlerRegistry.getNotificationHandler(
            NotificationEvent.Type.JOB_FAILED, Notification.Type.EMAIL)).thenReturn(emailNotificationHandler);
        when(notificationHandlerRegistry.getNotificationHandler(
            NotificationEvent.Type.JOB_FAILED, Notification.Type.SLACK)).thenReturn(slackNotificationHandler);

        notificationJobStatusApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.FAILED));

        verify(notificationHandlerContexts).builder(job, Job.Status.FAILED, NotificationEvent.Type.JOB_FAILED);

        ArgumentCaptor<NotificationHandlerContext> emailNotificationHandlerContextArgumentCaptor =
            ArgumentCaptor.forClass(NotificationHandlerContext.class);
        ArgumentCaptor<NotificationHandlerContext> slackNotificationHandlerContextArgumentCaptor =
            ArgumentCaptor.forClass(NotificationHandlerContext.class);

        verify(emailNotificationSender).send(
            eq(emailNotification),
            eq(emailNotificationHandler),
            emailNotificationHandlerContextArgumentCaptor.capture());
        verify(slackNotificationSender).send(
            eq(slackNotification),
            eq(slackNotificationHandler),
            slackNotificationHandlerContextArgumentCaptor.capture());

        NotificationHandlerContext emailNotificationHandlerContext =
            emailNotificationHandlerContextArgumentCaptor.getValue();

        assertThat(emailNotificationHandlerContext)
            .isSameAs(slackNotificationHandlerContextArgumentCaptor.getValue());
        assertThat(emailNotificationHandlerContext.getEventType())
            .isEqualTo(NotificationEvent.Type.JOB_FAILED);
        assertThat(emailNotificationHandlerContext.getJobId())
            .isEqualTo(JOB_ID);
    }

    @Test
    void testSuspendedJobStatusEventIsSentAsAJobStoppedNotification() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);

        when(notificationService.getNotifications(NotificationEvent.Type.JOB_STOPPED))
            .thenReturn(List.of(emailNotification));
        when(notificationHandlerRegistry.getNotificationHandler(
            NotificationEvent.Type.JOB_STOPPED, Notification.Type.EMAIL)).thenReturn(emailNotificationHandler);

        notificationJobStatusApplicationEventListener.onApplicationEvent(JobStatusApplicationEvent.suspended(JOB_ID));

        verify(emailNotificationSender).send(
            eq(emailNotification),
            eq(emailNotificationHandler), any());
    }

    @Test
    void testPaymentRequiredJobStatusEventIsSentAsAJobPaymentRequiredNotification() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);

        when(notificationService.getNotifications(NotificationEvent.Type.JOB_PAYMENT_REQUIRED))
            .thenReturn(List.of(emailNotification));
        when(notificationHandlerRegistry.getNotificationHandler(
            NotificationEvent.Type.JOB_PAYMENT_REQUIRED, Notification.Type.EMAIL))
                .thenReturn(emailNotificationHandler);

        notificationJobStatusApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.PAYMENT_REQUIRED));

        verify(emailNotificationSender).send(
            eq(emailNotification),
            eq(emailNotificationHandler), any());
    }

    @Test
    void testNotificationWithoutASenderIsSkippedAndTheOthersAreSent() {
        Notification webhookNotification = createNotification(1L, Notification.Type.WEBHOOK);
        Notification emailNotification = createNotification(2L, Notification.Type.EMAIL);

        when(notificationService.getNotifications(NotificationEvent.Type.JOB_COMPLETED))
            .thenReturn(List.of(webhookNotification, emailNotification));
        when(notificationHandlerRegistry.getNotificationHandler(
            NotificationEvent.Type.JOB_COMPLETED, Notification.Type.EMAIL)).thenReturn(emailNotificationHandler);

        notificationJobStatusApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        verify(emailNotificationSender).send(
            eq(emailNotification),
            eq(emailNotificationHandler), any());
    }

    @Test
    void testNotificationWithoutAHandlerIsSkippedAndTheOthersAreSent() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);
        Notification slackNotification = createNotification(2L, Notification.Type.SLACK);

        when(notificationService.getNotifications(NotificationEvent.Type.JOB_COMPLETED))
            .thenReturn(List.of(emailNotification, slackNotification));
        when(notificationHandlerRegistry.getNotificationHandler(
            NotificationEvent.Type.JOB_COMPLETED, Notification.Type.SLACK)).thenReturn(slackNotificationHandler);

        notificationJobStatusApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        verify(emailNotificationSender, never()).send(any(), any(), any());
        verify(slackNotificationSender).send(
            eq(slackNotification),
            eq(slackNotificationHandler), any());
    }

    @Test
    void testSenderFailureDoesNotStopTheRemainingNotifications() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);
        Notification slackNotification = createNotification(2L, Notification.Type.SLACK);

        when(notificationService.getNotifications(NotificationEvent.Type.JOB_FAILED))
            .thenReturn(List.of(emailNotification, slackNotification));
        when(notificationHandlerRegistry.getNotificationHandler(
            NotificationEvent.Type.JOB_FAILED, Notification.Type.EMAIL)).thenReturn(emailNotificationHandler);
        when(notificationHandlerRegistry.getNotificationHandler(
            NotificationEvent.Type.JOB_FAILED, Notification.Type.SLACK)).thenReturn(slackNotificationHandler);
        when(emailNotificationSender.send(any(), any(), any())).thenThrow(new IllegalStateException("SMTP down"));

        notificationJobStatusApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.FAILED));

        verify(slackNotificationSender).send(
            eq(slackNotification),
            eq(slackNotificationHandler), any());
    }

    private static Notification createNotification(long id, Notification.Type type) {
        Notification notification = new Notification();

        notification.setId(id);
        notification.setType(type);

        return notification;
    }
}
