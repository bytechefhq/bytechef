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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationHandler;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSender;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings({
    "rawtypes", "unchecked"
})
class ApprovalNotificationDispatcherTest {

    private static final NotificationEvent.Type EVENT_TYPE = NotificationEvent.Type.JOB_APPROVAL_EXPIRING;

    private final NotificationHandler notificationHandler = mock(NotificationHandler.class);
    private final NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder()
        .eventType(EVENT_TYPE)
        .jobId(1L)
        .build();
    private final NotificationHandlerRegistry notificationHandlerRegistry = mock(NotificationHandlerRegistry.class);
    private final NotificationSender notificationSender = mock(NotificationSender.class);
    private final NotificationSenderRegistry notificationSenderRegistry = mock(NotificationSenderRegistry.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ApprovalNotificationDispatcher approvalNotificationDispatcher = new ApprovalNotificationDispatcher(
        notificationHandlerRegistry, notificationSenderRegistry, notificationService);

    @Test
    void testNoConfiguredNotificationIsReportedAsNoneConfigured() {
        when(notificationService.getNotifications(EVENT_TYPE)).thenReturn(List.of());

        assertThat(approvalNotificationDispatcher.dispatch(EVENT_TYPE, notificationHandlerContext))
            .isEqualTo(ApprovalNotificationDispatcher.Result.NONE_CONFIGURED);
    }

    @Test
    void testSuccessfulSendCountsAsDelivered() {
        Notification notification = createNotification(1L, Notification.Type.EMAIL);

        when(notificationService.getNotifications(EVENT_TYPE)).thenReturn(List.of(notification));
        when(notificationSenderRegistry.getNotificationSender(Notification.Type.EMAIL)).thenReturn(notificationSender);
        when(notificationHandlerRegistry.getNotificationHandler(EVENT_TYPE, Notification.Type.EMAIL))
            .thenReturn(notificationHandler);

        assertThat(approvalNotificationDispatcher.dispatch(EVENT_TYPE, notificationHandlerContext))
            .isEqualTo(ApprovalNotificationDispatcher.Result.DELIVERED);

        verify(notificationSender).send(notification, notificationHandler, notificationHandlerContext);
    }

    @Test
    void testMissingSenderCountsAsFailedAttempt() {
        when(notificationService.getNotifications(EVENT_TYPE))
            .thenReturn(List.of(createNotification(1L, Notification.Type.SLACK)));
        when(notificationHandlerRegistry.getNotificationHandler(EVENT_TYPE, Notification.Type.SLACK))
            .thenReturn(notificationHandler);

        assertThat(approvalNotificationDispatcher.dispatch(EVENT_TYPE, notificationHandlerContext))
            .isEqualTo(ApprovalNotificationDispatcher.Result.FAILED);
    }

    @Test
    void testMissingHandlerCountsAsFailedAttempt() {
        when(notificationService.getNotifications(EVENT_TYPE))
            .thenReturn(List.of(createNotification(1L, Notification.Type.EMAIL)));
        when(notificationSenderRegistry.getNotificationSender(Notification.Type.EMAIL)).thenReturn(notificationSender);

        assertThat(approvalNotificationDispatcher.dispatch(EVENT_TYPE, notificationHandlerContext))
            .isEqualTo(ApprovalNotificationDispatcher.Result.FAILED);

        verify(notificationSender, never()).send(any(), any(), any());
    }

    @Test
    void testMissingSenderAlongsideDeliveredNotificationCountsAsDelivered() {
        Notification slackNotification = createNotification(1L, Notification.Type.SLACK);
        Notification emailNotification = createNotification(2L, Notification.Type.EMAIL);

        when(notificationService.getNotifications(EVENT_TYPE))
            .thenReturn(List.of(slackNotification, emailNotification));
        when(notificationSenderRegistry.getNotificationSender(Notification.Type.EMAIL)).thenReturn(notificationSender);
        when(notificationHandlerRegistry.getNotificationHandler(eq(EVENT_TYPE), any()))
            .thenReturn(notificationHandler);

        assertThat(approvalNotificationDispatcher.dispatch(EVENT_TYPE, notificationHandlerContext))
            .isEqualTo(ApprovalNotificationDispatcher.Result.DELIVERED);

        verify(notificationSender).send(emailNotification, notificationHandler, notificationHandlerContext);
    }

    @Test
    void testEveryAttemptFailingCountsAsFailed() {
        when(notificationService.getNotifications(EVENT_TYPE))
            .thenReturn(List.of(createNotification(1L, Notification.Type.EMAIL)));
        when(notificationSenderRegistry.getNotificationSender(Notification.Type.EMAIL)).thenReturn(notificationSender);
        when(notificationHandlerRegistry.getNotificationHandler(EVENT_TYPE, Notification.Type.EMAIL))
            .thenReturn(notificationHandler);

        doThrow(new IllegalStateException("boom"))
            .when(notificationSender)
            .send(any(), any(), any());

        assertThat(approvalNotificationDispatcher.dispatch(EVENT_TYPE, notificationHandlerContext))
            .isEqualTo(ApprovalNotificationDispatcher.Result.FAILED);
    }

    private static Notification createNotification(long id, Notification.Type type) {
        Notification notification = new Notification();

        notification.setId(id);
        notification.setType(type);

        return notification;
    }
}
