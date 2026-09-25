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
import com.bytechef.platform.data.table.domain.DataTableRef;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.Map;
import org.springframework.context.ApplicationEvent;

/**
 * Application event published when a data table change should trigger webhooks.
 *
 * <p>
 * Carries the table the row was written into rather than the base name it is known by, so the listener resolves the
 * same registry row that registration hung off.
 *
 * @author Ivica Cardic
 */
public class DataTableWebhookEvent extends ApplicationEvent {

    /**
     * {@code SE_BAD_FIELD}: this is an in-process Spring event delivered to a local {@code @EventListener}, never
     * serialized -- the same reason the mutable payload map is held directly.
     */
    @SuppressFBWarnings("SE_BAD_FIELD")
    private final DataTableRef dataTableRef;
    private final Map<String, Object> payload;
    private final DataTableWebhookType type;

    @SuppressFBWarnings("EI")
    public DataTableWebhookEvent(
        DataTableRef dataTableRef, DataTableWebhookType type, Map<String, Object> payload) {

        super(dataTableRef);

        this.dataTableRef = dataTableRef;
        this.type = type;
        this.payload = payload;
    }

    /**
     * The table the changed row lives in, as resolution settled it: environment, base name and the owner whose copy it
     * is. The listener looks registrations up by it, so an event reaches only the registrations of that very table.
     */
    public DataTableRef getDataTableRef() {
        return dataTableRef;
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
            "dataTableRef=" + dataTableRef +
            ", payload=" + payload +
            ", type=" + type +
            "} " + super.toString();
    }
}
