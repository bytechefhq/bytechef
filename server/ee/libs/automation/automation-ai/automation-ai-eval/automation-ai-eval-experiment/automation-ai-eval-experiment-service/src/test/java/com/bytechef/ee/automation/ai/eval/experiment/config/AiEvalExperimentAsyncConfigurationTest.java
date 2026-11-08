/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalExperimentAsyncConfigurationTest {

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testPropagatesSubmitterSecurityAndTenantContextToWorkerThread() throws InterruptedException {
        AiEvalExperimentAsyncConfiguration configuration = new AiEvalExperimentAsyncConfiguration();
        ThreadPoolTaskExecutor executor = configuration.aiEvalExperimentTaskExecutor();

        try {
            Authentication submitterAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                "admin@localhost.com", null, List.of());

            SecurityContext submitterContext = SecurityContextHolder.createEmptyContext();

            submitterContext.setAuthentication(submitterAuthentication);

            SecurityContextHolder.setContext(submitterContext);

            TenantContext.setCurrentTenantId("tenant_a");

            CountDownLatch completedLatch = new CountDownLatch(1);
            AtomicReference<Authentication> observedAuthentication = new AtomicReference<>();
            AtomicReference<String> observedTenantId = new AtomicReference<>();
            AtomicReference<Thread> observedThread = new AtomicReference<>();

            executor.execute(() -> {
                SecurityContext workerContext = SecurityContextHolder.getContext();

                observedAuthentication.set(workerContext.getAuthentication());
                observedTenantId.set(TenantContext.getCurrentTenantId());
                observedThread.set(Thread.currentThread());

                completedLatch.countDown();
            });

            assertThat(completedLatch.await(10, TimeUnit.SECONDS)).isTrue();

            assertThat(observedThread.get()).isNotSameAs(Thread.currentThread());
            assertThat(observedAuthentication.get()).isSameAs(submitterAuthentication);
            assertThat(observedTenantId.get()).isEqualTo("tenant_a");

            AtomicReference<Authentication> leftoverAuthentication = new AtomicReference<>();
            CountDownLatch leftoverLatch = new CountDownLatch(1);

            SecurityContextHolder.clearContext();

            executor.execute(() -> {
                SecurityContext workerContext = SecurityContextHolder.getContext();

                leftoverAuthentication.set(workerContext.getAuthentication());

                leftoverLatch.countDown();
            });

            assertThat(leftoverLatch.await(10, TimeUnit.SECONDS)).isTrue();

            assertThat(leftoverAuthentication.get()).isNull();
        } finally {
            executor.getThreadPoolExecutor()
                .shutdownNow();
        }
    }

    @Test
    void testRejectsExcessiveTasksByRunningOnCallerThread() throws InterruptedException {
        AiEvalExperimentAsyncConfiguration configuration = new AiEvalExperimentAsyncConfiguration();
        ThreadPoolTaskExecutor executor = configuration.aiEvalExperimentTaskExecutor();

        try {
            CountDownLatch holdLatch = new CountDownLatch(1);

            CountDownLatch startedLatch = new CountDownLatch(8);

            Runnable blockingTask = () -> {
                startedLatch.countDown();

                try {
                    holdLatch.await();
                } catch (InterruptedException ignored) {
                    Thread.currentThread()
                        .interrupt();
                }
            };

            for (int taskIndex = 0; taskIndex < 108; taskIndex++) {
                executor.submit(blockingTask);
            }

            boolean saturated = startedLatch.await(10, TimeUnit.SECONDS);

            assertThat(saturated)
                .as("All 8 worker threads should start executing within the timeout")
                .isTrue();

            ThreadPoolExecutor underlying = executor.getThreadPoolExecutor();

            assertThat(underlying.getActiveCount()).as("All 8 worker threads should be active")
                .isEqualTo(8);
            assertThat(underlying.getQueue()
                .size()).as("Queue should be at capacity (100)")
                    .isEqualTo(100);

            AtomicReference<Thread> runningThread = new AtomicReference<>();
            Thread submittingThread = Thread.currentThread();

            executor.submit(() -> runningThread.set(Thread.currentThread()));

            assertThat(runningThread.get())
                .as("CallerRunsPolicy must execute the rejected task on the submitting thread")
                .isSameAs(submittingThread);

            holdLatch.countDown();
        } finally {
            executor.getThreadPoolExecutor()
                .shutdownNow();
        }
    }
}
