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

package com.bytechef.tenant.event;

import org.springframework.context.ApplicationEvent;

/**
 * Application event published when a new tenant schema is created. Listeners can react to this event to initialize
 * tenant-specific resources in external data sources.
 *
 * @author Ivica Cardic
 */
public class TenantSchemaCreatedEvent extends ApplicationEvent {

    private final String tenantId;

    public TenantSchemaCreatedEvent(String tenantId) {
        super(tenantId);

        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String toString() {
        return "TenantSchemaCreatedEvent{" +
            "tenantId='" + tenantId + '\'' +
            "} " + super.toString();
    }
}
