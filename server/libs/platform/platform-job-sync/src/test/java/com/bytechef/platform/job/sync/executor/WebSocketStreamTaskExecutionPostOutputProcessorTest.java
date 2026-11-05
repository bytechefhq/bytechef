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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * @author Ivica Cardic
 */
class WebSocketStreamTaskExecutionPostOutputProcessorTest {

    private static final long JOB_ID = 100L;

    private final Cache<String, CopyOnWriteArrayList<SseStreamBridge>> sseStreamBridges = Caffeine.newBuilder()
        .build();
    private final SseStreamBridge sseStreamBridge = mock(SseStreamBridge.class);
    private final WebSocketStreamTaskExecutionPostOutputProcessor processor =
        new WebSocketStreamTaskExecutionPostOutputProcessor(sseStreamBridges);

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
    void testProcessWithNonWebSocketOutputPassesThrough() {
        Object result = processor.process(createTaskExecution(), "hello");

        assertThat(result).isEqualTo("hello");

        verifyNoInteractions(sseStreamBridge);
    }

    @Test
    void testProcessRequiresAJobId() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        ActionDefinition.WebSocketHandler webSocketHandler =
            ActionDefinition.WebSocketHandler.WebSocketEmitter::complete;

        assertThatThrownBy(() -> processor.process(taskExecution, webSocketHandler))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("jobId");
    }

    @Test
    void testProcessForwardsSentDataAndCompletionToTheRegisteredBridges() {
        ActionDefinition.WebSocketHandler webSocketHandler = emitter -> {
            emitter.send("data1");
            emitter.send("data2");
            emitter.complete();
        };

        Object result = processor.process(createTaskExecution(), webSocketHandler);

        assertThat(result).isNull();

        InOrder inOrder = inOrder(sseStreamBridge);

        inOrder.verify(sseStreamBridge)
            .onEvent("data1");
        inOrder.verify(sseStreamBridge)
            .onEvent("data2");
        inOrder.verify(sseStreamBridge)
            .onComplete();

        verify(sseStreamBridge, never()).onError(any());
    }

    @Test
    void testProcessWaitsForAWebSocketSessionThatCompletesOnAnotherThread() {
        ActionDefinition.WebSocketHandler webSocketHandler = emitter -> Thread.startVirtualThread(() -> {
            emitter.send("late data");
            emitter.complete();
        });

        Object result = processor.process(createTaskExecution(), webSocketHandler);

        assertThat(result).isNull();

        verify(sseStreamBridge).onEvent("late data");
        verify(sseStreamBridge).onComplete();
    }

    @Test
    void testProcessReportsAStreamErrorToTheRegisteredBridges() {
        RuntimeException streamException = new RuntimeException("stream failed");

        ActionDefinition.WebSocketHandler webSocketHandler = emitter -> {
            emitter.send("chunk");
            emitter.error(streamException);
        };

        Object result = processor.process(createTaskExecution(), webSocketHandler);

        assertThat(result).isNull();

        verify(sseStreamBridge).onEvent("chunk");
        verify(sseStreamBridge).onError(streamException);
    }

    @Test
    void testProcessNotifiesEveryBridgeEvenWhenOneFails() {
        SseStreamBridge failingSseStreamBridge = mock(SseStreamBridge.class);

        doThrow(new IllegalStateException("not delivered")).when(failingSseStreamBridge)
            .onEvent("chunk");
        doThrow(new IllegalStateException("not delivered")).when(failingSseStreamBridge)
            .onComplete();

        sseStreamBridges.put(
            TenantCacheKeyUtils.getKey(JOB_ID),
            new CopyOnWriteArrayList<>(List.of(failingSseStreamBridge, sseStreamBridge)));

        ActionDefinition.WebSocketHandler webSocketHandler = emitter -> {
            emitter.send("chunk");
            emitter.complete();
        };

        Object result = processor.process(createTaskExecution(), webSocketHandler);

        assertThat(result).isNull();

        verify(sseStreamBridge).onEvent("chunk");
        verify(sseStreamBridge).onComplete();
    }

    @Test
    void testProcessCompletesWhenNoBridgeIsRegistered() {
        sseStreamBridges.invalidateAll();

        ActionDefinition.WebSocketHandler webSocketHandler = emitter -> {
            emitter.send("chunk");
            emitter.error(new RuntimeException("stream failed"));
        };

        Object result = processor.process(createTaskExecution(), webSocketHandler);

        assertThat(result).isNull();

        verifyNoInteractions(sseStreamBridge);
    }

    @Test
    void testProcessDoesNotNotifyTheBridgesOfAnotherJob() {
        SseStreamBridge otherJobSseStreamBridge = mock(SseStreamBridge.class);

        sseStreamBridges.put(
            TenantCacheKeyUtils.getKey(JOB_ID + 1), new CopyOnWriteArrayList<>(List.of(otherJobSseStreamBridge)));

        ActionDefinition.WebSocketHandler webSocketHandler = emitter -> {
            emitter.send("chunk");
            emitter.complete();
        };

        processor.process(createTaskExecution(), webSocketHandler);

        verify(sseStreamBridge).onEvent("chunk");
        verifyNoInteractions(otherJobSseStreamBridge);
    }

    @Test
    void testProcessReturnsAndKeepsTheInterruptFlagWhenTheWaitIsInterrupted() {
        ActionDefinition.WebSocketHandler webSocketHandler = emitter -> emitter.send("chunk");

        Thread currentThread = Thread.currentThread();

        currentThread.interrupt();

        try {
            Object result = processor.process(createTaskExecution(), webSocketHandler);

            assertThat(result).isNull();
            assertThat(currentThread.isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

        verify(sseStreamBridge).onEvent("chunk");
        verify(sseStreamBridge, never()).onComplete();
    }

    private static TaskExecution createTaskExecution() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(JOB_ID);

        return taskExecution;
    }
}
