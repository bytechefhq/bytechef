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
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

/**
 * Application event published when a data table change should trigger webhooks. One write publishes one event holding
 * every row it changed.
 *
 * @author Ivica Cardic
 */
public final class DataTableWebhookEvent extends ApplicationEvent {

    private final String baseName;
    private final long environmentId;
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Published in-process only, never serialized")
    private final List<DataTableRow> rows;
    private final DataTableWebhookType type;

    public DataTableWebhookEvent(
        String baseName, DataTableWebhookType type, List<DataTableRow> rows, long environmentId) {

        super(Objects.requireNonNull(baseName, "baseName must not be null"));

        Objects.requireNonNull(rows, "rows must not be null");

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("rows must not be empty");
        }

        this.baseName = baseName;
        this.environmentId = environmentId;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.rows = rows.stream()
            .map(row -> {
                Objects.requireNonNull(row, "row must not be null");

                return new DataTableRow(
                    row.id(), copy(Objects.requireNonNull(row.values(), "row values must not be null")));
            })
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
    public List<DataTableRow> getRows() {
        return rows;
    }

    @Override
    public String toString() {
        return "DataTableWebhookEvent{" +
            "baseName='" + baseName + '\'' +
            ", environmentId=" + environmentId +
            ", rowCount=" + rows.size() +
            ", type=" + type +
            "} " + super.toString();
    }

    private static Map<String, Object> copy(Map<?, ?> map) {
        Map<String, Object> copy = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            copy.put(
                String.valueOf(Objects.requireNonNull(entry.getKey(), "value keys must not be null")),
                copyValue(entry.getValue()));
        }

        return Collections.unmodifiableMap(copy);
    }

    private static @Nullable Object copyValue(@Nullable Object value) {
        if (value instanceof Map<?, ?> map) {
            return copy(map);
        }

        if (value instanceof Collection<?> collection) {
            return collection.stream()
                .map(DataTableWebhookEvent::copyValue)
                .toList();
        }

        return value;
    }
}
