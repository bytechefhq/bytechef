/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.config;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.concurrent.TenantTaskDecorator;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;

/**
 * @author Ivica Cardic
 * @version ee
 */
@AutoConfiguration
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiEvalExperimentAsyncConfiguration {

    @Bean(name = "aiEvalExperimentTaskExecutor")
    public ThreadPoolTaskExecutor aiEvalExperimentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-experiment-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(securityAndTenantTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        return executor;
    }

    static TaskDecorator securityAndTenantTaskDecorator() {
        TenantTaskDecorator tenantTaskDecorator = new TenantTaskDecorator();

        return runnable -> DelegatingSecurityContextRunnable.create(tenantTaskDecorator.decorate(runnable), null);
    }
}
