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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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

        verify(messageBroker, atLeastOnce()).send(
            eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), any(SseStreamEvent.class));
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

        verify(messageBroker, atLeastOnce()).send(
            eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), any(SseStreamEvent.class));
    }
}
