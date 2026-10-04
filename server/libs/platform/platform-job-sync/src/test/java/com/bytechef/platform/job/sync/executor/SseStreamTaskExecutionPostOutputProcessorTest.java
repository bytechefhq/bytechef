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

package com.bytechef.platform.job.sync.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskExecutionPostOutputProcessor;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandler;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.worker.task.SuspendTaskExecutionPostOutputProcessor;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SseStreamTaskExecutionPostOutputProcessorTest {

    private static final long JOB_ID = 100L;

    private final Cache<String, CopyOnWriteArrayList<SseStreamBridge>> sseStreamBridges = Caffeine.newBuilder()
        .build();
    private final SseStreamBridge sseStreamBridge = mock(SseStreamBridge.class);
    private final SseStreamTaskExecutionPostOutputProcessor processor =
        new SseStreamTaskExecutionPostOutputProcessor(sseStreamBridges);

    @BeforeEach
    void beforeEach() {
        TenantContext.setCurrentTenantId("public");

        sseStreamBridges.put(TenantCacheKeyUtils.getKey(JOB_ID), new CopyOnWriteArrayList<>(List.of(sseStreamBridge)));
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testProcessWithSuspendAwareSseEmitterHandlerReturnsTheFinalizedSuspend() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));
        when(actionContextAware.getJobResumeId()).thenReturn("jobResumeId");

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            emitter -> {
                emitter.send("question");
                emitter.complete();
            },
            actionContextAware);

        Object result = processor.process(createTaskExecution(), suspendAwareSseEmitterHandler);

        assertThat(result).isInstanceOf(Suspend.class);

        Suspend suspend = (Suspend) result;

        Map<String, ?> continueParameters = suspend.continueParameters();

        assertThat(continueParameters.get("pendingToolCallId")).isEqualTo("call_1");
        assertThat(continueParameters.get(MetadataConstants.JOB_RESUME_ID)).isEqualTo("jobResumeId");

        verify(sseStreamBridge).onEvent("question");
        verify(sseStreamBridge).onComplete();
    }

    @Test
    void testProcessWithSuspendAwareSseEmitterHandlerThrowsAfterAStreamError() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

        RuntimeException streamException = new RuntimeException("agent failed");

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            emitter -> emitter.error(streamException), actionContextAware);

        TaskExecution taskExecution = createTaskExecution();

        assertThatThrownBy(() -> processor.process(taskExecution, suspendAwareSseEmitterHandler))
            .isInstanceOf(IllegalStateException.class)
            .hasCause(streamException);

        verify(sseStreamBridge).onError(streamException);
    }

    @Test
    void testProcessWithNonSseOutputPassesThrough() {
        Object result = processor.process(createTaskExecution(), "hello");

        assertThat(result).isEqualTo("hello");
    }

    @Test
    void testCreateTaskExecutionPostOutputProcessorsRunsSseStreamBeforeSuspend() {
        List<TaskExecutionPostOutputProcessor> taskExecutionPostOutputProcessors =
            JobSyncExecutor.createTaskExecutionPostOutputProcessors(sseStreamBridges);

        int sseStreamIndex = indexOf(
            taskExecutionPostOutputProcessors, SseStreamTaskExecutionPostOutputProcessor.class);
        int suspendIndex = indexOf(taskExecutionPostOutputProcessors, SuspendTaskExecutionPostOutputProcessor.class);

        assertThat(sseStreamIndex).isNotNegative();
        assertThat(suspendIndex).isNotNegative();
        assertThat(sseStreamIndex).isLessThan(suspendIndex);
    }

    private static TaskExecution createTaskExecution() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(JOB_ID);

        return taskExecution;
    }

    private static int indexOf(
        List<TaskExecutionPostOutputProcessor> taskExecutionPostOutputProcessors, Class<?> processorClass) {

        for (int index = 0; index < taskExecutionPostOutputProcessors.size(); index++) {
            if (processorClass.isInstance(taskExecutionPostOutputProcessors.get(index))) {
                return index;
            }
        }

        return -1;
    }
}
