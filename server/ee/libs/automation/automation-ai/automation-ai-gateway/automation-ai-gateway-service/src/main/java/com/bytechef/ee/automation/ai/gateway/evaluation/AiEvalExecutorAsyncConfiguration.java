/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.evaluation;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.concurrent.TenantTaskDecorator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiEvalExecutorAsyncConfiguration {

    public static final String AI_EVAL_TASK_EXECUTOR = "aiEvalTaskExecutor";

    private static final String WORKER_EXECUTOR = "workerExecutor";

    @Bean(name = AI_EVAL_TASK_EXECUTOR)
    public AsyncTaskExecutor aiEvalTaskExecutor(
        @Qualifier(WORKER_EXECUTOR) ObjectProvider<AsyncTaskExecutor> workerExecutorProvider) {

        AsyncTaskExecutor delegate = workerExecutorProvider.getIfAvailable(
            AiEvalExecutorAsyncConfiguration::createFallbackExecutor);

        return new DelegatingSecurityContextAsyncTaskExecutor(delegate);
    }

    private static AsyncTaskExecutor createFallbackExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("ai-eval-");

        executor.setTaskDecorator(new TenantTaskDecorator());

        return executor;
    }
}
