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

package com.bytechef.platform.scheduler.db.task;

import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.tenant.TenantContext;
import java.util.function.Supplier;

/**
 * @author Ivica Cardic
 */
public final class ContextBinding {

    private ContextBinding() {
    }

    public static void run(String tenantId, Runnable runnable) {
        TenantContext.runWithTenantId(tenantId, () -> runAsSystem(runnable));
    }

    public static <T> T call(String tenantId, Supplier<T> supplier) {
        return TenantContext.callWithTenantId(tenantId, () -> SecurityUtils.runAsSystem(supplier));
    }

    public static void runAsSystem(Runnable runnable) {
        SecurityUtils.runAsSystem(() -> {
            runnable.run();

            return null;
        });
    }
}
