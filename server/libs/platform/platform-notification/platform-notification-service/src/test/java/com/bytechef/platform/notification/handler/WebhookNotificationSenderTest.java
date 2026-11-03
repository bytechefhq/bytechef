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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.notification.delivery.WebhookDelivery;
import com.bytechef.platform.notification.delivery.WebhookDeliveryException;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome.PayloadResult;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome.Status;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@Timeout(5)
@ExtendWith(ObjectMapperSetupExtension.class)
class WebhookNotificationSenderTest {

    private final NotificationHandlerContext notificationHandlerContext =
        new NotificationHandlerContext.Builder().build();
    private final WebhookDeliveryService webhookDeliveryService = mock(WebhookDeliveryService.class);
    private final WebhookNotificationHandler webhookNotificationHandler = mock(WebhookNotificationHandler.class);
    private final WebhookNotificationSender webhookNotificationSender = new WebhookNotificationSender(
        webhookDeliveryService);

    @Test
    void testSendReadsSecretFromSecretSettings() {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{\"a\":1}");
        when(webhookDeliveryService.submit(any())).thenReturn(CompletableFuture.completedFuture(sentOutcome()));

        Notification notification = createNotification(Map.of("webhook", "https://example.com/hook"));

        notification.setSecretSettings(Map.of("webhookSecret", "from-secret"));

        webhookNotificationSender.send(notification, webhookNotificationHandler, notificationHandlerContext);

        ArgumentCaptor<WebhookDelivery> webhookDeliveryArgumentCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(webhookDeliveryService).submit(webhookDeliveryArgumentCaptor.capture());

        assertThat(webhookDeliveryArgumentCaptor.getValue()
            .secret()).isEqualTo("from-secret");
    }

    @Test
    void testSendSubmitsSignedDeliveryWithoutRetry() {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{\"a\":1}");
        when(webhookDeliveryService.submit(any())).thenReturn(CompletableFuture.completedFuture(sentOutcome()));

        CompletableFuture<Void> future = webhookNotificationSender.send(
            createNotification(Map.of("webhook", "https://example.com/hook", "webhookSecret", "secret")),
            webhookNotificationHandler, notificationHandlerContext);

        ArgumentCaptor<WebhookDelivery> webhookDeliveryArgumentCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(webhookDeliveryService).submit(webhookDeliveryArgumentCaptor.capture());

        WebhookDelivery webhookDelivery = webhookDeliveryArgumentCaptor.getValue();

        assertThat(webhookDelivery.source()).isEqualTo("notification");
        assertThat(webhookDelivery.description()).isEqualTo("notification 1 'Alerts'");
        assertThat(webhookDelivery.url()).isEqualTo("https://example.com/hook");
        assertThat(webhookDelivery.secret()).isEqualTo("secret");
        assertThat(webhookDelivery.eventType()).isEqualTo("job.status");
        assertThat(webhookDelivery.validateUrl()).isTrue();
        assertThat(webhookDelivery.payloads()).hasSize(1);
        assertThat(webhookDelivery.payloads()
            .getFirst()
            .json()).isEqualTo("{\"a\":1}");
        assertThat(webhookDelivery.retry()
            .maxRetries()).isZero();
        assertThat(future).isCompleted();
    }

    @Test
    void testSendPassesTimestampAndMintsDeliveryId() {
        Instant timestamp = Instant.parse("2026-11-03T10:00:00Z");
        NotificationHandlerContext context = new NotificationHandlerContext.Builder().deliveryId("d-1")
            .timestamp(timestamp)
            .build();

        when(webhookNotificationHandler.getPayload(any())).thenReturn("{\"a\":1}");
        when(webhookDeliveryService.submit(any())).thenReturn(CompletableFuture.completedFuture(sentOutcome()));

        webhookNotificationSender.send(
            createNotification(Map.of("webhook", "https://example.com/hook")), webhookNotificationHandler, context);

        ArgumentCaptor<WebhookDelivery> webhookDeliveryArgumentCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(webhookDeliveryService).submit(webhookDeliveryArgumentCaptor.capture());

        WebhookDelivery webhookDelivery = webhookDeliveryArgumentCaptor.getValue();

        assertThat(webhookDelivery.deliveryId()).isNotEqualTo("d-1");
        assertThat(webhookDelivery.timestamp()).isEqualTo(timestamp);
    }

    @Test
    void testSendGivesEachDeliveryItsOwnDeliveryIdMatchingThePayload() {
        NotificationHandlerContext context = new NotificationHandlerContext.Builder().deliveryId("d-1")
            .build();

        when(webhookNotificationHandler.getPayload(any())).thenAnswer(
            invocation -> "{\"deliveryId\":\"" + ((NotificationHandlerContext) invocation.getArgument(0))
                .getDeliveryId() + "\"}");
        when(webhookDeliveryService.submit(any())).thenReturn(CompletableFuture.completedFuture(sentOutcome()));

        Notification notification = createNotification(Map.of("webhook", "https://example.com/hook"));

        webhookNotificationSender.send(notification, webhookNotificationHandler, context);
        webhookNotificationSender.send(notification, webhookNotificationHandler, context);

        ArgumentCaptor<WebhookDelivery> webhookDeliveryArgumentCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(webhookDeliveryService, times(2)).submit(webhookDeliveryArgumentCaptor.capture());

        List<WebhookDelivery> webhookDeliveries = webhookDeliveryArgumentCaptor.getAllValues();

        assertThat(webhookDeliveries.get(0)
            .deliveryId())
                .isNotEqualTo(webhookDeliveries.get(1)
                    .deliveryId());

        for (WebhookDelivery webhookDelivery : webhookDeliveries) {
            assertThat(webhookDelivery.deliveryId()).isNotEqualTo("d-1");
            assertThat(webhookDelivery.payloads()
                .getFirst()
                .json()).isEqualTo("{\"deliveryId\":\"" + webhookDelivery.deliveryId() + "\"}");
        }
    }

    @Test
    void testSendRendersTemplateWithDeliveryId() {
        when(webhookDeliveryService.submit(any())).thenReturn(CompletableFuture.completedFuture(sentOutcome()));

        Notification notification = createNotification(
            Map.of("webhook", "https://example.com/hook", "webhookBodyTemplate", "{\"d\":\"{{deliveryId}}\"}"));

        webhookNotificationSender.send(notification, webhookNotificationHandler, notificationHandlerContext);

        ArgumentCaptor<WebhookDelivery> webhookDeliveryArgumentCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(webhookDeliveryService).submit(webhookDeliveryArgumentCaptor.capture());

        WebhookDelivery webhookDelivery = webhookDeliveryArgumentCaptor.getValue();

        assertThat(webhookDelivery.payloads()
            .getFirst()
            .json()).isEqualTo("{\"d\":\"" + webhookDelivery.deliveryId() + "\"}");
        verify(webhookNotificationHandler, never()).getPayload(any());
    }

    @Test
    void testSendPassesHeadersInOrder() {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{}");
        when(webhookDeliveryService.submit(any())).thenReturn(CompletableFuture.completedFuture(sentOutcome()));

        Notification notification = createNotification(Map.of("webhook", "https://example.com/hook"));

        notification.setSecretSettings(
            Map.of(
                "webhookHeaders",
                List.of(
                    Map.of("name", "Z-First", "value", "1"), Map.of("name", "A-Second", "value", "2"),
                    Map.of("name", "M-Third", "value", "3"))));

        webhookNotificationSender.send(notification, webhookNotificationHandler, notificationHandlerContext);

        ArgumentCaptor<WebhookDelivery> webhookDeliveryArgumentCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(webhookDeliveryService).submit(webhookDeliveryArgumentCaptor.capture());

        assertThat(webhookDeliveryArgumentCaptor.getValue()
            .headers()
            .keySet()).containsExactly("Z-First", "A-Second", "M-Third");
    }

    @Test
    void testSendUsesDefaultBodyWhenTemplateBlank() {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{\"default\":true}");
        when(webhookDeliveryService.submit(any())).thenReturn(CompletableFuture.completedFuture(sentOutcome()));

        Notification notification = createNotification(
            Map.of("webhook", "https://example.com/hook", "webhookBodyTemplate", "   "));

        webhookNotificationSender.send(notification, webhookNotificationHandler, notificationHandlerContext);

        ArgumentCaptor<WebhookDelivery> webhookDeliveryArgumentCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(webhookDeliveryService).submit(webhookDeliveryArgumentCaptor.capture());

        assertThat(webhookDeliveryArgumentCaptor.getValue()
            .payloads()
            .getFirst()
            .json()).isEqualTo("{\"default\":true}");
    }

    @Test
    void testSendFutureFailsWhenNothingWasSent() {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{}");
        when(webhookDeliveryService.submit(any()))
            .thenReturn(
                CompletableFuture.completedFuture(
                    new WebhookDeliveryOutcome(
                        List.of(new PayloadResult("1", Status.FAILED)), null, "boom", "boom")));

        CompletableFuture<Void> future = webhookNotificationSender.send(
            createNotification(Map.of("webhook", "https://example.com/hook")), webhookNotificationHandler,
            notificationHandlerContext);

        assertThat(future).isCompletedExceptionally();
        assertThatThrownBy(future::join)
            .hasCauseInstanceOf(WebhookDeliveryException.class);
    }

    @Test
    void testSendThrowsWhenWebhookUrlMissing() {
        assertThatThrownBy(
            () -> webhookNotificationSender.send(
                createNotification(Map.of()), webhookNotificationHandler, notificationHandlerContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no webhook URL");

        verify(webhookDeliveryService, never()).submit(any());
    }

    @Test
    void testSendThrowsWhenWebhookUrlBlank() {
        assertThatThrownBy(
            () -> webhookNotificationSender.send(
                createNotification(Map.of("webhook", "")), webhookNotificationHandler, notificationHandlerContext))
                    .isInstanceOf(IllegalStateException.class);

        verify(webhookDeliveryService, never()).submit(any());
    }

    @Test
    void testSendReturnsTheDeliveryFutureWithoutBlocking() {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{}");
        when(webhookDeliveryService.submit(any())).thenReturn(new CompletableFuture<>());

        CompletableFuture<Void> future = assertTimeoutPreemptively(
            Duration.ofSeconds(1),
            () -> webhookNotificationSender.send(
                createNotification(Map.of("webhook", "https://example.com/hook")), webhookNotificationHandler,
                notificationHandlerContext));

        assertThat(future).isNotDone();
    }

    private static WebhookDeliveryOutcome sentOutcome() {
        return new WebhookDeliveryOutcome(List.of(new PayloadResult("1", Status.SENT)), null, null, null);
    }

    private static Notification createNotification(Map<String, Object> settings) {
        Notification notification = new Notification();

        notification.setId(1L);
        notification.setName("Alerts");
        notification.setType(Notification.Type.WEBHOOK);
        notification.setSettings(settings);

        return notification;
    }
}
