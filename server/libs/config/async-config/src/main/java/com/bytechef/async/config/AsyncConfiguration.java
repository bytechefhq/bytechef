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

package com.bytechef.async.config;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfiguration implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

    private final TaskExecutionProperties taskExecutionProperties;

    @SuppressFBWarnings("EI")
    public AsyncConfiguration(TaskExecutionProperties taskExecutionProperties) {
        this.taskExecutionProperties = taskExecutionProperties;
    }

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        log.debug("Creating Async Task Executor");

        ThreadPoolTaskExecutor executor = new TenantThreadPoolTaskExecutor();

        TaskExecutionProperties.Pool pool = taskExecutionProperties.getPool();

        executor.setCorePoolSize(pool.getCoreSize());
        executor.setMaxPoolSize(pool.getMaxSize());
        executor.setQueueCapacity(pool.getQueueCapacity());
        executor.setThreadNamePrefix(taskExecutionProperties.getThreadNamePrefix());

        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    private static class TenantThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {
        @Override
        public void execute(Runnable task) {
            super.execute(getTenantRunnable(task));
        }

        @Override
        public void execute(Runnable task, long startTimeout) {
            super.execute(getTenantRunnable(task), startTimeout);
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
}
