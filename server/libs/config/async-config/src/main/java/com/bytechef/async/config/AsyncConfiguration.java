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

package com.bytechef.async.config;

import com.bytechef.tenant.concurrent.TenantTaskDecorator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.Executor;
import org.jspecify.annotations.NonNull;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfiguration implements AsyncConfigurer {

    public static final String MESSAGE_EVENT_EXECUTOR = "messageEventExecutor";
    public static final String TASK_EXECUTOR = "taskExecutor";
    public static final String WORKER_EXECUTOR = "workerExecutor";
    private static final int DEFAULT_WORKER_CONCURRENCY = 10;
    private static final String WORKER_CONCURRENCY_PROPERTY = "bytechef.worker.task.subscriptions.default";
    private final Environment environment;
    private final TaskDecorator taskDecorator;
    private final TaskExecutionProperties taskExecutionProperties;

    @SuppressFBWarnings("EI")
    public AsyncConfiguration(
        ContextPropagatingTaskDecorator contextPropagatingTaskDecorator, Environment environment,
        TaskExecutionProperties taskExecutionProperties) {

        TenantTaskDecorator tenantTaskDecorator = new TenantTaskDecorator();

        this.environment = environment;
        this.taskDecorator = runnable -> tenantTaskDecorator.decorate(
            contextPropagatingTaskDecorator.decorate(runnable));
        this.taskExecutionProperties = taskExecutionProperties;
    }

    @Override
    @Bean(name = TASK_EXECUTOR)
    @Primary
    public TaskExecutor getAsyncExecutor() {
        TaskExecutionProperties.Simple simple = taskExecutionProperties.getSimple();

        Integer concurrencyLimit = simple.getConcurrencyLimit();

        return createExecutor(
            taskExecutionProperties.getThreadNamePrefix(),
            concurrencyLimit == null ? SimpleAsyncTaskExecutor.UNBOUNDED_CONCURRENCY : concurrencyLimit);
    }

    @Bean(name = MESSAGE_EVENT_EXECUTOR)
    TaskExecutor messageEventExecutor() {
        return createExecutor("message-event-", SimpleAsyncTaskExecutor.UNBOUNDED_CONCURRENCY);
    }

    @Bean(name = WORKER_EXECUTOR)
    TaskExecutor workerExecutor() {
        int concurrencyLimit = environment.getProperty(
            WORKER_CONCURRENCY_PROPERTY, Integer.class, DEFAULT_WORKER_CONCURRENCY);

        return createExecutor("worker-", concurrencyLimit);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    @Bean
    protected WebMvcConfigurer webMvcConfigurer(@Qualifier(TASK_EXECUTOR) Executor executor) {
        return new WebMvcConfigurer() {

            @Override
            public void configureAsyncSupport(@NonNull AsyncSupportConfigurer configurer) {
                configurer.setTaskExecutor((AsyncTaskExecutor) executor);
            }
        };
    }

    SimpleAsyncTaskExecutor createExecutor(String threadNamePrefix, int concurrencyLimit) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(threadNamePrefix);

        executor.setConcurrencyLimit(concurrencyLimit);
        executor.setTaskDecorator(taskDecorator);
        executor.setVirtualThreads(Threading.VIRTUAL.isActive(environment));

        return executor;
    }
}
