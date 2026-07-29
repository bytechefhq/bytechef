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

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * One outbound webhook delivery. {@code payloadJson} is the complete, already-serialized JSON body — callers own the
 * payload shape; this layer owns transport, SSRF validation, headers, and signing.
 *
 * @param url         destination URL; validated against private/internal addresses before any connection is made
 * @param eventType   event discriminator sent as the {@code X-ByteChef-Event} header (e.g. {@code job.status},
 *                    {@code ai-observability.alert})
 * @param payloadJson the JSON request body
 * @param headers     extra headers to add verbatim (may be empty)
 * @param secret      optional HMAC secret; when present the delivery carries an {@code X-ByteChef-Signature} header of
 *                    the form {@code t=<epochMillis>,v1=<hex(HMAC-SHA256(secret, "<t>.<body>"))>}
 *
 * @author Ivica Cardic
 */
public record WebhookDeliveryRequest(
    String url, String eventType, String payloadJson, Map<String, String> headers, @Nullable String secret) {

    public WebhookDeliveryRequest {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public static WebhookDeliveryRequest of(String url, String eventType, String payloadJson) {
        return new WebhookDeliveryRequest(url, eventType, payloadJson, Map.of(), null);
    }
}
