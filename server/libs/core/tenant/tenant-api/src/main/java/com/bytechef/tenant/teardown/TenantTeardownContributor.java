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

package com.bytechef.tenant.teardown;

import java.util.List;

/**
 * Contributes the module-specific part of tenant deletion. Beans carry {@code @Order}; contributors run in ascending
 * order. {@link #onDeletionRequested(String)} and {@link #beforeSchemaDrop(String)} run inside the tenant context as
 * the system user. {@link #afterSchemaDrop(String)} must not query the tenant's tables.
 *
 * @author Ivica Cardic
 */
public interface TenantTeardownContributor {

    /**
     * Stops the tenant's activity when deletion is requested.
     *
     * @return human-readable failure lines, empty when everything succeeded
     */
    default List<String> onDeletionRequested(String tenantId) {
        return List.of();
    }

    default void beforeSchemaDrop(String tenantId) {
    }

    default void afterSchemaDrop(String tenantId) {
    }
}
