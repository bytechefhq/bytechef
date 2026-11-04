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

import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.notification.config.PlatformNotificationIntTestConfiguration;
import com.bytechef.platform.notification.delivery.WebhookDelivery;
import com.bytechef.platform.notification.delivery.WebhookDeliveryException;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.domain.Notification;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = PlatformNotificationIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "bytechef.webhook-delivery.allowed-hosts=localhost",
        "bytechef.webhook-delivery.concurrency-limit=10",
        "bytechef.webhook-delivery.max-pending-deliveries=1000"
    })
@Timeout(30)
class WebhookNotificationSenderIntTest {

    private LoopbackWebhookReceiver loopbackWebhookReceiver;
    private final NotificationHandlerContext notificationHandlerContext =
        new NotificationHandlerContext.Builder().build();
    private final WebhookNotificationHandler webhookNotificationHandler = mock(WebhookNotificationHandler.class);

    @MockitoBean
    private MailService mailService;

    @MockitoSpyBean
    private WebhookDeliveryService webhookDeliveryService;

    @Autowired
    private WebhookNotificationSender webhookNotificationSender;

    @BeforeEach
    void beforeEach() {
        loopbackWebhookReceiver = new LoopbackWebhookReceiver();
    }

    @AfterEach
    void afterEach() {
        loopbackWebhookReceiver.close();
    }

    @Test
    void testSendReadsSecretFromSecretSettings() throws Exception {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{\"a\":1}");

        Notification notification = createNotification(Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook")));

        notification.setSecretSettings(Map.of("webhookSecret", "from-secret"));

        CompletableFuture<Void> future = webhookNotificationSender.send(
            notification, webhookNotificationHandler, notificationHandlerContext);

        LoopbackWebhookReceiver.ReceivedRequest receivedRequest = loopbackWebhookReceiver.awaitRequest();

        future.join();

        Map<String, String> headers = receivedRequest.headers();

        assertThat(headers.get("X-ByteChef-Signature"))
            .isEqualTo(
                "t=" + headers.get("X-ByteChef-Timestamp") + ",v1=" + sign(
                    "from-secret", headers.get("X-ByteChef-Timestamp") + ".{\"a\":1}"));
    }

    @Test
    void testSendPostsSignedDeliveryWithoutRetry() throws Exception {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{\"a\":1}");

        CompletableFuture<Void> future = webhookNotificationSender.send(
            createNotification(
                Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook"), "webhookSecret", "secret")),
            webhookNotificationHandler, notificationHandlerContext);

        LoopbackWebhookReceiver.ReceivedRequest receivedRequest = loopbackWebhookReceiver.awaitRequest();

        future.join();

        Map<String, String> headers = receivedRequest.headers();

        assertThat(receivedRequest.method()).isEqualTo("POST");
        assertThat(receivedRequest.path()).isEqualTo("/hook");
        assertThat(receivedRequest.body()).isEqualTo("{\"a\":1}");
        assertThat(headers).containsEntry("X-ByteChef-Event", "job.status");
        assertThat(headers.get("X-ByteChef-Signature"))
            .isEqualTo(
                "t=" + headers.get("X-ByteChef-Timestamp") + ",v1=" + sign(
                    "secret", headers.get("X-ByteChef-Timestamp") + ".{\"a\":1}"));
        assertThat(loopbackWebhookReceiver.getReceivedCount()).isEqualTo(1);
        assertThat(future).isCompleted();
    }

    @Test
    void testSendPostsUnsignedDeliveryWhenNoSecretIsConfigured() throws Exception {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{\"a\":1}");

        CompletableFuture<Void> future = webhookNotificationSender.send(
            createNotification(Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook"))),
            webhookNotificationHandler, notificationHandlerContext);

        LoopbackWebhookReceiver.ReceivedRequest receivedRequest = loopbackWebhookReceiver.awaitRequest();

        future.join();

        assertThat(receivedRequest.headers()).doesNotContainKey("X-ByteChef-Signature");
    }

    @Test
    void testSendPassesTimestampAndMintsDeliveryId() throws Exception {
        Instant timestamp = Instant.parse("2026-11-03T10:00:00Z");
        NotificationHandlerContext context = new NotificationHandlerContext.Builder().deliveryId("d-1")
            .timestamp(timestamp)
            .build();

        when(webhookNotificationHandler.getPayload(any())).thenReturn("{\"a\":1}");

        CompletableFuture<Void> future = webhookNotificationSender.send(
            createNotification(Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook"))),
            webhookNotificationHandler, context);

        LoopbackWebhookReceiver.ReceivedRequest receivedRequest = loopbackWebhookReceiver.awaitRequest();

        future.join();

        assertThat(receivedRequest.headers()
            .get("X-ByteChef-Delivery")).isNotEqualTo("d-1");
        assertThat(receivedRequest.headers()).containsEntry(
            "X-ByteChef-Timestamp", String.valueOf(timestamp.toEpochMilli()));
    }

    @Test
    void testSendGivesEachDeliveryItsOwnDeliveryIdMatchingThePayload() throws Exception {
        NotificationHandlerContext context = new NotificationHandlerContext.Builder().deliveryId("d-1")
            .build();

        when(webhookNotificationHandler.getPayload(any())).thenAnswer(
            invocation -> "{\"deliveryId\":\"" + ((NotificationHandlerContext) invocation.getArgument(0))
                .getDeliveryId() + "\"}");

        Notification notification = createNotification(Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook")));

        webhookNotificationSender.send(notification, webhookNotificationHandler, context)
            .join();

        LoopbackWebhookReceiver.ReceivedRequest firstRequest = loopbackWebhookReceiver.awaitRequest();

        webhookNotificationSender.send(notification, webhookNotificationHandler, context)
            .join();

        LoopbackWebhookReceiver.ReceivedRequest secondRequest = loopbackWebhookReceiver.awaitRequest();

        String firstDeliveryId = firstRequest.headers()
            .get("X-ByteChef-Delivery");
        String secondDeliveryId = secondRequest.headers()
            .get("X-ByteChef-Delivery");

        assertThat(firstDeliveryId).isNotEqualTo(secondDeliveryId);

        assertThat(firstDeliveryId).isNotEqualTo("d-1");
        assertThat(secondDeliveryId).isNotEqualTo("d-1");
        assertThat(firstRequest.body()).isEqualTo("{\"deliveryId\":\"" + firstDeliveryId + "\"}");
        assertThat(secondRequest.body()).isEqualTo("{\"deliveryId\":\"" + secondDeliveryId + "\"}");
    }

    @Test
    void testSendRendersTemplateWithDeliveryId() throws Exception {
        CompletableFuture<Void> future = webhookNotificationSender.send(
            createNotification(
                Map.of(
                    "webhook", loopbackWebhookReceiver.getUrl("/hook"), "webhookBodyTemplate",
                    "{\"d\":\"{{deliveryId}}\"}")),
            webhookNotificationHandler, notificationHandlerContext);

        LoopbackWebhookReceiver.ReceivedRequest receivedRequest = loopbackWebhookReceiver.awaitRequest();

        future.join();

        String deliveryId = receivedRequest.headers()
            .get("X-ByteChef-Delivery");

        assertThat(receivedRequest.body()).isEqualTo("{\"d\":\"" + deliveryId + "\"}");

        verify(webhookNotificationHandler, never()).getPayload(any());
    }

    @Test
    void testSendPassesHeadersInOrder() throws Exception {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{}");

        Notification notification = createNotification(Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook")));

        notification.setSecretSettings(
            Map.of(
                "webhookHeaders",
                List.of(
                    Map.of("name", "Z-First", "value", "1"), Map.of("name", "A-Second", "value", "2"),
                    Map.of("name", "M-Third", "value", "3"))));

        CompletableFuture<Void> future = webhookNotificationSender.send(
            notification, webhookNotificationHandler, notificationHandlerContext);

        LoopbackWebhookReceiver.ReceivedRequest receivedRequest = loopbackWebhookReceiver.awaitRequest();

        future.join();

        Map<String, String> headers = receivedRequest.headers();

        assertThat(headers).containsEntry("Z-First", "1");
        assertThat(headers).containsEntry("A-Second", "2");
        assertThat(headers).containsEntry("M-Third", "3");

        ArgumentCaptor<WebhookDelivery> webhookDeliveryArgumentCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(webhookDeliveryService).submit(webhookDeliveryArgumentCaptor.capture());

        WebhookDelivery webhookDelivery = webhookDeliveryArgumentCaptor.getValue();

        assertThat(webhookDelivery.headers()
            .keySet()).containsExactly("Z-First", "A-Second", "M-Third");
    }

    @Test
    void testSendUsesDefaultBodyWhenTemplateBlank() throws Exception {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{\"default\":true}");

        CompletableFuture<Void> future = webhookNotificationSender.send(
            createNotification(
                Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook"), "webhookBodyTemplate", "   ")),
            webhookNotificationHandler, notificationHandlerContext);

        LoopbackWebhookReceiver.ReceivedRequest receivedRequest = loopbackWebhookReceiver.awaitRequest();

        future.join();

        assertThat(receivedRequest.body()).isEqualTo("{\"default\":true}");
    }

    @Test
    void testSendValidatesTheWebhookUrl() {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{}");

        CompletableFuture<Void> future = webhookNotificationSender.send(
            createNotification(Map.of("webhook", getDisallowedHostUrl("/hook"))), webhookNotificationHandler,
            notificationHandlerContext);

        assertThatThrownBy(future::join)
            .hasCauseInstanceOf(WebhookDeliveryException.class);

        assertThat(loopbackWebhookReceiver.getReceivedCount()).isZero();
    }

    @Test
    void testSendFutureFailsWhenNothingWasSent() {
        loopbackWebhookReceiver.respondWith(500);

        when(webhookNotificationHandler.getPayload(any())).thenReturn("{}");

        CompletableFuture<Void> future = webhookNotificationSender.send(
            createNotification(Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook"))),
            webhookNotificationHandler, notificationHandlerContext);

        assertThatThrownBy(future::join)
            .hasCauseInstanceOf(WebhookDeliveryException.class);

        assertThat(future).isCompletedExceptionally();
        assertThat(loopbackWebhookReceiver.getReceivedCount()).isEqualTo(1);
    }

    @Test
    void testSendThrowsWhenWebhookUrlMissing() {
        assertThatThrownBy(
            () -> webhookNotificationSender.send(
                createNotification(Map.of()), webhookNotificationHandler, notificationHandlerContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no webhook URL");

        assertThat(loopbackWebhookReceiver.getReceivedCount()).isZero();
    }

    @Test
    void testSendThrowsWhenWebhookUrlBlank() {
        assertThatThrownBy(
            () -> webhookNotificationSender.send(
                createNotification(Map.of("webhook", "")), webhookNotificationHandler, notificationHandlerContext))
                    .isInstanceOf(IllegalStateException.class);

        assertThat(loopbackWebhookReceiver.getReceivedCount()).isZero();
    }

    @Test
    void testSendReturnsTheDeliveryFutureWithoutBlocking() throws Exception {
        CountDownLatch holdResponse = new CountDownLatch(1);

        loopbackWebhookReceiver.holdResponses(holdResponse);

        when(webhookNotificationHandler.getPayload(any())).thenReturn("{}");

        CompletableFuture<Void> future = assertTimeoutPreemptively(
            Duration.ofSeconds(1),
            () -> webhookNotificationSender.send(
                createNotification(Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook"))),
                webhookNotificationHandler, notificationHandlerContext));

        loopbackWebhookReceiver.awaitRequest();

        assertThat(future).isNotDone();

        holdResponse.countDown();

        future.join();

        assertThat(future).isCompleted();
    }

    @Test
    void testSendTestUsesSameDeliveryAsSend() throws Exception {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{\"a\":1}");

        loopbackWebhookReceiver.respondWith(200, "ok");

        Notification notification = createNotification(
            Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook"), "webhookSecret", "secret"));

        notification.setSecretSettings(
            Map.of("webhookHeaders", List.of(Map.of("name", "X-Token", "value", "t"))));

        webhookNotificationSender.send(notification, webhookNotificationHandler, notificationHandlerContext)
            .join();

        NotificationTestResult notificationTestResult = webhookNotificationSender.sendTest(
            notification, webhookNotificationHandler, notificationHandlerContext);

        ArgumentCaptor<WebhookDelivery> submitCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);
        ArgumentCaptor<WebhookDelivery> testCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(webhookDeliveryService).submit(submitCaptor.capture());
        verify(webhookDeliveryService).sendTest(testCaptor.capture());

        WebhookDelivery sent = submitCaptor.getValue();
        WebhookDelivery tested = testCaptor.getValue();

        assertThat(tested.headers()).isEqualTo(sent.headers());
        assertThat(tested.url()).isEqualTo(sent.url());
        assertThat(tested.source()).isEqualTo(sent.source());
        assertThat(tested.description()).isEqualTo(sent.description());
        assertThat(tested.eventType()).isEqualTo(sent.eventType());
        assertThat(tested.secret()).isEqualTo(sent.secret());
        assertThat(tested.validateUrl()).isEqualTo(sent.validateUrl());
        assertThat(tested.timestamp()).isEqualTo(sent.timestamp());
        assertThat(tested.payloads()).isEqualTo(sent.payloads());
        assertThat(tested.deliveryId()).isNotEqualTo(sent.deliveryId());
        assertThat(notificationTestResult.success()).isTrue();
        assertThat(notificationTestResult.statusCode()).isEqualTo(200);
        assertThat(notificationTestResult.responseBody()).isEqualTo("ok");
        assertThat(loopbackWebhookReceiver.getReceivedCount()).isEqualTo(2);
    }

    @Test
    void testSendTestReportsSignedOnlyWithSecret() {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{}");

        assertThat(
            webhookNotificationSender.sendTest(
                createNotification(
                    Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook"), "webhookSecret", "secret")),
                webhookNotificationHandler, notificationHandlerContext)
                .signed()).isTrue();
        assertThat(
            webhookNotificationSender.sendTest(
                createNotification(Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook"))),
                webhookNotificationHandler, notificationHandlerContext)
                .signed()).isFalse();
        assertThat(
            webhookNotificationSender.sendTest(
                createNotification(
                    Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook"), "webhookSecret", " ")),
                webhookNotificationHandler, notificationHandlerContext)
                .signed()).isFalse();
    }

    @Test
    void testSendTestMapsFailure() {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{}");

        loopbackWebhookReceiver.respondWith(422, "bad");

        NotificationTestResult notificationTestResult = webhookNotificationSender.sendTest(
            createNotification(Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook"))),
            webhookNotificationHandler, notificationHandlerContext);

        assertThat(notificationTestResult.success()).isFalse();
        assertThat(notificationTestResult.statusCode()).isEqualTo(422);
        assertThat(notificationTestResult.responseBody()).isEqualTo("bad");
        assertThat(notificationTestResult.error()).contains("HTTP 422");
    }

    @Test
    void testSendTestValidatesTheWebhookUrl() {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{}");

        NotificationTestResult notificationTestResult = webhookNotificationSender.sendTest(
            createNotification(Map.of("webhook", getDisallowedHostUrl("/hook"))), webhookNotificationHandler,
            notificationHandlerContext);

        assertThat(notificationTestResult.success()).isFalse();
        assertThat(notificationTestResult.error()).isNotBlank();
        assertThat(loopbackWebhookReceiver.getReceivedCount()).isZero();
    }

    @Test
    void testSendTestFailsWhenWebhookUrlMissing() {
        NotificationTestResult notificationTestResult = webhookNotificationSender.sendTest(
            createNotification(Map.of()), webhookNotificationHandler, notificationHandlerContext);

        assertThat(notificationTestResult.success()).isFalse();
        assertThat(notificationTestResult.error()).isEqualTo("Webhook URL is required");

        verify(webhookDeliveryService, never()).sendTest(any());
    }

    @Test
    void testSendTestReportsMissingUrl() {
        NotificationTestResult notificationTestResult = webhookNotificationSender.sendTest(
            createNotification(Map.of("webhook", "  ")), webhookNotificationHandler, notificationHandlerContext);

        assertThat(notificationTestResult.success()).isFalse();
        assertThat(notificationTestResult.error()).isEqualTo("Webhook URL is required");

        verify(webhookDeliveryService, never()).sendTest(any());
    }

    @Test
    void testSendTestRedactsSigningSecretAndHeaderValues() {
        when(webhookNotificationHandler.getPayload(any())).thenReturn("{}");

        loopbackWebhookReceiver.respondWith(403, "got token-12345 and whsec_signing and abc");

        Notification notification = createNotification(Map.of("webhook", loopbackWebhookReceiver.getUrl("/hook")));

        notification.setSecretSettings(
            Map.of(
                "webhookSecret", "whsec_signing", "webhookHeaders",
                List.of(
                    Map.of("name", "Authorization", "value", "token-12345"),
                    Map.of("name", "X-Short", "value", "abc"))));

        NotificationTestResult notificationTestResult = webhookNotificationSender.sendTest(
            notification, webhookNotificationHandler, notificationHandlerContext);

        assertThat(notificationTestResult.responseBody()).isEqualTo("got ******** and ******** and abc");
        assertThat(notificationTestResult.error()).doesNotContain("token-12345", "whsec_signing");
    }

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private String getDisallowedHostUrl(String path) {
        String url = loopbackWebhookReceiver.getUrl(path);

        return url.replace("localhost", "127.0.0.1");
    }

    private static String sign(String secret, String signedContent) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        HexFormat hexFormat = HexFormat.of();

        return hexFormat.formatHex(mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8)));
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
