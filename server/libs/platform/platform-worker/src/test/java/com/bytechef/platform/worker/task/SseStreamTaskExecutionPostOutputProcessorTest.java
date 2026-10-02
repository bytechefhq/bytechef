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

package com.bytechef.platform.worker.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandler;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class SseStreamTaskExecutionPostOutputProcessorTest {

    private final MessageBroker messageBroker = mock(MessageBroker.class);

    private final SseStreamTaskExecutionPostOutputProcessor processor =
        new SseStreamTaskExecutionPostOutputProcessor(messageBroker);

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testProcessWithNonSseOutputPassesThrough() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        String regularOutput = "hello";

        Object result = processor.process(taskExecution, regularOutput);

        assertEquals(regularOutput, result);

        verify(messageBroker, never()).send(any(SseStreamMessageRoute.class), any());
    }

    @Test
    void testProcessWithSseEmitterHandlerReturnsNull() {
        TenantContext.setCurrentTenantId("public");

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        ActionDefinition.SseEmitterHandler sseEmitterHandler = emitter -> {
            emitter.send("data1");
            emitter.send("data2");
            emitter.complete();
        };

        Object result = processor.process(taskExecution, sseEmitterHandler);

        assertNull(result);
        assertEquals(
            List.of(SseStreamEvent.EVENT_TYPE_DATA, SseStreamEvent.EVENT_TYPE_DATA, SseStreamEvent.EVENT_TYPE_COMPLETE),
            getSentEventTypes());
    }

    @Test
    void testProcessWithSseEmitterHandlerErrorSendsErrorEvent() {
        TenantContext.setCurrentTenantId("public");

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        ActionDefinition.SseEmitterHandler sseEmitterHandler = emitter -> {
            emitter.send("data");
            emitter.error(new RuntimeException("test error"));
        };

        Object result = processor.process(taskExecution, sseEmitterHandler);

        assertNull(result);
        assertEquals(List.of(SseStreamEvent.EVENT_TYPE_DATA, SseStreamEvent.EVENT_TYPE_ERROR), getSentEventTypes());
    }

    @Test
    void testProcessWithSuspendAwareSseEmitterHandlerReturnsTheRecordedSuspend() {
        TenantContext.setCurrentTenantId("public");

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            emitter -> {
                emitter.send("question");
                emitter.complete();
            },
            actionContextAware);

        Object result = processor.process(taskExecution, suspendAwareSseEmitterHandler);

        Suspend suspend = assertInstanceOf(Suspend.class, result);

        Map<String, ?> continueParameters = suspend.continueParameters();

        assertEquals("call_1", continueParameters.get("pendingToolCallId"));
    }

    @Test
    void testProcessWithSuspendAwareSseEmitterHandlerThatSuspendedSendsNoCompleteEvent() {
        TenantContext.setCurrentTenantId("public");

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            ActionDefinition.SseEmitterHandler.SseEmitter::complete, actionContextAware);

        processor.process(taskExecution, suspendAwareSseEmitterHandler);

        verify(messageBroker, never()).send(
            eq(SseStreamMessageRoute.SSE_STREAM_EVENTS),
            argThat((SseStreamEvent sseStreamEvent) -> SseStreamEvent.EVENT_TYPE_COMPLETE.equals(
                sseStreamEvent.getEventType())));
    }

    @Test
    void testProcessWithSuspendAwareSseEmitterHandlerThatDidNotSuspendSendsCompleteEvent() {
        TenantContext.setCurrentTenantId("public");

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            emitter -> {
                emitter.send("answer");
                emitter.complete();
            },
            mock(ActionContextAware.class));

        Object result = processor.process(taskExecution, suspendAwareSseEmitterHandler);

        assertNull(result);

        verify(messageBroker).send(
            eq(SseStreamMessageRoute.SSE_STREAM_EVENTS),
            argThat((SseStreamEvent sseStreamEvent) -> SseStreamEvent.EVENT_TYPE_COMPLETE.equals(
                sseStreamEvent.getEventType())));
    }

    @Test
    void testProcessWithSuspendAwareSseEmitterHandlerThrowsAfterAStreamError() {
        TenantContext.setCurrentTenantId("public");

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

        RuntimeException streamException = new RuntimeException("agent failed");

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            emitter -> emitter.error(streamException), actionContextAware);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> processor.process(taskExecution, suspendAwareSseEmitterHandler));

        assertSame(streamException, exception.getCause());
        assertEquals(List.of(SseStreamEvent.EVENT_TYPE_ERROR), getSentEventTypes());
    }

    @Test
    void testProcessWithSuspendAwareSseEmitterHandlerWhoseBeforeSuspendFailsSendsAnErrorInsteadOfComplete() {
        TenantContext.setCurrentTenantId("public");

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            ActionDefinition.SseEmitterHandler.SseEmitter::complete, actionContextAware).withBeforeSuspend(suspend -> {
                throw new IllegalStateException("Notification failed");
            });

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> processor.process(taskExecution, suspendAwareSseEmitterHandler));

        assertEquals("Notification failed", exception.getMessage());
        assertEquals(List.of(SseStreamEvent.EVENT_TYPE_ERROR), getSentEventTypes());
    }

    @Test
    void testProcessPropagatesADataEventBrokerFailureToTheEmitterSend() {
        TenantContext.setCurrentTenantId("public");

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        RuntimeException brokerException = new RuntimeException("broker down");

        doThrow(brokerException).when(messageBroker)
            .send(
                eq(SseStreamMessageRoute.SSE_STREAM_EVENTS),
                argThat((SseStreamEvent sseStreamEvent) -> SseStreamEvent.EVENT_TYPE_DATA.equals(
                    sseStreamEvent.getEventType())));

        AtomicReference<RuntimeException> sendFailureReference = new AtomicReference<>();

        ActionDefinition.SseEmitterHandler sseEmitterHandler = emitter -> {
            try {
                emitter.send("question");
            } catch (RuntimeException exception) {
                sendFailureReference.set(exception);
            }

            emitter.complete();
        };

        Object result = processor.process(taskExecution, sseEmitterHandler);

        assertNull(result);
        assertSame(brokerException, sendFailureReference.get());
    }

    private List<String> getSentEventTypes() {
        ArgumentCaptor<SseStreamEvent> sseStreamEventArgumentCaptor = ArgumentCaptor.forClass(SseStreamEvent.class);

        verify(messageBroker, atLeastOnce()).send(
            eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), sseStreamEventArgumentCaptor.capture());

        return sseStreamEventArgumentCaptor.getAllValues()
            .stream()
            .map(SseStreamEvent::getEventType)
            .toList();
    }
}
