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

package com.bytechef.platform.oauth2.authorizationserver.tenant;

import com.bytechef.tenant.TenantContext;
import java.util.function.Supplier;

/**
 * Runs code with a given current tenant and restores the previous one afterwards, rethrowing exceptions unchanged.
 *
 * @author Ivica Cardic
 */
public final class TenantScope {

    private TenantScope() {
    }

    public static <T> T call(String tenantId, Supplier<T> supplier) {
        String previousTenantId = TenantContext.getCurrentTenantId();

        TenantContext.setCurrentTenantId(tenantId);

        try {
            return supplier.get();
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }
    }

    public static void run(String tenantId, Runnable runnable) {
        call(tenantId, () -> {
            runnable.run();

            return null;
        });
    }
}
