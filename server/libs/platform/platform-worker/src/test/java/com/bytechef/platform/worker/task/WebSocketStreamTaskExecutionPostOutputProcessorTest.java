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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class WebSocketStreamTaskExecutionPostOutputProcessorTest {

    private static final long JOB_ID = 100L;
    private static final String TENANT_ID = "tenant1";

    private final MessageBroker messageBroker = mock(MessageBroker.class);
    private final WebSocketStreamTaskExecutionPostOutputProcessor processor =
        new WebSocketStreamTaskExecutionPostOutputProcessor(messageBroker);

    @BeforeEach
    void beforeEach() {
        TenantContext.setCurrentTenantId(TENANT_ID);
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testProcessWithNonWebSocketOutputPassesThrough() {
        Object result = processor.process(createTaskExecution(), "hello");

        assertThat(result).isEqualTo("hello");

        verify(messageBroker, never()).send(any(SseStreamMessageRoute.class), any());
    }

    @Test
    void testProcessRequiresAJobId() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        WebSocketHandler webSocketHandler = WebSocketHandler.WebSocketEmitter::complete;

        assertThatThrownBy(() -> processor.process(taskExecution, webSocketHandler))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("jobId");
    }

    @Test
    void testProcessSendsDataEventsAndACompleteEvent() {
        WebSocketHandler webSocketHandler = emitter -> {
            emitter.send("data1");
            emitter.send("data2");
            emitter.complete();
        };

        Object result = processor.process(createTaskExecution(), webSocketHandler);

        assertThat(result).isNull();

        List<SseStreamEvent> sseStreamEvents = captureSentEvents();

        assertThat(sseStreamEvents)
            .extracting(SseStreamEvent::getEventType)
            .containsExactly(
                SseStreamEvent.EVENT_TYPE_DATA, SseStreamEvent.EVENT_TYPE_DATA, SseStreamEvent.EVENT_TYPE_COMPLETE);
        assertThat(sseStreamEvents)
            .extracting(SseStreamEvent::getPayload)
            .containsExactly("data1", "data2", null);
    }

    @Test
    void testProcessTagsEveryEventWithTheJobAndTheTenantOfTheCallingThread() {
        WebSocketHandler webSocketHandler = emitter -> {
            emitter.send("data");
            emitter.complete();
        };

        processor.process(createTaskExecution(), webSocketHandler);

        List<SseStreamEvent> sseStreamEvents = captureSentEvents();

        assertThat(sseStreamEvents).hasSize(2);

        for (SseStreamEvent sseStreamEvent : sseStreamEvents) {
            assertThat(sseStreamEvent.getJobId()).isEqualTo(JOB_ID);
            assertThat(sseStreamEvent.getMetadata(TenantContext.CURRENT_TENANT_ID)).isEqualTo(TENANT_ID);
        }
    }

    @Test
    void testProcessKeepsTheTenantForEventsSentFromAnotherThread() {
        WebSocketHandler webSocketHandler = emitter -> Thread.startVirtualThread(() -> {
            emitter.send("late data");
            emitter.complete();
        });

        Object result = processor.process(createTaskExecution(), webSocketHandler);

        assertThat(result).isNull();

        List<SseStreamEvent> sseStreamEvents = captureSentEvents();

        assertThat(sseStreamEvents)
            .extracting(SseStreamEvent::getEventType)
            .containsExactly(SseStreamEvent.EVENT_TYPE_DATA, SseStreamEvent.EVENT_TYPE_COMPLETE);

        for (SseStreamEvent sseStreamEvent : sseStreamEvents) {
            assertThat(sseStreamEvent.getMetadata(TenantContext.CURRENT_TENANT_ID)).isEqualTo(TENANT_ID);
        }
    }

    @Test
    void testProcessSendsAnErrorEventWithTheErrorMessage() {
        WebSocketHandler webSocketHandler = emitter -> {
            emitter.send("data");
            emitter.error(new RuntimeException("stream failed"));
        };

        Object result = processor.process(createTaskExecution(), webSocketHandler);

        assertThat(result).isNull();

        List<SseStreamEvent> sseStreamEvents = captureSentEvents();

        SseStreamEvent dataSseStreamEvent = sseStreamEvents.get(0);

        assertThat(dataSseStreamEvent.getEventType()).isEqualTo(SseStreamEvent.EVENT_TYPE_DATA);

        SseStreamEvent errorSseStreamEvent = sseStreamEvents.get(1);

        assertThat(errorSseStreamEvent.getEventType()).isEqualTo(SseStreamEvent.EVENT_TYPE_ERROR);
        assertThat(errorSseStreamEvent.getPayload()).isEqualTo("stream failed");
    }

    @Test
    void testProcessCompletesWhenTheMessageBrokerFails() {
        doThrow(new IllegalStateException("broker down")).when(messageBroker)
            .send(eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), any());

        WebSocketHandler webSocketHandler = emitter -> {
            emitter.send("data");
            emitter.complete();
        };

        Object result = processor.process(createTaskExecution(), webSocketHandler);

        assertThat(result).isNull();

        verify(messageBroker, atLeastOnce()).send(eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), any());
    }

    @Test
    void testProcessReturnsAndKeepsTheInterruptFlagWhenTheWaitIsInterrupted() {
        WebSocketHandler webSocketHandler = emitter -> emitter.send("data");

        Thread currentThread = Thread.currentThread();

        currentThread.interrupt();

        try {
            Object result = processor.process(createTaskExecution(), webSocketHandler);

            assertThat(result).isNull();
            assertThat(currentThread.isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

        assertThat(captureSentEvents())
            .extracting(SseStreamEvent::getEventType)
            .containsExactly(SseStreamEvent.EVENT_TYPE_DATA);
    }

    private List<SseStreamEvent> captureSentEvents() {
        ArgumentCaptor<SseStreamEvent> sseStreamEventArgumentCaptor = ArgumentCaptor.forClass(SseStreamEvent.class);

        verify(messageBroker, atLeastOnce()).send(
            eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), sseStreamEventArgumentCaptor.capture());

        return sseStreamEventArgumentCaptor.getAllValues();
    }

    private static TaskExecution createTaskExecution() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(JOB_ID);

        return taskExecution;
    }
}
