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

package com.bytechef.platform.data.table.execution.event;

import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.ApplicationEvent;

/**
 * Application event published when a data table change should trigger webhooks. A bulk change publishes one event
 * holding a payload per changed row.
 *
 * @author Ivica Cardic
 */
public final class DataTableWebhookEvent extends ApplicationEvent {

    private final String baseName;
    private final long environmentId;
    private final List<Map<String, Object>> payloads;
    private final DataTableWebhookType type;

    @SuppressFBWarnings("EI")
    public DataTableWebhookEvent(
        String baseName, DataTableWebhookType type, Map<String, Object> payload, long environmentId) {

        this(baseName, type, List.of(payload), environmentId);
    }

    @SuppressFBWarnings("EI")
    public DataTableWebhookEvent(
        String baseName, DataTableWebhookType type, List<Map<String, Object>> payloads, long environmentId) {

        super(Objects.requireNonNull(baseName, "baseName must not be null"));

        Objects.requireNonNull(payloads, "payloads must not be null");

        if (payloads.isEmpty()) {
            throw new IllegalArgumentException("payloads must not be empty");
        }

        this.baseName = baseName;
        this.environmentId = environmentId;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.payloads = payloads.stream()
            .map(payload -> Collections.unmodifiableMap(
                new LinkedHashMap<>(Objects.requireNonNull(payload, "payload must not be null"))))
            .toList();
    }

    public String getBaseName() {
        return baseName;
    }

    public long getEnvironmentId() {
        return environmentId;
    }

    public DataTableWebhookType getType() {
        return type;
    }

    @SuppressFBWarnings("EI")
    public List<Map<String, Object>> getPayloads() {
        return payloads;
    }

    @Override
    public String toString() {
        return "DataTableWebhookEvent{" +
            "baseName='" + baseName + '\'' +
            ", environmentId=" + environmentId +
            ", payloads=" + payloads +
            ", type=" + type +
            "} " + super.toString();
    }
}
