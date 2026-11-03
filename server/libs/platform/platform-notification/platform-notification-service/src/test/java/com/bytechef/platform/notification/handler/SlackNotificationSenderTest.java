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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.notification.delivery.WebhookDelivery;
import com.bytechef.platform.notification.delivery.WebhookDeliveryException;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome.PayloadResult;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome.Status;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.delivery.WebhookTestResult;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
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
@ExtendWith(ObjectMapperSetupExtension.class)
@Timeout(5)
class SlackNotificationSenderTest {

    private final NotificationHandlerContext notificationHandlerContext =
        new NotificationHandlerContext.Builder().build();
    private final SlackNotificationHandler slackNotificationHandler = mock(SlackNotificationHandler.class);
    private final WebhookDeliveryService webhookDeliveryService = mock(WebhookDeliveryService.class);
    private final SlackNotificationSender slackNotificationSender = new SlackNotificationSender(
        webhookDeliveryService);

    @Test
    void testGetType() {
        assertThat(slackNotificationSender.getType()).isEqualTo(Notification.Type.SLACK);
    }

    @Test
    void testSendSubmitsTextPayloadToConfiguredWebhookUrl() {
        when(slackNotificationHandler.getText(notificationHandlerContext)).thenReturn("Approval pending");
        when(webhookDeliveryService.submit(any())).thenReturn(CompletableFuture.completedFuture(sentOutcome()));

        CompletableFuture<Void> future = slackNotificationSender.send(
            createNotification(Map.of("slackWebhookUrl", "https://hooks.slack.com/services/T/B/X")),
            slackNotificationHandler, notificationHandlerContext);

        WebhookDelivery webhookDelivery = captureDelivery();

        assertThat(webhookDelivery.source()).isEqualTo("slack");
        assertThat(webhookDelivery.url()).isEqualTo("https://hooks.slack.com/services/T/B/X");
        assertThat(webhookDelivery.eventType()).isEqualTo("slack.message");
        assertThat(webhookDelivery.secret()).isNull();
        assertThat(webhookDelivery.validateUrl()).isTrue();
        assertThat(webhookDelivery.payloads()).hasSize(1);
        assertThat(webhookDelivery.payloads()
            .getFirst()
            .json()).isEqualTo("{\"text\":\"Approval pending\"}");
        assertThat(webhookDelivery.retry()
            .maxRetries()).isZero();
        assertThat(future).isCompleted();
    }

    @Test
    void testSendReadsSlackUrlFromSecretSettings() {
        when(slackNotificationHandler.getText(notificationHandlerContext)).thenReturn("Approval pending");
        when(webhookDeliveryService.submit(any())).thenReturn(CompletableFuture.completedFuture(sentOutcome()));

        Notification notification = createNotification(Map.of());

        notification.setSecretSettings(Map.of("slackWebhookUrl", "https://hooks.slack.com/services/S/E/C"));

        slackNotificationSender.send(notification, slackNotificationHandler, notificationHandlerContext);

        assertThat(captureDelivery().url()).isEqualTo("https://hooks.slack.com/services/S/E/C");
    }

    @Test
    void testSendStripsSurroundingWhitespaceFromWebhookUrl() {
        when(slackNotificationHandler.getText(notificationHandlerContext)).thenReturn("Approval pending");
        when(webhookDeliveryService.submit(any())).thenReturn(CompletableFuture.completedFuture(sentOutcome()));

        slackNotificationSender.send(
            createNotification(Map.of("slackWebhookUrl", "  https://hooks.slack.com/services/T/B/X \n")),
            slackNotificationHandler, notificationHandlerContext);

        assertThat(captureDelivery().url()).isEqualTo("https://hooks.slack.com/services/T/B/X");
    }

    @Test
    void testSendFutureFailsWhenNothingWasSent() {
        when(slackNotificationHandler.getText(notificationHandlerContext)).thenReturn("Approval pending");
        when(webhookDeliveryService.submit(any()))
            .thenReturn(
                CompletableFuture.completedFuture(
                    new WebhookDeliveryOutcome(
                        List.of(new PayloadResult("1", Status.DROPPED)), null, null, null)));

        CompletableFuture<Void> future = slackNotificationSender.send(
            createNotification(Map.of("slackWebhookUrl", "https://hooks.slack.com/services/T/B/X")),
            slackNotificationHandler, notificationHandlerContext);

        assertThat(future).isCompletedExceptionally();
        assertThatThrownBy(future::join)
            .hasCauseInstanceOf(WebhookDeliveryException.class);
    }

    @Test
    void testSendReturnsTheDeliveryFutureWithoutBlocking() {
        when(slackNotificationHandler.getText(notificationHandlerContext)).thenReturn("Approval pending");
        when(webhookDeliveryService.submit(any())).thenReturn(new CompletableFuture<>());

        CompletableFuture<Void> future = assertTimeoutPreemptively(
            Duration.ofSeconds(1),
            () -> slackNotificationSender.send(
                createNotification(Map.of("slackWebhookUrl", "https://hooks.slack.com/services/T/B/X")),
                slackNotificationHandler, notificationHandlerContext));

        assertThat(future).isNotDone();
    }

    @Test
    void testSendThrowsWhenWebhookUrlMissing() {
        assertThatThrownBy(
            () -> slackNotificationSender.send(
                createNotification(Map.of()), slackNotificationHandler, notificationHandlerContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no Slack webhook URL");

        verify(webhookDeliveryService, never()).submit(any());
    }

    @Test
    void testSendThrowsWhenWebhookUrlBlank() {
        assertThatThrownBy(
            () -> slackNotificationSender.send(
                createNotification(Map.of("slackWebhookUrl", "  ")), slackNotificationHandler,
                notificationHandlerContext))
                    .isInstanceOf(IllegalStateException.class);

        verify(webhookDeliveryService, never()).submit(any());
    }

    private WebhookDelivery captureDelivery() {
        ArgumentCaptor<WebhookDelivery> webhookDeliveryArgumentCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(webhookDeliveryService).submit(webhookDeliveryArgumentCaptor.capture());

        return webhookDeliveryArgumentCaptor.getValue();
    }

    @Test
    void testSendTestPostsSlackText() {
        when(slackNotificationHandler.getText(any())).thenReturn("Approval pending");
        when(webhookDeliveryService.sendTest(any())).thenReturn(new WebhookTestResult(true, 200, "ok", null));

        NotificationTestResult notificationTestResult = slackNotificationSender.sendTest(
            createNotification(Map.of("slackWebhookUrl", " https://hooks.slack.com/services/T/B/X ")),
            slackNotificationHandler, notificationHandlerContext);

        ArgumentCaptor<WebhookDelivery> webhookDeliveryArgumentCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(webhookDeliveryService).sendTest(webhookDeliveryArgumentCaptor.capture());
        verify(webhookDeliveryService, never()).submit(any());

        WebhookDelivery webhookDelivery = webhookDeliveryArgumentCaptor.getValue();

        assertThat(webhookDelivery.url()).isEqualTo("https://hooks.slack.com/services/T/B/X");
        assertThat(webhookDelivery.eventType()).isEqualTo("slack.message");
        assertThat(webhookDelivery.validateUrl()).isTrue();
        assertThat(webhookDelivery.payloads()
            .getFirst()
            .json()).isEqualTo("{\"text\":\"Approval pending\"}");
        assertThat(notificationTestResult.success()).isTrue();
        assertThat(notificationTestResult.signed()).isFalse();
    }

    @Test
    void testSendTestFailsWhenSlackUrlMissing() {
        NotificationTestResult notificationTestResult = slackNotificationSender.sendTest(
            createNotification(Map.of()), slackNotificationHandler, notificationHandlerContext);

        assertThat(notificationTestResult.success()).isFalse();
        assertThat(notificationTestResult.error()).isEqualTo("Slack webhook URL is required");

        verify(webhookDeliveryService, never()).sendTest(any());
    }

    @Test
    void testSendTestReportsMissingUrl() {
        NotificationTestResult notificationTestResult = slackNotificationSender.sendTest(
            createNotification(Map.of("slackWebhookUrl", "  ")), slackNotificationHandler, notificationHandlerContext);

        assertThat(notificationTestResult.success()).isFalse();
        assertThat(notificationTestResult.error()).isEqualTo("Slack webhook URL is required");

        verify(webhookDeliveryService, never()).sendTest(any());
    }

    private static WebhookDeliveryOutcome sentOutcome() {
        return new WebhookDeliveryOutcome(List.of(new PayloadResult("1", Status.SENT)), null, null, null);
    }

    private static Notification createNotification(Map<String, Object> settings) {
        Notification notification = new Notification();

        notification.setId(1L);
        notification.setType(Notification.Type.SLACK);
        notification.setSettings(settings);

        return notification;
    }
}
