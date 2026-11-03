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

import com.bytechef.tenant.TenantContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WebhookDeliveryTest {

    @Test
    void testBuilderDefaultsTenantIdFromTenantContext() {
        AtomicReference<String> tenantIdReference = new AtomicReference<>();

        TenantContext.runWithTenantId(
            "t1", () -> tenantIdReference.set(
                WebhookDelivery.builder("source", "https://example.com")
                    .description("description")
                    .payload("label", "{}")
                    .build()
                    .tenantId()));

        assertThat(tenantIdReference.get()).isEqualTo("t1");
    }

    @Test
    void testRejectsBlankSourceDescriptionAndUrl() {
        assertThatThrownBy(
            () -> WebhookDelivery.builder(" ", "https://example.com")
                .description("description")
                .payload("label", "{}")
                .build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
            () -> WebhookDelivery.builder("source", "https://example.com")
                .description(" ")
                .payload("label", "{}")
                .build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
            () -> WebhookDelivery.builder("source", "")
                .description("description")
                .payload("label", "{}")
                .build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRejectsEmptyPayloads() {
        assertThatThrownBy(
            () -> WebhookDelivery.builder("source", "https://example.com")
                .description("description")
                .build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRejectsBlankPayloadJson() {
        assertThatThrownBy(() -> new WebhookPayload("label", " ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCopiesHeadersAndPayloads() {
        Map<String, String> headers = new HashMap<>(Map.of("a", "b"));
        List<WebhookPayload> payloads = new ArrayList<>(List.of(new WebhookPayload("label", "{}")));

        WebhookDelivery webhookDelivery = WebhookDelivery.builder("source", "https://example.com")
            .description("description")
            .headers(headers)
            .payloads(payloads)
            .build();

        headers.put("c", "d");
        payloads.add(new WebhookPayload("other", "{}"));

        assertThat(webhookDelivery.headers()).containsOnlyKeys("a");
        assertThat(webhookDelivery.payloads()).hasSize(1);
    }

    @Test
    void testHostIsTheOnlyPartOfTheUrlExposedForLogging() {
        WebhookDelivery webhookDelivery = WebhookDelivery.builder(
            "source", "https://user:password@hooks.example.com:8443/path?token=SECRET")
            .description("description")
            .payload("label", "{}")
            .build();

        assertThat(webhookDelivery.host()).isEqualTo("hooks.example.com");

        WebhookDelivery unencodedWebhookDelivery = WebhookDelivery.builder(
            "source", "https://hooks.example.com/path?status=a b|c")
            .description("description")
            .payload("label", "{}")
            .build();

        assertThat(unencodedWebhookDelivery.host()).isEqualTo("hooks.example.com");

        WebhookDelivery underscoreWebhookDelivery = WebhookDelivery.builder(
            "source", "https://my_hooks.example.com/path")
            .description("description")
            .payload("label", "{}")
            .build();

        assertThat(underscoreWebhookDelivery.host()).isEqualTo("my_hooks.example.com");
    }

    @Test
    void testToStringRedactsHeaderValuesAndSecret() {
        WebhookDelivery webhookDelivery = WebhookDelivery.builder(
            "source", "https://user:password@hooks.example.com:8443/path?token=URL-SECRET")
            .description("description")
            .headers(Map.of("Authorization", "Bearer header-value"))
            .secret("whsec_signing-secret")
            .payload("label", "{}")
            .build();

        String deliveryString = webhookDelivery.toString();

        assertThat(deliveryString).contains("hooks.example.com", "Authorization", "[redacted]")
            .doesNotContain("header-value", "whsec_signing-secret", "URL-SECRET", "password", "/path");
    }

    @Test
    void testRetryNoneMakesNoRetries() {
        assertThat(WebhookRetry.none()
            .maxRetries()).isEqualTo(0);
    }

    @Test
    void testOutcomeCountsAndDropped() {
        WebhookDeliveryOutcome webhookDeliveryOutcome = WebhookDeliveryOutcome.dropped(
            List.of(new WebhookPayload("a", "{}"), new WebhookPayload("b", "{}")));

        assertThat(webhookDeliveryOutcome.count(WebhookDeliveryOutcome.Status.DROPPED)).isEqualTo(2);
        assertThat(webhookDeliveryOutcome.anySent()).isFalse();
    }
}
