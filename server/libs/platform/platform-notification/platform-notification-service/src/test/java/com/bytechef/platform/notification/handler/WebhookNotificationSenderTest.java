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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.notification.delivery.WebhookDeliveryException;
import com.bytechef.platform.notification.delivery.WebhookDeliveryRequest;
import com.bytechef.platform.notification.delivery.WebhookNotificationClient;
import com.bytechef.platform.notification.domain.Notification;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Async;

/**
 * @author Ivica Cardic
 */
class WebhookNotificationSenderTest {

    private final NotificationHandlerContext notificationHandlerContext =
        new NotificationHandlerContext.Builder().build();
    private final WebhookNotificationClient webhookNotificationClient = mock(WebhookNotificationClient.class);
    private final WebhookNotificationHandler webhookNotificationHandler = mock(WebhookNotificationHandler.class);
    private final WebhookNotificationSender webhookNotificationSender = new WebhookNotificationSender(
        webhookNotificationClient);

    @Test
    void testSendDeliversPayloadWithSecret() {
        when(webhookNotificationHandler.getPayload(notificationHandlerContext)).thenReturn("{\"a\":1}");

        webhookNotificationSender.send(
            createNotification(Map.of("webhook", "https://example.com/hook", "webhookSecret", "secret")),
            webhookNotificationHandler, notificationHandlerContext);

        ArgumentCaptor<WebhookDeliveryRequest> webhookDeliveryRequestArgumentCaptor =
            ArgumentCaptor.forClass(WebhookDeliveryRequest.class);

        verify(webhookNotificationClient).deliver(webhookDeliveryRequestArgumentCaptor.capture());

        WebhookDeliveryRequest webhookDeliveryRequest = webhookDeliveryRequestArgumentCaptor.getValue();

        assertThat(webhookDeliveryRequest.url()).isEqualTo("https://example.com/hook");
        assertThat(webhookDeliveryRequest.payloadJson()).isEqualTo("{\"a\":1}");
        assertThat(webhookDeliveryRequest.secret()).isEqualTo("secret");
    }

    @Test
    void testSendThrowsWhenWebhookUrlMissing() {
        assertThatThrownBy(
            () -> webhookNotificationSender.send(
                createNotification(Map.of()), webhookNotificationHandler, notificationHandlerContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no webhook URL");

        verify(webhookNotificationClient, never()).deliver(any());
    }

    @Test
    void testSendThrowsWhenWebhookUrlBlank() {
        assertThatThrownBy(
            () -> webhookNotificationSender.send(
                createNotification(Map.of("webhook", "")), webhookNotificationHandler, notificationHandlerContext))
                    .isInstanceOf(IllegalStateException.class);

        verify(webhookNotificationClient, never()).deliver(any());
    }

    @Test
    void testSendPropagatesDeliveryFailure() {
        when(webhookNotificationHandler.getPayload(notificationHandlerContext)).thenReturn("{}");

        doThrow(new WebhookDeliveryException("boom", 500))
            .when(webhookNotificationClient)
            .deliver(any());

        assertThatThrownBy(
            () -> webhookNotificationSender.send(
                createNotification(Map.of("webhook", "https://example.com/hook")), webhookNotificationHandler,
                notificationHandlerContext))
                    .isInstanceOf(WebhookDeliveryException.class);
    }

    @Test
    void testSendIsSynchronous() throws NoSuchMethodException {
        assertThat(
            WebhookNotificationSender.class
                .getMethod(
                    "send", Notification.class, WebhookNotificationHandler.class, NotificationHandlerContext.class)
                .isAnnotationPresent(Async.class))
                    .isFalse();
    }

    private static Notification createNotification(Map<String, Object> settings) {
        Notification notification = new Notification();

        notification.setId(1L);
        notification.setType(Notification.Type.WEBHOOK);
        notification.setSettings(settings);

        return notification;
    }
}
