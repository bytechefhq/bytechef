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

package com.bytechef.platform.notification.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.notification.delivery.SlackNotificationClient;
import com.bytechef.platform.notification.delivery.WebhookDeliveryException;
import com.bytechef.platform.notification.domain.Notification;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SlackNotificationSenderTest {

    private final SlackNotificationClient slackNotificationClient = mock(SlackNotificationClient.class);
    private final SlackNotificationHandler slackNotificationHandler = mock(SlackNotificationHandler.class);
    private final SlackNotificationSender slackNotificationSender = new SlackNotificationSender(
        slackNotificationClient);

    @Test
    void testGetType() {
        assertThat(slackNotificationSender.getType()).isEqualTo(Notification.Type.SLACK);
    }

    @Test
    void testSendPostsHandlerTextToConfiguredWebhookUrl() {
        NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder().build();

        when(slackNotificationHandler.getText(notificationHandlerContext)).thenReturn("Approval pending");

        slackNotificationSender.send(
            createNotification(Map.of("slackWebhookUrl", "https://hooks.slack.com/services/T/B/X")),
            slackNotificationHandler, notificationHandlerContext);

        verify(slackNotificationClient).send("https://hooks.slack.com/services/T/B/X", "Approval pending");
    }

    @Test
    void testSendThrowsWhenWebhookUrlMissing() {
        NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder().build();

        assertThatThrownBy(
            () -> slackNotificationSender.send(
                createNotification(Map.of()), slackNotificationHandler, notificationHandlerContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no Slack webhook URL");

        verify(slackNotificationClient, never()).send(anyString(), anyString());
    }

    @Test
    void testSendThrowsWhenWebhookUrlBlank() {
        NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder().build();

        assertThatThrownBy(
            () -> slackNotificationSender.send(
                createNotification(Map.of("slackWebhookUrl", "  ")), slackNotificationHandler,
                notificationHandlerContext))
                    .isInstanceOf(IllegalStateException.class);

        verify(slackNotificationClient, never()).send(anyString(), anyString());
    }

    @Test
    void testSendPropagatesDeliveryFailure() {
        NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder().build();

        when(slackNotificationHandler.getText(any())).thenReturn("Approval pending");

        doThrow(new WebhookDeliveryException("boom", 500))
            .when(slackNotificationClient)
            .send(anyString(), anyString());

        assertThatThrownBy(
            () -> slackNotificationSender.send(
                createNotification(Map.of("slackWebhookUrl", "https://hooks.slack.com/services/T/B/X")),
                slackNotificationHandler, notificationHandlerContext))
                    .isInstanceOf(WebhookDeliveryException.class);
    }

    private static Notification createNotification(Map<String, Object> settings) {
        Notification notification = new Notification();

        notification.setId(1L);
        notification.setType(Notification.Type.SLACK);
        notification.setSettings(settings);

        return notification;
    }
}
