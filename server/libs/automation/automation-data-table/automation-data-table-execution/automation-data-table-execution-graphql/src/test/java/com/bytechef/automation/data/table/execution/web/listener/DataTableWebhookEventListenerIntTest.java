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

package com.bytechef.automation.data.table.execution.web.listener;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.automation.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.automation.data.table.execution.config.DataTableWebhookEventListenerIntTestConfiguration;
import com.bytechef.automation.data.table.execution.event.DataTableWebhookEvent;
import com.bytechef.platform.configuration.domain.Environment;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableWebhookEventListenerIntTestConfiguration.class)
public class DataTableWebhookEventListenerIntTest {

    private static final int WIREMOCK_PORT = 9997;
    private static final String BASE_URL = "http://localhost:" + WIREMOCK_PORT;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig()
            .port(WIREMOCK_PORT)
            .http2PlainDisabled(true))
        .configureStaticDsl(true)
        .build();

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private DataTableWebhookService dataTableWebhookService;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
    }

    @Test
    void testWebhookEventTriggersHttpCall() {
        String webhookPath = "/webhook/test";

        stubFor(post(urlPathEqualTo(webhookPath))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("OK")));

        when(dataTableWebhookService.listWebhooks("orders", Environment.DEVELOPMENT.ordinal()))
            .thenReturn(List.of(
                new DataTableWebhookService.Webhook(
                    1L, 1L, BASE_URL + webhookPath, DataTableWebhookType.RECORD_CREATED,
                    Environment.DEVELOPMENT.ordinal())));

        DataTableWebhookEvent event = new DataTableWebhookEvent(
            "orders",
            DataTableWebhookType.RECORD_CREATED,
            Map.of("id", 1L, "name", "Test Order"),
            Environment.DEVELOPMENT.ordinal());

        applicationEventPublisher.publishEvent(event);

        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(1, postRequestedFor(urlPathEqualTo(webhookPath))
                .withRequestBody(containing("\"type\":\"RECORD_CREATED\""))
                .withRequestBody(containing("\"table\":\"orders\""))
                .withRequestBody(containing("\"payload\""))));
    }

    @Test
    void testWebhookEventWithMultipleWebhooks() {
        String webhookPath1 = "/webhook/first";
        String webhookPath2 = "/webhook/second";

        stubFor(post(urlPathEqualTo(webhookPath1))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("OK")));

        stubFor(post(urlPathEqualTo(webhookPath2))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("OK")));

        when(dataTableWebhookService.listWebhooks("products", Environment.DEVELOPMENT.ordinal()))
            .thenReturn(List.of(
                new DataTableWebhookService.Webhook(
                    1L, 1L, BASE_URL + webhookPath1, DataTableWebhookType.RECORD_UPDATED,
                    Environment.DEVELOPMENT.ordinal()),
                new DataTableWebhookService.Webhook(
                    2L, 1L, BASE_URL + webhookPath2, DataTableWebhookType.RECORD_UPDATED,
                    Environment.DEVELOPMENT.ordinal())));

        DataTableWebhookEvent event = new DataTableWebhookEvent(
            "products",
            DataTableWebhookType.RECORD_UPDATED,
            Map.of("id", 5L, "price", 99.99),
            Environment.DEVELOPMENT.ordinal());

        applicationEventPublisher.publishEvent(event);

        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(1, postRequestedFor(urlPathEqualTo(webhookPath1))
                    .withRequestBody(containing("\"type\":\"RECORD_UPDATED\"")));
                verify(1, postRequestedFor(urlPathEqualTo(webhookPath2))
                    .withRequestBody(containing("\"type\":\"RECORD_UPDATED\"")));
            });
    }

    @Test
    void testWebhookEventFiltersTypeCorrectly() {
        String webhookPath = "/webhook/filtered";

        stubFor(post(urlPathEqualTo(webhookPath))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("OK")));

        when(dataTableWebhookService.listWebhooks("customers", Environment.DEVELOPMENT.ordinal()))
            .thenReturn(List.of(
                new DataTableWebhookService.Webhook(
                    1L, 1L, BASE_URL + webhookPath, DataTableWebhookType.RECORD_DELETED,
                    Environment.DEVELOPMENT.ordinal())));

        DataTableWebhookEvent event = new DataTableWebhookEvent(
            "customers",
            DataTableWebhookType.RECORD_CREATED,
            Map.of("id", 1L),
            Environment.DEVELOPMENT.ordinal());

        applicationEventPublisher.publishEvent(event);

        Awaitility.await()
            .atMost(1, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(0, postRequestedFor(urlPathEqualTo(webhookPath))));
    }

    @Test
    void testWebhookEventWithNoWebhooks() {
        when(dataTableWebhookService.listWebhooks("empty_table", Environment.DEVELOPMENT.ordinal()))
            .thenReturn(Collections.emptyList());

        DataTableWebhookEvent event = new DataTableWebhookEvent(
            "empty_table",
            DataTableWebhookType.RECORD_CREATED,
            Map.of("id", 1L),
            Environment.DEVELOPMENT.ordinal());

        applicationEventPublisher.publishEvent(event);

        Awaitility.await()
            .during(500, TimeUnit.MILLISECONDS)
            .atMost(1, TimeUnit.SECONDS)
            .until(() -> true);
    }

    @Test
    void testWebhookEventWithRecordDeletedType() {
        String webhookPath = "/webhook/deleted";

        stubFor(post(urlPathEqualTo(webhookPath))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("OK")));

        when(dataTableWebhookService.listWebhooks("records", Environment.DEVELOPMENT.ordinal()))
            .thenReturn(List.of(
                new DataTableWebhookService.Webhook(
                    1L, 1L, BASE_URL + webhookPath, DataTableWebhookType.RECORD_DELETED,
                    Environment.DEVELOPMENT.ordinal())));

        DataTableWebhookEvent event = new DataTableWebhookEvent(
            "records",
            DataTableWebhookType.RECORD_DELETED,
            Map.of("id", 42L),
            Environment.DEVELOPMENT.ordinal());

        applicationEventPublisher.publishEvent(event);

        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(1, postRequestedFor(urlPathEqualTo(webhookPath))
                .withRequestBody(containing("\"type\":\"RECORD_DELETED\""))
                .withRequestBody(containing("\"table\":\"records\""))));
    }

    @Test
    void testWebhookEventIncludesTimestamp() {
        String webhookPath = "/webhook/timestamp";

        stubFor(post(urlPathEqualTo(webhookPath))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("OK")));

        when(dataTableWebhookService.listWebhooks("timestamps", Environment.DEVELOPMENT.ordinal()))
            .thenReturn(List.of(
                new DataTableWebhookService.Webhook(
                    1L, 1L, BASE_URL + webhookPath, DataTableWebhookType.RECORD_CREATED,
                    Environment.DEVELOPMENT.ordinal())));

        DataTableWebhookEvent event = new DataTableWebhookEvent(
            "timestamps",
            DataTableWebhookType.RECORD_CREATED,
            Map.of("id", 1L),
            Environment.DEVELOPMENT.ordinal());

        applicationEventPublisher.publishEvent(event);

        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(1, postRequestedFor(urlPathEqualTo(webhookPath))
                .withRequestBody(containing("\"timestamp\""))));
    }

    @Test
    void testWebhookEventWithDifferentEnvironments() {
        String webhookPath = "/webhook/staging";

        stubFor(post(urlPathEqualTo(webhookPath))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("OK")));

        when(dataTableWebhookService.listWebhooks("staging_table", Environment.STAGING.ordinal()))
            .thenReturn(List.of(
                new DataTableWebhookService.Webhook(
                    1L, 1L, BASE_URL + webhookPath, DataTableWebhookType.RECORD_UPDATED,
                    Environment.STAGING.ordinal())));

        DataTableWebhookEvent event = new DataTableWebhookEvent(
            "staging_table",
            DataTableWebhookType.RECORD_UPDATED,
            Map.of("id", 100L),
            Environment.STAGING.ordinal());

        applicationEventPublisher.publishEvent(event);

        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(1, postRequestedFor(urlPathEqualTo(webhookPath))
                .withRequestBody(containing("\"type\":\"RECORD_UPDATED\""))));
    }

    @Test
    void testWebhookEventPayloadContainsCorrectData() {
        String webhookPath = "/webhook/payload";

        stubFor(post(urlPathEqualTo(webhookPath))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("OK")));

        when(dataTableWebhookService.listWebhooks("payload_test", Environment.DEVELOPMENT.ordinal()))
            .thenReturn(List.of(
                new DataTableWebhookService.Webhook(
                    1L, 1L, BASE_URL + webhookPath, DataTableWebhookType.RECORD_CREATED,
                    Environment.DEVELOPMENT.ordinal())));

        DataTableWebhookEvent event = new DataTableWebhookEvent(
            "payload_test",
            DataTableWebhookType.RECORD_CREATED,
            Map.of("id", 1L, "name", "Test Item", "price", 25.50),
            Environment.DEVELOPMENT.ordinal());

        applicationEventPublisher.publishEvent(event);

        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(1, postRequestedFor(urlPathEqualTo(webhookPath))
                .withRequestBody(containing("\"payload\""))
                .withRequestBody(containing("Test Item"))));
    }
}
