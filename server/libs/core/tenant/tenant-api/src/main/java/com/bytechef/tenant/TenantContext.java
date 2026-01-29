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

package com.bytechef.tenant;

import com.bytechef.tenant.constant.TenantConstants;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class TenantContext {

    public static final String CURRENT_TENANT_ID = "CURRENT_TENANT_ID";
    public static final String DEFAULT_TENANT_ID = "public";

    private static final ThreadLocal<String> currentTenant = ThreadLocal.withInitial(() -> DEFAULT_TENANT_ID);

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
            : TenantConstants.TENANT_PREFIX + "_" + getCurrentTenantId();
    }

    public static String getCurrentDatabaseSchema(String suffix) {
        return Objects.equals(getCurrentTenantId(), DEFAULT_TENANT_ID)
            ? DEFAULT_TENANT_ID
            : TenantConstants.TENANT_PREFIX + "_" + suffix + "_" + getCurrentTenantId();
    }

    public static String getCurrentTenantId() {
        return currentTenant.get();
    }

    public static void resetCurrentTenantId() {
        setCurrentTenantId(DEFAULT_TENANT_ID);
    }

    public static void runWithTenantId(String tenantId, Runnable runnable) {
        String curTenantId = getCurrentTenantId();

        try {
            setCurrentTenantId(tenantId);

            try {
                runnable.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            setCurrentTenantId(curTenantId);
        }
    }

    public static void setCurrentTenantId(String tenantId) {
        Assert.notNull(tenantId, "tenantId must not be null");

        currentTenant.set(tenantId);
    }

    @FunctionalInterface
    public interface Runnable {

        void run() throws Exception;
    }
}
