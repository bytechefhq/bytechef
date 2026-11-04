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

import com.bytechef.tenant.TenantContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record WebhookDelivery(
    String source, String description, String tenantId, String url, @Nullable String eventType,
    Map<String, String> headers, @Nullable String secret, boolean validateUrl, List<WebhookPayload> payloads,
    WebhookRetry retry, @Nullable String deliveryId, @Nullable Instant timestamp) {

    public WebhookDelivery {
        requireNotBlank(source, "source");
        requireNotBlank(description, "description");
        requireNotBlank(url, "url");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(headers, "headers");
        Objects.requireNonNull(payloads, "payloads");
        Objects.requireNonNull(retry, "retry");

        headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        payloads = List.copyOf(payloads);

        if (payloads.isEmpty()) {
            throw new IllegalArgumentException("payloads must not be empty");
        }
    }

    public static Builder builder(String source, String url) {
        return new Builder(source, url);
    }

    public String host() {
        return WebhookDeliveryMessages.host(url);
    }

    @Override
    public String toString() {
        return "WebhookDelivery[source=" + source + ", description=" + description + ", tenantId=" + tenantId +
            ", host=" + host() + ", eventType=" + eventType + ", headerNames=" + headers.keySet() + ", secret=" +
            (secret == null ? null : "[redacted]") + ", validateUrl=" + validateUrl + ", payloadCount=" +
            payloads.size() + ", retry=" + retry + ", deliveryId=" + deliveryId + ", timestamp=" + timestamp + "]";
    }

    private static void requireNotBlank(String value, String name) {
        Objects.requireNonNull(value, name);

        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    public static final class Builder {

        private final String source;
        private final String url;
        private String description = "";
        private String tenantId = TenantContext.getCurrentTenantId();
        private @Nullable String eventType;
        private Map<String, String> headers = Map.of();
        private @Nullable String secret;
        private boolean validateUrl = true;
        private final List<WebhookPayload> payloads = new ArrayList<>();
        private WebhookRetry retry = WebhookRetry.none();
        private @Nullable String deliveryId;
        private @Nullable Instant timestamp;

        private Builder(String source, String url) {
            this.source = source;
            this.url = url;
        }

        public Builder description(String description) {
            this.description = description;

            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;

            return this;
        }

        public Builder eventType(@Nullable String eventType) {
            this.eventType = eventType;

            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));

            return this;
        }

        public Builder secret(@Nullable String secret) {
            this.secret = secret;

            return this;
        }

        public Builder validateUrl(boolean validateUrl) {
            this.validateUrl = validateUrl;

            return this;
        }

        public Builder payloads(List<WebhookPayload> payloads) {
            this.payloads.clear();
            this.payloads.addAll(payloads);

            return this;
        }

        public Builder payload(String label, String json) {
            this.payloads.add(new WebhookPayload(label, json));

            return this;
        }

        public Builder retry(WebhookRetry retry) {
            this.retry = retry;

            return this;
        }

        public Builder deliveryId(@Nullable String deliveryId) {
            this.deliveryId = deliveryId;

            return this;
        }

        public Builder timestamp(@Nullable Instant timestamp) {
            this.timestamp = timestamp;

            return this;
        }

        public WebhookDelivery build() {
            return new WebhookDelivery(
                source, description, tenantId, url, eventType, headers, secret, validateUrl, payloads, retry,
                deliveryId, timestamp);
        }
    }
}
