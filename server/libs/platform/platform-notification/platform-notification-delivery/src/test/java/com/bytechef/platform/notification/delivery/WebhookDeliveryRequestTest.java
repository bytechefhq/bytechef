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

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WebhookDeliveryRequestTest {

    @Test
    void testRejectsNullUrl() {
        assertThatThrownBy(() -> WebhookDeliveryRequest.of(null, "job.status", "{}"))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("url");
    }

    @Test
    void testRejectsNullEventType() {
        assertThatThrownBy(() -> WebhookDeliveryRequest.of("https://example.com/hook", null, "{}"))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("eventType");
    }

    @Test
    void testRejectsNullPayloadJson() {
        assertThatThrownBy(() -> WebhookDeliveryRequest.of("https://example.com/hook", "job.status", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("payloadJson");
    }

    @Test
    void testNullHeadersBecomeEmpty() {
        WebhookDeliveryRequest webhookDeliveryRequest = new WebhookDeliveryRequest(
            "https://example.com/hook", "job.status", "{}", null, null);

        assertThat(webhookDeliveryRequest.headers()).isEqualTo(Map.of());
    }
}
