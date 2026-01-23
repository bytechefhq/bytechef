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

package com.bytechef.automation.data.table.execution.event;

import com.bytechef.automation.data.table.configuration.domain.DataTableWebhookType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.Map;
import org.springframework.context.ApplicationEvent;

/**
 * Application event published when a data table change should trigger webhooks.
 *
 * @author Ivica Cardic
 */
public class DataTableWebhookEvent extends ApplicationEvent {

    private final String baseName;
    private final long environmentId;
    private final Map<String, Object> payload;
    private final DataTableWebhookType type;

    @SuppressFBWarnings("EI")
    public DataTableWebhookEvent(
        String baseName, DataTableWebhookType type, Map<String, Object> payload, long environmentId) {

        super(baseName);

        this.baseName = baseName;
        this.environmentId = environmentId;
        this.type = type;
        this.payload = payload;
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

    public Map<String, Object> getPayload() {
        return Collections.unmodifiableMap(payload);
    }

    @Override
    public String toString() {
        return "DataTableWebhookEvent{" +
            "baseName='" + baseName + '\'' +
            ", environmentId=" + environmentId +
            ", payload=" + payload +
            ", type=" + type +
            "} " + super.toString();
    }
}
