/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.bytechef.ee.platform.external.invocation.log.config.ExternalInvocationLogIntTestConfiguration;
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.repository.ExternalInvocationLogRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationEvent;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        ExternalInvocationLogIntTestConfiguration.class,
        ExternalInvocationLogEventListenerIntTest.AsyncTestConfiguration.class
    },
    properties = "bytechef.edition=ee")
class ExternalInvocationLogEventListenerIntTest {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private AsyncTestConfiguration asyncTestConfiguration;

    @Autowired
    private ExternalInvocationLogRepository externalInvocationLogRepository;

    @BeforeEach
    void beforeEach() {
        asyncTestConfiguration.reset();
    }

    @AfterEach
    void afterEach() {
        externalInvocationLogRepository.deleteAll();
    }

    @Test
    void testPublishedEventIsPersistedOnTheWorkerExecutor() {
        ExternalInvocationEvent externalInvocationEvent = ExternalInvocationEvent
            .builder(
                PlatformType.EMBEDDED, ExternalInvocationSurface.MCP_EMBEDDED, ExternalInvocationKind.COMPONENT,
                "slack_sendMessage")
            .componentName("slack")
            .componentVersion(1)
            .operationName("sendMessage")
            .connectionId(7L)
            .environment(2)
            .workspaceId(4L)
            .externalUserId("user-1")
            .connectedUserId(8L)
            .integrationInstanceId(9L)
            .mcpServerId(3L)
            .a2aServerId(4L)
            .jobId(11L)
            .outcome(ExternalInvocationOutcome.ERROR)
            .errorType("IllegalStateException")
            .errorMessage("failed")
            .durationMs(42)
            .build();

        applicationEventPublisher.publishEvent(externalInvocationEvent);

        await().atMost(Duration.ofSeconds(10))
            .until(() -> externalInvocationLogRepository.count() == 1);

        List<ExternalInvocationLog> externalInvocationLogs = externalInvocationLogRepository.findAll();

        ExternalInvocationLog externalInvocationLog = externalInvocationLogs.getFirst();

        assertThat(externalInvocationLog.getType()).isEqualTo(PlatformType.EMBEDDED);
        assertThat(externalInvocationLog.getSurface()).isEqualTo(ExternalInvocationSurface.MCP_EMBEDDED);
        assertThat(externalInvocationLog.getKind()).isEqualTo(ExternalInvocationKind.COMPONENT);
        assertThat(externalInvocationLog.getToolName()).isEqualTo("slack_sendMessage");
        assertThat(externalInvocationLog.getComponentName()).isEqualTo("slack");
        assertThat(externalInvocationLog.getComponentVersion()).isEqualTo(1);
        assertThat(externalInvocationLog.getOperationName()).isEqualTo("sendMessage");
        assertThat(externalInvocationLog.getConnectionId()).isEqualTo(7L);
        assertThat(externalInvocationLog.getEnvironment()).isEqualTo(2);
        assertThat(externalInvocationLog.getWorkspaceId()).isEqualTo(4L);
        assertThat(externalInvocationLog.getExternalUserId()).isEqualTo("user-1");
        assertThat(externalInvocationLog.getConnectedUserId()).isEqualTo(8L);
        assertThat(externalInvocationLog.getIntegrationInstanceId()).isEqualTo(9L);
        assertThat(externalInvocationLog.getMcpServerId()).isEqualTo(3L);
        assertThat(externalInvocationLog.getA2aServerId()).isEqualTo(4L);
        assertThat(externalInvocationLog.getJobId()).isEqualTo(11L);
        assertThat(externalInvocationLog.getOutcome()).isEqualTo(ExternalInvocationOutcome.ERROR);
        assertThat(externalInvocationLog.getErrorType()).isEqualTo("IllegalStateException");
        assertThat(externalInvocationLog.getErrorMessage()).isEqualTo("failed");
        assertThat(externalInvocationLog.getDurationMs()).isEqualTo(42);
        assertThat(externalInvocationLog.getCreatedDate()).isNotNull();

        assertThat(asyncTestConfiguration.workerExecutorCount()).isEqualTo(1);
        assertThat(asyncTestConfiguration.taskExecutorCount()).isZero();
    }

    @Test
    void testOverlongNamesAndErrorMessageAreTruncatedToTheirColumnSizes() {
        ExternalInvocationEvent externalInvocationEvent = ExternalInvocationEvent
            .builder(PlatformType.AUTOMATION, ExternalInvocationSurface.A2A, ExternalInvocationKind.WORKFLOW,
                "s".repeat(300))
            .componentName("c".repeat(300))
            .operationName("o".repeat(300))
            .outcome(ExternalInvocationOutcome.ERROR)
            .errorType("t".repeat(300))
            .errorMessage("m".repeat(600))
            .build();

        applicationEventPublisher.publishEvent(externalInvocationEvent);

        await().atMost(Duration.ofSeconds(10))
            .until(() -> externalInvocationLogRepository.count() == 1);

        ExternalInvocationLog externalInvocationLog = externalInvocationLogRepository.findAll()
            .getFirst();

        assertThat(externalInvocationLog.getToolName()).isEqualTo("s".repeat(256));
        assertThat(externalInvocationLog.getComponentName()).isEqualTo("c".repeat(256));
        assertThat(externalInvocationLog.getOperationName()).isEqualTo("o".repeat(256));
        assertThat(externalInvocationLog.getErrorType()).isEqualTo("t".repeat(256));
        assertThat(externalInvocationLog.getErrorMessage()).isEqualTo("m".repeat(512));
    }

    @Test
    void testPersistenceFailureIsSwallowed() {
        ExternalInvocationEvent externalInvocationEvent = ExternalInvocationEvent
            .builder(
                PlatformType.EMBEDDED, ExternalInvocationSurface.EMBEDDED_API_ACTION, ExternalInvocationKind.COMPONENT,
                "slack_sendMessage")
            .externalUserId("x".repeat(300))
            .outcome(ExternalInvocationOutcome.SUCCESS)
            .build();

        applicationEventPublisher.publishEvent(externalInvocationEvent);

        await().atMost(Duration.ofSeconds(10))
            .until(() -> asyncTestConfiguration.workerExecutorCount() == 1);

        assertThat(asyncTestConfiguration.uncaughtExceptions()).isEmpty();
        assertThat(externalInvocationLogRepository.count()).isZero();
    }

    @EnableAsync
    static class AsyncTestConfiguration {

        private final AtomicInteger taskExecutorCount = new AtomicInteger();
        private final List<Throwable> uncaughtExceptions = new CopyOnWriteArrayList<>();
        private final AtomicInteger workerExecutorCount = new AtomicInteger();

        @Bean
        AsyncConfigurer asyncConfigurer() {
            return new AsyncConfigurer() {

                @Override
                public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
                    return (throwable, method, params) -> uncaughtExceptions.add(throwable);
                }
            };
        }

        @Bean
        TaskExecutor taskExecutor() {
            return countingExecutor(taskExecutorCount);
        }

        @Bean
        TaskExecutor workerExecutor() {
            return countingExecutor(workerExecutorCount);
        }

        int taskExecutorCount() {
            return taskExecutorCount.get();
        }

        List<Throwable> uncaughtExceptions() {
            return uncaughtExceptions;
        }

        int workerExecutorCount() {
            return workerExecutorCount.get();
        }

        void reset() {
            taskExecutorCount.set(0);
            uncaughtExceptions.clear();
            workerExecutorCount.set(0);
        }

        private static TaskExecutor countingExecutor(AtomicInteger completedTaskCount) {
            SimpleAsyncTaskExecutor simpleAsyncTaskExecutor = new SimpleAsyncTaskExecutor();

            return task -> simpleAsyncTaskExecutor.execute(() -> {
                try {
                    task.run();
                } finally {
                    completedTaskCount.incrementAndGet();
                }
            });
        }
    }
}
