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

package com.bytechef.platform.notification.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.notification.delivery.WebhookNotificationClient.WebhookRetry;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryException;
import org.springframework.web.client.ResourceAccessException;

/**
 * @author Ivica Cardic
 */
class WebhookNotificationClientTest {

    private final AtomicInteger failuresBeforeSuccess = new AtomicInteger();
    private final AtomicInteger responseStatus = new AtomicInteger(500);
    private final List<Long> requestNanoTimes = new CopyOnWriteArrayList<>();
    private final WebhookNotificationClient webhookNotificationClient = new WebhookNotificationClient();

    private HttpServer httpServer;
    private String url;

    @BeforeEach
    void beforeEach() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

        httpServer.createContext("/hook", httpExchange -> {
            requestNanoTimes.add(System.nanoTime());

            httpExchange.getRequestBody()
                .readAllBytes();

            int status = failuresBeforeSuccess.getAndDecrement() > 0 ? responseStatus.get() : 200;

            httpExchange.sendResponseHeaders(status, -1);
            httpExchange.close();
        });

        httpServer.start();

        url = "http://localhost:" + httpServer.getAddress()
            .getPort() + "/hook";
    }

    @AfterEach
    void afterEach() {
        httpServer.stop(0);
    }

    @Test
    void testNoRetryMakesOneAttemptAndWrapsTheHttpStatus() {
        failuresBeforeSuccess.set(Integer.MAX_VALUE);

        assertThatThrownBy(() -> webhookNotificationClient.deliverEvent(url, Map.of(), WebhookRetry.none()))
            .isInstanceOfSatisfying(
                WebhookDeliveryException.class,
                webhookDeliveryException -> assertThat(webhookDeliveryException.getStatusCode()).isEqualTo(500));

        assertThat(requestNanoTimes).hasSize(1);
    }

    @Test
    void testRetriesUpToMaxRetriesThenWrapsTheRetryException() {
        failuresBeforeSuccess.set(Integer.MAX_VALUE);

        assertThatThrownBy(
            () -> webhookNotificationClient.deliverEvent(
                url, Map.of(), new WebhookRetry(3, Duration.ofMillis(1), 1.0)))
                    .isInstanceOfSatisfying(
                        WebhookDeliveryException.class,
                        webhookDeliveryException -> {
                            assertThat(webhookDeliveryException.getStatusCode()).isNull();
                            assertThat(webhookDeliveryException.getCause()).isInstanceOf(RetryException.class);
                            assertThat(webhookDeliveryException.getMessage()).contains("failed after retries");
                        });

        assertThat(requestNanoTimes).hasSize(4);
    }

    @Test
    void testRetryStopsOnTheFirstSuccess() {
        failuresBeforeSuccess.set(2);

        webhookNotificationClient.deliverEvent(url, Map.of(), new WebhookRetry(5, Duration.ofMillis(1), 1.0));

        assertThat(requestNanoTimes).hasSize(3);
    }

    @Test
    void testRetryBacksOffExponentially() {
        failuresBeforeSuccess.set(Integer.MAX_VALUE);

        assertThatThrownBy(
            () -> webhookNotificationClient.deliverEvent(
                url, Map.of(), new WebhookRetry(2, Duration.ofMillis(50), 2.0)))
                    .isInstanceOf(WebhookDeliveryException.class);

        assertThat(requestNanoTimes).hasSize(3);

        long firstGapMillis = Duration.ofNanos(requestNanoTimes.get(1) - requestNanoTimes.get(0))
            .toMillis();
        long secondGapMillis = Duration.ofNanos(requestNanoTimes.get(2) - requestNanoTimes.get(1))
            .toMillis();

        assertThat(firstGapMillis).isGreaterThanOrEqualTo(50);
        assertThat(secondGapMillis).isGreaterThanOrEqualTo(100);
    }

    @Test
    void testClientErrorIsRetried() {
        failuresBeforeSuccess.set(Integer.MAX_VALUE);
        responseStatus.set(400);

        assertThatThrownBy(
            () -> webhookNotificationClient.deliverEvent(
                url, Map.of(), new WebhookRetry(2, Duration.ofMillis(1), 1.0)))
                    .isInstanceOf(WebhookDeliveryException.class);

        assertThat(requestNanoTimes).hasSize(3);
    }

    @Test
    void testClientErrorWithoutRetryWrapsTheHttpStatus() {
        failuresBeforeSuccess.set(Integer.MAX_VALUE);
        responseStatus.set(404);

        assertThatThrownBy(() -> webhookNotificationClient.deliverEvent(url, Map.of(), WebhookRetry.none()))
            .isInstanceOfSatisfying(
                WebhookDeliveryException.class,
                webhookDeliveryException -> assertThat(webhookDeliveryException.getStatusCode()).isEqualTo(404));
    }

    @Test
    void testConnectionFailureIsWrapped() {
        httpServer.stop(0);

        assertThatThrownBy(() -> webhookNotificationClient.deliverEvent(url, Map.of(), WebhookRetry.none()))
            .isInstanceOfSatisfying(
                WebhookDeliveryException.class,
                webhookDeliveryException -> {
                    assertThat(webhookDeliveryException.getStatusCode()).isNull();
                    assertThat(webhookDeliveryException.getCause()).isInstanceOf(ResourceAccessException.class);
                });
    }

    @Test
    void testRejectsLoopbackUrl() {
        assertThatThrownBy(
            () -> webhookNotificationClient.deliver(
                WebhookDeliveryRequest.of("http://127.0.0.1/hook", "job.status", "{}")))
                    .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRejectsNonHttpScheme() {
        assertThatThrownBy(
            () -> webhookNotificationClient.deliver(
                WebhookDeliveryRequest.of("ftp://example.com/hook", "job.status", "{}")))
                    .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSignatureMatchesHmacSha256OverTimestampDotBody() {
        String signature = WebhookNotificationClient.sign("secret", "12345.{}");

        assertThat(signature).isEqualTo("4df28e080fc082b61331789f00e00e122571a07d78931a85ae3d327292701d4e");
    }
}
