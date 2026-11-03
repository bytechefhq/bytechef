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

import com.bytechef.platform.notification.delivery.config.WebhookDeliveryIntTestConfiguration;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.ResourceAccessException;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = WebhookDeliveryIntTestConfiguration.class,
    properties = {
        "bytechef.webhook-delivery.concurrency-limit=2", "bytechef.webhook-delivery.max-pending-deliveries=2"
    })
class WebhookNotificationClientIntTest {

    private final AtomicInteger failuresBeforeSuccess = new AtomicInteger();
    private final AtomicInteger responseStatus = new AtomicInteger(500);
    private final List<Long> requestNanoTimes = new CopyOnWriteArrayList<>();

    private final AtomicReference<Headers> receivedHeaders = new AtomicReference<>();
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private final AtomicReference<String> receivedRawQuery = new AtomicReference<>();
    private final AtomicInteger redirectTargetHits = new AtomicInteger();

    @Autowired
    private WebhookNotificationClient webhookNotificationClient;

    private HttpServer httpServer;
    private String url;

    @BeforeEach
    void beforeEach() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

        httpServer.createContext("/hook", httpExchange -> {
            URI requestUri = httpExchange.getRequestURI();

            receivedRawQuery.set(requestUri.getRawQuery());
            requestNanoTimes.add(System.nanoTime());
            receivedHeaders.set(httpExchange.getRequestHeaders());

            byte[] requestBody = httpExchange.getRequestBody()
                .readAllBytes();

            receivedBody.set(new String(requestBody, StandardCharsets.UTF_8));

            int status = failuresBeforeSuccess.getAndDecrement() > 0 ? responseStatus.get() : 200;

            httpExchange.sendResponseHeaders(status, -1);
            httpExchange.close();
        });

        httpServer.createContext("/redirect", httpExchange -> {
            httpExchange.getRequestBody()
                .readAllBytes();

            int port = httpServer.getAddress()
                .getPort();

            httpExchange.getResponseHeaders()
                .add("Location", "http://localhost:" + port + "/target");
            httpExchange.sendResponseHeaders(302, -1);
            httpExchange.close();
        });

        httpServer.createContext("/target", httpExchange -> {
            redirectTargetHits.incrementAndGet();

            httpExchange.sendResponseHeaders(200, -1);
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
    void testPostSendsJsonBodyAndEventHeaders() {
        WebhookDelivery webhookDelivery = delivery(url).eventType("job.status")
            .headers(Map.of("X-Custom", "custom-value"))
            .build();

        webhookNotificationClient.post(webhookDelivery, webhookDelivery.payloads()
            .getFirst());

        Headers headers = receivedHeaders.get();

        assertThat(headers.getFirst("Content-Type")).isEqualTo("application/json");
        assertThat(headers.getFirst("X-ByteChef-Event")).isEqualTo("job.status");
        assertThat(headers.getFirst("X-ByteChef-Timestamp")).isNotBlank();
        assertThat(headers.getFirst("X-ByteChef-Delivery")).isNotBlank();
        assertThat(headers.getFirst("X-Custom")).isEqualTo("custom-value");
        assertThat(headers.getFirst("X-ByteChef-Signature")).isNull();
        assertThat(receivedBody.get()).isEqualTo("{\"a\":1}");
    }

    @Test
    void testCustomHeadersCannotOverrideFixedHeaders() {
        Map<String, String> customHeaders = new LinkedHashMap<>();

        customHeaders.put("X-ByteChef-Event", "evil");
        customHeaders.put("Content-Type", "text/plain");
        customHeaders.put("Authorization", "a");

        WebhookDelivery webhookDelivery = delivery(url).eventType("job.status")
            .headers(customHeaders)
            .build();

        webhookNotificationClient.post(webhookDelivery, webhookDelivery.payloads()
            .getFirst());

        Headers headers = receivedHeaders.get();

        assertThat(headers.get("X-ByteChef-Event")).containsExactly("job.status");
        assertThat(headers.get("Content-Type")).containsExactly("application/json");
        assertThat(headers.getFirst("Authorization")).isEqualTo("a");
    }

    @Test
    void testPostEncodesUrlCharactersThatAStrictUriRejects() {
        WebhookDelivery webhookDelivery = delivery(url + "?status=a b|c^d").build();

        webhookNotificationClient.post(webhookDelivery, webhookDelivery.payloads()
            .getFirst());

        assertThat(receivedRawQuery.get()).isEqualTo("status=a%20b%7Cc%5Ed");
        assertThat(receivedBody.get()).isEqualTo("{\"a\":1}");
    }

    @Test
    void testPostWithoutEventTypeSendsBareJson() {
        WebhookDelivery webhookDelivery = delivery(url).build();

        webhookNotificationClient.post(webhookDelivery, webhookDelivery.payloads()
            .getFirst());

        Headers headers = receivedHeaders.get();

        assertThat(headers.getFirst("Content-Type")).isEqualTo("application/json");
        assertThat(headers.getFirst("X-ByteChef-Event")).isNull();
        assertThat(headers.getFirst("X-ByteChef-Timestamp")).isNull();
        assertThat(headers.getFirst("X-ByteChef-Delivery")).isNull();
        assertThat(receivedBody.get()).isEqualTo("{\"a\":1}");
    }

    @Test
    void testPostSignsWithSecret() {
        WebhookDelivery webhookDelivery = delivery(url).eventType("job.status")
            .secret("s3cret")
            .build();

        webhookNotificationClient.post(webhookDelivery, webhookDelivery.payloads()
            .getFirst());

        Headers headers = receivedHeaders.get();
        String timestamp = headers.getFirst("X-ByteChef-Timestamp");

        assertThat(headers.getFirst("X-ByteChef-Signature")).isEqualTo(
            "t=" + timestamp + ",v1=" + WebhookNotificationClient.sign("s3cret", timestamp + ".{\"a\":1}"));
    }

    @Test
    void testPostUsesSuppliedDeliveryIdAndTimestampInHeadersAndSignature() {
        Instant timestamp = Instant.parse("2026-11-03T10:00:00Z");
        WebhookDelivery webhookDelivery = delivery(url).eventType("job.status")
            .secret("s3cret")
            .deliveryId("d-1")
            .timestamp(timestamp)
            .build();

        webhookNotificationClient.post(webhookDelivery, webhookDelivery.payloads()
            .getFirst());

        Headers headers = receivedHeaders.get();

        assertThat(headers.getFirst("X-ByteChef-Delivery")).isEqualTo("d-1");
        assertThat(headers.getFirst("X-ByteChef-Timestamp")).isEqualTo("1793700000000");
        assertThat(headers.getFirst("X-ByteChef-Signature")).isEqualTo(
            "t=1793700000000,v1=" + WebhookNotificationClient.sign("s3cret", "1793700000000.{\"a\":1}"));
    }

    @Test
    void testNon2xxIsAnException() {
        failuresBeforeSuccess.set(Integer.MAX_VALUE);

        WebhookDelivery webhookDelivery = delivery(url).build();

        assertThatThrownBy(() -> webhookNotificationClient.post(webhookDelivery, webhookDelivery.payloads()
            .getFirst())).isInstanceOfSatisfying(
                WebhookDeliveryException.class,
                webhookDeliveryException -> {
                    assertThat(webhookDeliveryException.getStatusCode()).isEqualTo(500);
                    assertThat(webhookDeliveryException.getMessage()).contains("localhost")
                        .doesNotContain("/hook");
                });

        assertThat(requestNanoTimes).hasSize(1);
    }

    @Test
    void testRedirectIsAnExceptionAndIsNotFollowed() {
        String redirectUrl = "http://localhost:" + httpServer.getAddress()
            .getPort() + "/redirect";
        WebhookDelivery webhookDelivery = delivery(redirectUrl).build();

        assertThatThrownBy(() -> webhookNotificationClient.post(webhookDelivery, webhookDelivery.payloads()
            .getFirst())).isInstanceOfSatisfying(
                WebhookDeliveryException.class,
                webhookDeliveryException -> assertThat(webhookDeliveryException.getStatusCode()).isEqualTo(302));

        assertThat(redirectTargetHits.get()).isZero();
    }

    @Test
    void testConnectionFailureHasNoStatusCode() {
        httpServer.stop(0);

        WebhookDelivery webhookDelivery = delivery(url).build();

        assertThatThrownBy(() -> webhookNotificationClient.post(webhookDelivery, webhookDelivery.payloads()
            .getFirst())).isInstanceOfSatisfying(
                WebhookDeliveryException.class,
                webhookDeliveryException -> {
                    assertThat(webhookDeliveryException.getStatusCode()).isNull();
                    assertThat(webhookDeliveryException.getCause()).isInstanceOf(ResourceAccessException.class);
                    assertThat(webhookDeliveryException.getMessage()).doesNotContain("/hook");
                });
    }

    @Test
    void testInvalidUrlThrowsInvalidWebhookUrlException() {
        for (String invalidUrl : List.of("not a url", "ftp://x")) {
            WebhookDelivery webhookDelivery = delivery(invalidUrl).build();

            assertThatThrownBy(() -> webhookNotificationClient.post(webhookDelivery, webhookDelivery.payloads()
                .getFirst())).isInstanceOf(InvalidWebhookUrlException.class);
        }
    }

    private static WebhookDelivery.Builder delivery(String targetUrl) {
        return WebhookDelivery.builder("test", targetUrl)
            .tenantId("tenant")
            .description("test delivery")
            .payload("body", "{\"a\":1}");
    }

    @Test
    void testSignatureMatchesHmacSha256OverTimestampDotBody() {
        String signature = WebhookNotificationClient.sign("secret", "12345.{}");

        assertThat(signature).isEqualTo("4df28e080fc082b61331789f00e00e122571a07d78931a85ae3d327292701d4e");
    }
}
