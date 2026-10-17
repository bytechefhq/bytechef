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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
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

class NotificationJobStatusApplicationEventListenerTest {

    private static final long JOB_ID = 7L;

    private final JobService jobService = mock(JobService.class);
    private final NotificationHandlerRegistry notificationHandlerRegistry = mock(NotificationHandlerRegistry.class);
    private final NotificationSenderRegistry notificationSenderRegistry = mock(NotificationSenderRegistry.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final NotificationJobStatusApplicationEventListener notificationJobStatusApplicationEventListener =
        new NotificationJobStatusApplicationEventListener(
            null, jobService, notificationHandlerRegistry, notificationSenderRegistry, notificationService);

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testFailedNotificationSendDoesNotStopTheRemainingNotifications() {
        Notification emailNotification = createNotification(1L, Notification.Type.EMAIL);
        Notification webhookNotification = createNotification(2L, Notification.Type.WEBHOOK);

        NotificationHandler notificationHandler = mock(NotificationHandler.class);
        NotificationSender emailNotificationSender = mock(NotificationSender.class);
        NotificationSender webhookNotificationSender = mock(NotificationSender.class);

        when(jobService.getJob(JOB_ID)).thenReturn(new Job(JOB_ID));
        when(notificationService.getNotifications(NotificationEvent.Type.JOB_COMPLETED))
            .thenReturn(List.of(emailNotification, webhookNotification));
        when(notificationSenderRegistry.getNotificationSender(Notification.Type.EMAIL))
            .thenReturn(emailNotificationSender);
        when(notificationSenderRegistry.getNotificationSender(Notification.Type.WEBHOOK))
            .thenReturn(webhookNotificationSender);
        when(notificationHandlerRegistry.getNotificationHandler(eq(NotificationEvent.Type.JOB_COMPLETED), any()))
            .thenReturn(notificationHandler);

        doThrow(new IllegalStateException("SMTP unavailable")).when(emailNotificationSender)
            .send(eq(emailNotification), eq(notificationHandler), any(NotificationHandlerContext.class));

        assertThatCode(
            () -> notificationJobStatusApplicationEventListener.onApplicationEvent(
                new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED)))
                    .doesNotThrowAnyException();

        verify(webhookNotificationSender).send(
            eq(webhookNotification), eq(notificationHandler), any(NotificationHandlerContext.class));
    }

    private static Notification createNotification(long id, Notification.Type type) {
        Notification notification = new Notification();

        notification.setId(id);
        notification.setType(type);

        return notification;
    }
}
