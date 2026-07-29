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

import org.junit.jupiter.api.Test;

/**
 * SSRF validation and signature-scheme tests. Actual HTTP delivery cannot be exercised here — the URL validator
 * correctly rejects loopback addresses, so a local test server is unreachable by design.
 *
 * @author Ivica Cardic
 */
class WebhookNotificationClientTest {

    private final WebhookNotificationClient webhookNotificationClient = new WebhookNotificationClient();

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
        // Deterministic vector: HMAC-SHA256("secret", "12345.{}") — pinned so the wire contract documented for
        // receivers ("verify over '<t>.<rawBody>'") cannot silently drift.
        String signature = WebhookNotificationClient.sign("secret", "12345.{}");

        assertThat(signature).isEqualTo("4df28e080fc082b61331789f00e00e122571a07d78931a85ae3d327292701d4e");
    }
}
