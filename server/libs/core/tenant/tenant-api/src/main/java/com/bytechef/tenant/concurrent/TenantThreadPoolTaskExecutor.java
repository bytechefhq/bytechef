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

package com.bytechef.tenant.concurrent;

import com.bytechef.tenant.TenantContext;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.jspecify.annotations.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Ivica Cardic
 */
public class TenantThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    @Override
    public void execute(@NonNull Runnable task) {
        super.execute(getTenantRunnable(task));
    }

    @Override
    public @NonNull Future<?> submit(@NonNull Runnable task) {
        return super.submit(getTenantRunnable(task));
    }

    @Override
    public <T> @NonNull Future<T> submit(@NonNull Callable<T> task) {
        return super.submit(getTenantCallable(task));
    }

    private Runnable getTenantRunnable(Runnable task) {
        String tenantId = TenantContext.getCurrentTenantId();

        return () -> {
            String currentTenantId = TenantContext.getCurrentTenantId();

            try {
                TenantContext.setCurrentTenantId(tenantId);

                task.run();
            } finally {
                TenantContext.setCurrentTenantId(currentTenantId);
            }
        };
    }

    private <V> Callable<V> getTenantCallable(Callable<V> task) {
        String tenantId = TenantContext.getCurrentTenantId();

        return () -> {
            String currentTenantId = TenantContext.getCurrentTenantId();

            try {
                TenantContext.setCurrentTenantId(tenantId);

                return task.call();
            } finally {
                TenantContext.setCurrentTenantId(currentTenantId);
            }
        };
    }
}
