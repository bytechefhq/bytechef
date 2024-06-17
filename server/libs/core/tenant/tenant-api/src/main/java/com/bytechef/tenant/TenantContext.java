/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.tenant;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * @author Ivica Cardic
 */
public class TenantContext {

    public static final String TENANT_PREFIX = "bytechef";

    private static final String DEFAULT_TENANT_ID = "public";

    private static final ThreadLocal<String> currentTenant = ThreadLocal.withInitial(() -> DEFAULT_TENANT_ID);

    public static String getCurrentTenantId() {
        return currentTenant.get();
    }

    public static <V> V callWithTenantId(String tenantId, Callable<V> callable) {
        String curTenantId = getCurrentTenantId();

        try {
            setCurrentTenantId(tenantId);

            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            setCurrentTenantId(curTenantId);
        }
    }

    public static String getCurrentDatabaseSchema() {
        return Objects.equals(getCurrentTenantId(), DEFAULT_TENANT_ID)
            ? DEFAULT_TENANT_ID
            : TENANT_PREFIX + "_" + getCurrentTenantId();
    }

    public static String getDatabaseSchema(String tenantId) {
        return TENANT_PREFIX + "_" + tenantId;
    }

    public static String getTenantId(String schemaName) {
        if (schemaName == null) {
            return "000000";
        } else {
            return schemaName.replace(TENANT_PREFIX + "_", "");
        }
    }

    public static void runWithTenantId(String tenantId, Runnable runnable) {
        String curTenantId = getCurrentTenantId();

        try {
            setCurrentTenantId(tenantId);

            runnable.run();
        } finally {
            setCurrentTenantId(curTenantId);
        }
    }

    public static void setCurrentTenantId(String tenantId) {
        if (tenantId == null) {
            tenantId = DEFAULT_TENANT_ID;
        }

        currentTenant.set(tenantId);
    }
}
