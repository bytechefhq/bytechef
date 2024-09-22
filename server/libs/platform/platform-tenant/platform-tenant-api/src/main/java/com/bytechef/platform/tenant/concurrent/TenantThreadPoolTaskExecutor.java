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

package com.bytechef.platform.tenant.concurrent;

import com.bytechef.platform.tenant.TenantContext;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * @author Ivica Cardic
 */
public class TenantThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    @Override
    public void execute(Runnable task) {
        super.execute(getTenantRunnable(task));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(getTenantRunnable(task));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(getTenantCallable(task));
    }

    @Override
    public ListenableFuture<?> submitListenable(Runnable task) {
        return super.submitListenable(getTenantRunnable(task));
    }

    @Override
    public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
        return super.submitListenable(getTenantCallable(task));
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
