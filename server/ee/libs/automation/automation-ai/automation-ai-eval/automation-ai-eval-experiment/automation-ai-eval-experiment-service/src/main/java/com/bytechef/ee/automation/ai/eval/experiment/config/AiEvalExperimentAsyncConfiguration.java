/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.config;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Registers a bounded {@link ThreadPoolTaskExecutor} for the experiment executor so concurrent
 * {@code POST /experiments} calls do not exhaust JVM threads under load. Spring's default
 * {@code SimpleAsyncTaskExecutor} spawns a fresh thread per invocation with no upper bound — fine for one-off tests,
 * dangerous in production with sustained traffic.
 *
 * <p>
 * Pool sizing: {@code corePoolSize=2, maxPoolSize=8, queueCapacity=100}. Rationale — most experiments are LLM-bound
 * (latency dominated by upstream provider, not local CPU), so a small pool with a bounded queue handles the typical
 * "10s of concurrent experiments per gateway node" load without contention. The {@code CallerRunsPolicy} rejection
 * handler back-pressures the calling thread when the queue fills, surfacing as a slow {@code POST /experiments} rather
 * than silent task loss.
 *
 * <p>
 * <strong>Scope:</strong> this executor is exposed only as the named bean {@code aiEvalExperimentTaskExecutor}.
 * {@code @EnableAsync} is intentionally NOT declared here — the project-wide {@code AsyncConfiguration} in
 * {@code server/libs/config/async-config/} already enables async proxying for the entire {@code ApplicationContext}.
 * Adding a second {@code @EnableAsync} would register Spring's async AOP advisor twice (the docs note the annotation
 * "should be applied once" for the AOP weaver), and the resulting nested-proxy behavior is unspecified. Experiment
 * methods bind to this pool by qualifying the executor explicitly — {@code @Async("aiEvalExperimentTaskExecutor")} — so
 * the experiment subsystem is unaffected by whatever {@code AsyncConfigurer.getAsyncExecutor()} returns globally.
 *
 * <p>
 * The {@code AsyncUncaughtExceptionHandler} for uncaught exceptions in {@code @Async} methods is provided by Spring's
 * default {@code SimpleAsyncUncaughtExceptionHandler}; the {@code AiEvalExperimentExecutor} catch-all already converges
 * the experiment lifecycle on failure, so no custom handler is necessary here.
 *
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
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        return executor;
    }
}
