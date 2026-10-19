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

package com.bytechef.ai.chat.memory.inmemory.config;

import com.bytechef.tenant.teardown.TenantTeardownContributor;
import com.bytechef.tenant.util.TenantIdUtils;
import org.springframework.core.annotation.Order;

/**
 * @author Ivica Cardic
 */
@Order(1200)
class InMemoryChatMemoryTenantTeardownContributor implements TenantTeardownContributor {

    private final TenantRoutingInMemoryChatMemoryRepository repository;

    InMemoryChatMemoryTenantTeardownContributor(TenantRoutingInMemoryChatMemoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void afterSchemaDrop(String tenantId) {
        repository.evictTenant(TenantIdUtils.requireValid(tenantId));
    }
}
