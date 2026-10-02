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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandler;
import com.bytechef.platform.job.sync.SseStreamBridge;
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
    void testProcessWithSuspendAwareSseEmitterHandlerReturnsTheRecordedSuspend() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

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

        verify(sseStreamBridge).onEvent("question");
        verify(sseStreamBridge).onSuspend();
        verify(sseStreamBridge, never()).onComplete();
    }

    @Test
    void testProcessWithSuspendAwareSseEmitterHandlerCompletesTheBridgeWhenTheStreamDoesNotSuspend() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            emitter -> {
                emitter.send("answer");
                emitter.complete();
            },
            actionContextAware);

        Object result = processor.process(createTaskExecution(), suspendAwareSseEmitterHandler);

        assertThat(result).isNull();

        verify(sseStreamBridge).onEvent("answer");
        verify(sseStreamBridge).onComplete();
        verify(sseStreamBridge, never()).onSuspend();
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
        verify(sseStreamBridge, never()).onComplete();
    }

    @Test
    void testProcessWithAFailedStreamReportsOnlyTheError() {
        RuntimeException streamException = new RuntimeException("agent failed");

        ActionDefinition.SseEmitterHandler sseEmitterHandler = emitter -> {
            emitter.send("chunk");
            emitter.error(streamException);
        };

        Object result = processor.process(createTaskExecution(), sseEmitterHandler);

        assertThat(result).isNull();

        verify(sseStreamBridge).onEvent("chunk");
        verify(sseStreamBridge).onError(streamException);
        verify(sseStreamBridge, never()).onComplete();
        verify(sseStreamBridge, never()).onSuspend();
    }

    @Test
    void testSendOfAnAskUserQuestionEventFailsWhenNoBridgeIsRegistered() {
        sseStreamBridges.invalidateAll();

        List<Exception> sendFailures = new CopyOnWriteArrayList<>();

        processor.process(
            createTaskExecution(), (ActionDefinition.SseEmitterHandler) emitter -> {
                try {
                    emitter.send(
                        Map.of(AiAgentSseEventType.EVENT_TYPE, AiAgentSseEventType.ASK_USER_QUESTION, "questions",
                            List.of()));
                } catch (Exception exception) {
                    sendFailures.add(exception);
                }

                emitter.complete();
            });

        assertThat(sendFailures).hasSize(1);
    }

    @Test
    void testSendOfAnOrdinaryEventSucceedsWhenNoBridgeIsRegistered() {
        sseStreamBridges.invalidateAll();

        List<Exception> sendFailures = new CopyOnWriteArrayList<>();

        processor.process(
            createTaskExecution(), (ActionDefinition.SseEmitterHandler) emitter -> {
                try {
                    emitter.send("chunk");
                } catch (Exception exception) {
                    sendFailures.add(exception);
                }

                emitter.complete();
            });

        assertThat(sendFailures).isEmpty();
    }

    @Test
    void testSendOfAnAskUserQuestionEventReachesTheRegisteredBridge() {
        Map<String, Object> questionEvent = Map.of(
            AiAgentSseEventType.EVENT_TYPE, AiAgentSseEventType.ASK_USER_QUESTION, "questions", List.of());

        processor.process(
            createTaskExecution(), (ActionDefinition.SseEmitterHandler) emitter -> {
                emitter.send(questionEvent);
                emitter.complete();
            });

        verify(sseStreamBridge).onEvent(questionEvent);
    }

    @Test
    void testSendOfAnAskUserQuestionEventFailsWhenNoRegisteredBridgeDeliversIt() {
        Map<String, Object> questionEvent = Map.of(
            AiAgentSseEventType.EVENT_TYPE, AiAgentSseEventType.ASK_USER_QUESTION, "questions", List.of());

        doThrow(new IllegalStateException("not delivered")).when(sseStreamBridge)
            .onEvent(questionEvent);

        List<Exception> sendFailures = new CopyOnWriteArrayList<>();

        processor.process(
            createTaskExecution(), (ActionDefinition.SseEmitterHandler) emitter -> {
                try {
                    emitter.send(questionEvent);
                } catch (Exception exception) {
                    sendFailures.add(exception);
                }

                emitter.complete();
            });

        assertThat(sendFailures).hasSize(1);
    }

    @Test
    void testSendOfAnAskUserQuestionEventSucceedsWhenOneOfTheRegisteredBridgesDeliversIt() {
        Map<String, Object> questionEvent = Map.of(
            AiAgentSseEventType.EVENT_TYPE, AiAgentSseEventType.ASK_USER_QUESTION, "questions", List.of());
        SseStreamBridge failingSseStreamBridge = mock(SseStreamBridge.class);

        doThrow(new IllegalStateException("not delivered")).when(failingSseStreamBridge)
            .onEvent(questionEvent);

        sseStreamBridges.put(
            TenantCacheKeyUtils.getKey(JOB_ID),
            new CopyOnWriteArrayList<>(List.of(failingSseStreamBridge, sseStreamBridge)));

        List<Exception> sendFailures = new CopyOnWriteArrayList<>();

        processor.process(
            createTaskExecution(), (ActionDefinition.SseEmitterHandler) emitter -> {
                try {
                    emitter.send(questionEvent);
                } catch (Exception exception) {
                    sendFailures.add(exception);
                }

                emitter.complete();
            });

        assertThat(sendFailures).isEmpty();

        verify(sseStreamBridge).onEvent(questionEvent);
    }

    @Test
    void testSendOfAnOrdinaryEventSucceedsWhenTheRegisteredBridgeFails() {
        doThrow(new IllegalStateException("not delivered")).when(sseStreamBridge)
            .onEvent("chunk");

        List<Exception> sendFailures = new CopyOnWriteArrayList<>();

        processor.process(
            createTaskExecution(), (ActionDefinition.SseEmitterHandler) emitter -> {
                try {
                    emitter.send("chunk");
                } catch (Exception exception) {
                    sendFailures.add(exception);
                }

                emitter.complete();
            });

        assertThat(sendFailures).isEmpty();
    }

    @Test
    void testProcessWithNonSseOutputPassesThrough() {
        Object result = processor.process(createTaskExecution(), "hello");

        assertThat(result).isEqualTo("hello");
    }

    private static TaskExecution createTaskExecution() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(JOB_ID);

        return taskExecution;
    }
}
