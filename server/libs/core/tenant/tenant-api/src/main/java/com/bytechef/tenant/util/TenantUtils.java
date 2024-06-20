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

package com.bytechef.tenant.util;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import java.util.concurrent.Callable;

/**
 * @author Ivica Cardic
 */
public class TenantUtils {

    public static <V> V callWithTenantId(String tenantId, Callable<V> callable) {
        String curTenantId = TenantContext.getCurrentTenantId();

        try {
            TenantContext.setCurrentTenantId(tenantId);

            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            TenantContext.setCurrentTenantId(curTenantId);
        }
    }

    public static String getDatabaseSchema(String tenantId) {
        return TenantConstants.TENANT_PREFIX + "_" + tenantId;
    }

    public static String getTenantId(String schemaName) {
        if (schemaName == null) {
            return "000000";
        } else {
            return schemaName.replace(TenantConstants.TENANT_PREFIX + "_", "");
        }
    }

    public static void runWithTenantId(String tenantId, Runnable runnable) {
        String curTenantId = TenantContext.getCurrentTenantId();

        try {
            TenantContext.setCurrentTenantId(tenantId);

            try {
                runnable.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            TenantContext.setCurrentTenantId(curTenantId);
        }
    }

    @FunctionalInterface
    public interface Runnable {

        void run() throws Exception;
    }
}
