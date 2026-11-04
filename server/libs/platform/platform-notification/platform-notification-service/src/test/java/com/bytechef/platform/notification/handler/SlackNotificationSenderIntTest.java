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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.notification.config.PlatformNotificationIntTestConfiguration;
import com.bytechef.platform.notification.delivery.WebhookDeliveryException;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.domain.Notification;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = PlatformNotificationIntTestConfiguration.class,
    properties = {
        "bytechef.webhook-delivery.concurrency-limit=10", "bytechef.webhook-delivery.max-pending-deliveries=1000"
    })
@Timeout(30)
class SlackNotificationSenderIntTest {

    private final NotificationHandlerContext notificationHandlerContext =
        new NotificationHandlerContext.Builder().build();
    private LoopbackWebhookReceiver loopbackWebhookReceiver;
    private final SlackNotificationHandler slackNotificationHandler = mock(SlackNotificationHandler.class);

    @MockitoBean
    private MailService mailService;

    @Autowired
    private SlackNotificationSender slackNotificationSender;

    @Autowired
    private WebhookDeliveryService webhookDeliveryService;

    private Consumer<String> originalUrlValidator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        loopbackWebhookReceiver = new LoopbackWebhookReceiver();

        originalUrlValidator = (Consumer<String>) ReflectionTestUtils.getField(webhookDeliveryService, "urlValidator");

        ReflectionTestUtils.setField(webhookDeliveryService, "urlValidator", (Consumer<String>) url -> {});

        when(slackNotificationHandler.getText(notificationHandlerContext)).thenReturn("Approval pending");
    }

    @AfterEach
    void afterEach() {
        loopbackWebhookReceiver.close();

        ReflectionTestUtils.setField(webhookDeliveryService, "urlValidator", originalUrlValidator);
    }

    @Test
    void testGetType() {
        assertThat(slackNotificationSender.getType()).isEqualTo(Notification.Type.SLACK);
    }

    @Test
    void testSendPostsTextPayloadToConfiguredWebhookUrl() throws Exception {
        CompletableFuture<Void> future = slackNotificationSender.send(
            createNotification(Map.of("slackWebhookUrl", loopbackWebhookReceiver.getUrl("/services/T/B/X"))),
            slackNotificationHandler, notificationHandlerContext);

        LoopbackWebhookReceiver.ReceivedRequest receivedRequest = loopbackWebhookReceiver.awaitRequest();

        future.join();

        assertThat(receivedRequest.method()).isEqualTo("POST");
        assertThat(receivedRequest.path()).isEqualTo("/services/T/B/X");
        assertThat(receivedRequest.body()).isEqualTo("{\"text\":\"Approval pending\"}");
        assertThat(receivedRequest.headers()).containsEntry("X-ByteChef-Event", "slack.message");
        assertThat(receivedRequest.headers()).doesNotContainKey("X-ByteChef-Signature");
        assertThat(loopbackWebhookReceiver.getReceivedCount()).isEqualTo(1);
        assertThat(future).isCompleted();
    }

    @Test
    void testSendReadsSlackUrlFromSecretSettings() throws Exception {
        Notification notification = createNotification(Map.of());

        notification.setSecretSettings(Map.of("slackWebhookUrl", loopbackWebhookReceiver.getUrl("/services/S/E/C")));

        CompletableFuture<Void> future = slackNotificationSender.send(
            notification, slackNotificationHandler, notificationHandlerContext);

        LoopbackWebhookReceiver.ReceivedRequest receivedRequest = loopbackWebhookReceiver.awaitRequest();

        future.join();

        assertThat(receivedRequest.path()).isEqualTo("/services/S/E/C");
        assertThat(receivedRequest.body()).isEqualTo("{\"text\":\"Approval pending\"}");
        assertThat(future).isCompleted();
    }

    @Test
    void testSendStripsSurroundingWhitespaceFromWebhookUrl() throws Exception {
        CompletableFuture<Void> future = slackNotificationSender.send(
            createNotification(
                Map.of("slackWebhookUrl", "  " + loopbackWebhookReceiver.getUrl("/services/T/B/X") + " \n")),
            slackNotificationHandler, notificationHandlerContext);

        LoopbackWebhookReceiver.ReceivedRequest receivedRequest = loopbackWebhookReceiver.awaitRequest();

        future.join();

        assertThat(receivedRequest.path()).isEqualTo("/services/T/B/X");
        assertThat(future).isCompleted();
    }

    @Test
    void testSendValidatesTheWebhookUrl() {
        ReflectionTestUtils.setField(webhookDeliveryService, "urlValidator", originalUrlValidator);

        CompletableFuture<Void> future = slackNotificationSender.send(
            createNotification(Map.of("slackWebhookUrl", loopbackWebhookReceiver.getUrl("/services/T/B/X"))),
            slackNotificationHandler, notificationHandlerContext);

        assertThatThrownBy(future::join)
            .hasCauseInstanceOf(WebhookDeliveryException.class);

        assertThat(future).isCompletedExceptionally();
        assertThat(loopbackWebhookReceiver.getReceivedCount()).isZero();
    }

    @Test
    void testSendFutureFailsWhenNothingWasSent() {
        loopbackWebhookReceiver.respondWith(500);

        CompletableFuture<Void> future = slackNotificationSender.send(
            createNotification(Map.of("slackWebhookUrl", loopbackWebhookReceiver.getUrl("/services/T/B/X"))),
            slackNotificationHandler, notificationHandlerContext);

        assertThatThrownBy(future::join)
            .hasCauseInstanceOf(WebhookDeliveryException.class);

        assertThat(future).isCompletedExceptionally();
        assertThat(loopbackWebhookReceiver.getReceivedCount()).isEqualTo(1);
    }

    @Test
    void testSendReturnsTheDeliveryFutureWithoutBlocking() throws Exception {
        CountDownLatch holdResponse = new CountDownLatch(1);

        loopbackWebhookReceiver.holdResponses(holdResponse);

        CompletableFuture<Void> future = assertTimeoutPreemptively(
            Duration.ofSeconds(1),
            () -> slackNotificationSender.send(
                createNotification(Map.of("slackWebhookUrl", loopbackWebhookReceiver.getUrl("/services/T/B/X"))),
                slackNotificationHandler, notificationHandlerContext));

        loopbackWebhookReceiver.awaitRequest();

        assertThat(future).isNotDone();

        holdResponse.countDown();

        future.join();

        assertThat(future).isCompleted();
    }

    @Test
    void testSendThrowsWhenWebhookUrlMissing() {
        assertThatThrownBy(
            () -> slackNotificationSender.send(
                createNotification(Map.of()), slackNotificationHandler, notificationHandlerContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no Slack webhook URL");

        assertThat(loopbackWebhookReceiver.getReceivedCount()).isZero();
    }

    @Test
    void testSendThrowsWhenWebhookUrlBlank() {
        assertThatThrownBy(
            () -> slackNotificationSender.send(
                createNotification(Map.of("slackWebhookUrl", "  ")), slackNotificationHandler,
                notificationHandlerContext))
                    .isInstanceOf(IllegalStateException.class);

        assertThat(loopbackWebhookReceiver.getReceivedCount()).isZero();
    }

    private static Notification createNotification(Map<String, Object> settings) {
        Notification notification = new Notification();

        notification.setId(1L);
        notification.setType(Notification.Type.SLACK);
        notification.setSettings(settings);

        return notification;
    }
}
