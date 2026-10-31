/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.bytechef.ee.platform.tool.invocation.log.config.ToolInvocationLogIntTestConfiguration;
import com.bytechef.ee.platform.tool.invocation.log.repository.ToolInvocationLogRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
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
        ToolInvocationLogIntTestConfiguration.class, ToolInvocationLogEventListenerIntTest.AsyncTestConfiguration.class
    },
    properties = "bytechef.edition=ee")
class ToolInvocationLogEventListenerIntTest {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private AsyncTestConfiguration asyncTestConfiguration;

    @Autowired
    private ToolInvocationLogRepository toolInvocationLogRepository;

    @BeforeEach
    void beforeEach() {
        asyncTestConfiguration.reset();
    }

    @AfterEach
    void afterEach() {
        toolInvocationLogRepository.deleteAll();
    }

    @Test
    void testPublishedEventIsPersistedOnTheWorkerExecutor() {
        ToolExecutionEvent toolExecutionEvent = ToolExecutionEvent
            .builder(
                PlatformType.EMBEDDED, ToolExecutionSurface.MCP_EMBEDDED, ToolExecutionKind.COMPONENT,
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
            .jobId(11L)
            .outcome(ToolExecutionOutcome.ERROR)
            .errorType("IllegalStateException")
            .errorMessage("failed")
            .durationMs(42)
            .build();

        applicationEventPublisher.publishEvent(toolExecutionEvent);

        await().atMost(Duration.ofSeconds(10))
            .until(() -> toolInvocationLogRepository.count() == 1);

        List<ToolInvocationLog> toolInvocationLogs = toolInvocationLogRepository.findAll();

        ToolInvocationLog toolInvocationLog = toolInvocationLogs.getFirst();

        assertThat(toolInvocationLog.getType()).isEqualTo(PlatformType.EMBEDDED);
        assertThat(toolInvocationLog.getSurface()).isEqualTo(ToolExecutionSurface.MCP_EMBEDDED);
        assertThat(toolInvocationLog.getKind()).isEqualTo(ToolExecutionKind.COMPONENT);
        assertThat(toolInvocationLog.getToolName()).isEqualTo("slack_sendMessage");
        assertThat(toolInvocationLog.getComponentName()).isEqualTo("slack");
        assertThat(toolInvocationLog.getComponentVersion()).isEqualTo(1);
        assertThat(toolInvocationLog.getOperationName()).isEqualTo("sendMessage");
        assertThat(toolInvocationLog.getConnectionId()).isEqualTo(7L);
        assertThat(toolInvocationLog.getEnvironment()).isEqualTo(2);
        assertThat(toolInvocationLog.getWorkspaceId()).isEqualTo(4L);
        assertThat(toolInvocationLog.getExternalUserId()).isEqualTo("user-1");
        assertThat(toolInvocationLog.getConnectedUserId()).isEqualTo(8L);
        assertThat(toolInvocationLog.getIntegrationInstanceId()).isEqualTo(9L);
        assertThat(toolInvocationLog.getMcpServerId()).isEqualTo(3L);
        assertThat(toolInvocationLog.getJobId()).isEqualTo(11L);
        assertThat(toolInvocationLog.getOutcome()).isEqualTo(ToolExecutionOutcome.ERROR);
        assertThat(toolInvocationLog.getErrorType()).isEqualTo("IllegalStateException");
        assertThat(toolInvocationLog.getErrorMessage()).isEqualTo("failed");
        assertThat(toolInvocationLog.getDurationMs()).isEqualTo(42);
        assertThat(toolInvocationLog.getCreatedDate()).isNotNull();

        assertThat(asyncTestConfiguration.workerExecutorCount()).isEqualTo(1);
        assertThat(asyncTestConfiguration.taskExecutorCount()).isZero();
    }

    @Test
    void testPersistenceFailureIsSwallowed() {
        ToolExecutionEvent toolExecutionEvent = ToolExecutionEvent
            .builder(
                PlatformType.EMBEDDED, ToolExecutionSurface.EMBEDDED_API_ACTION, ToolExecutionKind.COMPONENT,
                "x".repeat(300))
            .outcome(ToolExecutionOutcome.SUCCESS)
            .build();

        applicationEventPublisher.publishEvent(toolExecutionEvent);

        await().atMost(Duration.ofSeconds(10))
            .until(() -> asyncTestConfiguration.workerExecutorCount() == 1);

        assertThat(asyncTestConfiguration.uncaughtExceptions()).isEmpty();
        assertThat(toolInvocationLogRepository.count()).isZero();
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
