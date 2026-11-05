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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.coordinator.monitor.ApprovalNotificationDispatcher.Result;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationHandler;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSender;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings({
    "rawtypes", "unchecked"
})
class ApprovalNotificationDispatcherTest {

    private static final NotificationEvent.Type EVENT_TYPE = NotificationEvent.Type.JOB_APPROVAL_EXPIRING;
    private static final long JOB_ID = 42L;

    private final NotificationHandler emailNotificationHandler = mock(NotificationHandler.class);
    private final NotificationSender emailNotificationSender = mock(NotificationSender.class);
    private final NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder()
        .eventType(EVENT_TYPE)
        .jobId(JOB_ID)
        .build();
    private final NotificationHandlerRegistry notificationHandlerRegistry = mock(NotificationHandlerRegistry.class);
    private final NotificationSenderRegistry notificationSenderRegistry = mock(NotificationSenderRegistry.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final NotificationHandler slackNotificationHandler = mock(NotificationHandler.class);
    private final NotificationSender slackNotificationSender = mock(NotificationSender.class);

    private final ApprovalNotificationDispatcher approvalNotificationDispatcher = new ApprovalNotificationDispatcher(
        notificationHandlerRegistry, notificationSenderRegistry, notificationService);

    @BeforeEach
    void beforeEach() {
        when(notificationSenderRegistry.getNotificationSender(Notification.Type.EMAIL))
            .thenReturn((NotificationSender) emailNotificationSender);
        when(notificationSenderRegistry.getNotificationSender(Notification.Type.SLACK))
            .thenReturn((NotificationSender) slackNotificationSender);
        when(notificationHandlerRegistry.getNotificationHandler(EVENT_TYPE, Notification.Type.EMAIL))
            .thenReturn(emailNotificationHandler);
        when(notificationHandlerRegistry.getNotificationHandler(EVENT_TYPE, Notification.Type.SLACK))
            .thenReturn(slackNotificationHandler);
    }

    @Test
    void testDispatchWithoutSubscribedNotificationsReturnsNoneConfigured() {
        when(notificationService.getNotifications(EVENT_TYPE)).thenReturn(List.of());

        Result result = dispatch();

        assertThat(result).isEqualTo(Result.NONE_CONFIGURED);

        verifyNoInteractions(notificationSenderRegistry, notificationHandlerRegistry);
    }

    @Test
    void testDispatchReturnsDeliveredWhenTheDeliverySucceeds() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);

        when(notificationService.getNotifications(EVENT_TYPE)).thenReturn(List.of(emailNotification));
        when(emailNotificationSender.send(emailNotification, emailNotificationHandler, notificationHandlerContext))
            .thenReturn(CompletableFuture.completedFuture(null));

        Result result = dispatch();

        assertThat(result).isEqualTo(Result.DELIVERED);

        verify(emailNotificationSender).send(emailNotification, emailNotificationHandler, notificationHandlerContext);
    }

    @Test
    void testDispatchReturnsDeliveredWhenAnyDeliverySucceeds() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);
        Notification slackNotification = createNotification(2L, Notification.Type.SLACK);

        when(notificationService.getNotifications(EVENT_TYPE))
            .thenReturn(List.of(emailNotification, slackNotification));
        when(emailNotificationSender.send(emailNotification, emailNotificationHandler, notificationHandlerContext))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("SMTP down")));
        when(slackNotificationSender.send(slackNotification, slackNotificationHandler, notificationHandlerContext))
            .thenReturn(CompletableFuture.completedFuture(null));

        Result result = dispatch();

        assertThat(result).isEqualTo(Result.DELIVERED);
    }

    @Test
    void testDispatchReturnsFailedWhenEveryDeliveryFails() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);
        Notification slackNotification = createNotification(2L, Notification.Type.SLACK);

        when(notificationService.getNotifications(EVENT_TYPE))
            .thenReturn(List.of(emailNotification, slackNotification));
        when(emailNotificationSender.send(emailNotification, emailNotificationHandler, notificationHandlerContext))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("SMTP down")));
        when(slackNotificationSender.send(slackNotification, slackNotificationHandler, notificationHandlerContext))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Slack down")));

        Result result = dispatch();

        assertThat(result).isEqualTo(Result.FAILED);
    }

    @Test
    void testDispatchReturnsFailedWhenNoSenderIsRegisteredForTheNotificationType() {
        Notification webhookNotification = createNotification(1L, Notification.Type.WEBHOOK);

        when(notificationService.getNotifications(EVENT_TYPE)).thenReturn(List.of(webhookNotification));

        Result result = dispatch();

        assertThat(result).isEqualTo(Result.FAILED);
    }

    @Test
    void testDispatchReturnsFailedWhenNoHandlerIsRegisteredForTheEventType() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);

        when(notificationService.getNotifications(EVENT_TYPE)).thenReturn(List.of(emailNotification));
        when(notificationHandlerRegistry.getNotificationHandler(EVENT_TYPE, Notification.Type.EMAIL))
            .thenReturn(null);

        Result result = dispatch();

        assertThat(result).isEqualTo(Result.FAILED);

        verify(emailNotificationSender, never()).send(any(), any(), any());
    }

    @Test
    void testDispatchSkipsANotificationWithoutAHandlerAndDeliversTheOthers() {
        Notification webhookNotification = createNotification(1L, Notification.Type.WEBHOOK);
        Notification emailNotification = createNotification(2L, Notification.Type.EMAIL);

        when(notificationService.getNotifications(EVENT_TYPE))
            .thenReturn(List.of(webhookNotification, emailNotification));
        when(emailNotificationSender.send(emailNotification, emailNotificationHandler, notificationHandlerContext))
            .thenReturn(CompletableFuture.completedFuture(null));

        Result result = dispatch();

        assertThat(result).isEqualTo(Result.DELIVERED);
    }

    @Test
    void testDispatchReturnsFailedWhenTheOnlySenderThrows() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);

        when(notificationService.getNotifications(EVENT_TYPE)).thenReturn(List.of(emailNotification));
        when(emailNotificationSender.send(emailNotification, emailNotificationHandler, notificationHandlerContext))
            .thenThrow(new IllegalStateException("cannot build message"));

        Result result = dispatch();

        assertThat(result).isEqualTo(Result.FAILED);
    }

    @Test
    void testDispatchContinuesPastASenderThatThrows() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);
        Notification slackNotification = createNotification(2L, Notification.Type.SLACK);

        when(notificationService.getNotifications(EVENT_TYPE))
            .thenReturn(List.of(emailNotification, slackNotification));
        when(emailNotificationSender.send(emailNotification, emailNotificationHandler, notificationHandlerContext))
            .thenThrow(new IllegalStateException("cannot build message"));
        when(slackNotificationSender.send(slackNotification, slackNotificationHandler, notificationHandlerContext))
            .thenReturn(CompletableFuture.completedFuture(null));

        Result result = dispatch();

        assertThat(result).isEqualTo(Result.DELIVERED);
    }

    @Test
    void testDispatchCountsAPendingDeliveryAsNotDeliveredWhenTheDeadlinePasses() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);

        when(notificationService.getNotifications(EVENT_TYPE)).thenReturn(List.of(emailNotification));
        when(emailNotificationSender.send(emailNotification, emailNotificationHandler, notificationHandlerContext))
            .thenReturn(new CompletableFuture<>());

        Result result = approvalNotificationDispatcher.dispatch(
            EVENT_TYPE, notificationHandlerContext, System.nanoTime());

        assertThat(result).isEqualTo(Result.FAILED);
    }

    @Test
    void testDispatchReturnsDeliveredWhenOneDeliverySucceededBeforeTheDeadlinePassed() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);
        Notification slackNotification = createNotification(2L, Notification.Type.SLACK);

        when(notificationService.getNotifications(EVENT_TYPE))
            .thenReturn(List.of(emailNotification, slackNotification));
        when(emailNotificationSender.send(emailNotification, emailNotificationHandler, notificationHandlerContext))
            .thenReturn(new CompletableFuture<>());
        when(slackNotificationSender.send(slackNotification, slackNotificationHandler, notificationHandlerContext))
            .thenReturn(CompletableFuture.completedFuture(null));

        Result result = approvalNotificationDispatcher.dispatch(
            EVENT_TYPE, notificationHandlerContext, System.nanoTime());

        assertThat(result).isEqualTo(Result.DELIVERED);
    }

    @Test
    void testDispatchWaitsForADeliveryThatCompletesBeforeTheDeadline() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);

        CompletableFuture<Void> deliveryFuture = new CompletableFuture<>();

        deliveryFuture.completeOnTimeout(null, 50, TimeUnit.MILLISECONDS);

        when(notificationService.getNotifications(EVENT_TYPE)).thenReturn(List.of(emailNotification));
        when(emailNotificationSender.send(emailNotification, emailNotificationHandler, notificationHandlerContext))
            .thenReturn(deliveryFuture);

        Result result = dispatch();

        assertThat(result).isEqualTo(Result.DELIVERED);
    }

    @Test
    void testDispatchReturnsFailedAndKeepsTheInterruptFlagWhenInterruptedWhileWaiting() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);

        when(notificationService.getNotifications(EVENT_TYPE)).thenReturn(List.of(emailNotification));
        when(emailNotificationSender.send(emailNotification, emailNotificationHandler, notificationHandlerContext))
            .thenReturn(new CompletableFuture<>());

        Thread currentThread = Thread.currentThread();

        currentThread.interrupt();

        try {
            Result result = dispatch();

            assertThat(result).isEqualTo(Result.FAILED);
            assertThat(currentThread.isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private Result dispatch() {
        Duration deliveryTimeout = Duration.ofSeconds(10);

        long deliveryDeadlineNanos = System.nanoTime() + deliveryTimeout.toNanos();

        return approvalNotificationDispatcher.dispatch(EVENT_TYPE, notificationHandlerContext, deliveryDeadlineNanos);
    }

    private static Notification createNotification(long id, Notification.Type type) {
        Notification notification = new Notification();

        notification.setId(id);
        notification.setType(type);

        return notification;
    }
}
