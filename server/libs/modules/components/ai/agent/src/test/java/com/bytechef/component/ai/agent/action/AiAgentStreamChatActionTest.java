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

package com.bytechef.component.ai.agent.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Tests for {@link AiAgentStreamChatAction#createSseHandler} covering buffered-event replay, cancel-on-send-failure,
 * upstream-error propagation, and timeout-cancel wiring.
 *
 * @author Ivica Cardic
 */
class AiAgentStreamChatActionTest {

    @Test
    void testBufferedEventsAreReplayedOnceEmitterBinds() {
        Queue<Map<String, @Nullable Object>> bufferedEvents = new ConcurrentLinkedQueue<>();

        Map<String, @Nullable Object> firstBuffered = new LinkedHashMap<>();
        firstBuffered.put("__eventType", "tool_execution");
        firstBuffered.put("toolName", "lookupCustomer");

        Map<String, @Nullable Object> secondBuffered = new LinkedHashMap<>();
        secondBuffered.put("__eventType", "tool_execution");
        secondBuffered.put("toolName", "createTicket");

        bufferedEvents.add(firstBuffered);
        bufferedEvents.add(secondBuffered);

        AtomicReference<@Nullable SseEmitter> emitterReference = new AtomicReference<>();
        ActionContext context = mock(ActionContext.class);
        SseEmitter emitter = mock(SseEmitter.class);

        Flux<Object> emptyUpstream = Flux.empty();

        AiAgentStreamChatAction
            .createSseHandler(emptyUpstream, emitterReference, bufferedEvents, context)
            .handle(emitter);

        verify(emitter).send(firstBuffered);
        verify(emitter).send(secondBuffered);

        assertThat(emitterReference.get()).isSameAs(emitter);
        assertThat(bufferedEvents).isEmpty();
    }

    @Test
    void testSendFailureOnStreamItemCancelsSubscriptionAndLogsAtWarn() {
        Queue<Map<String, @Nullable Object>> bufferedEvents = new ConcurrentLinkedQueue<>();
        AtomicReference<@Nullable SseEmitter> emitterReference = new AtomicReference<>();
        ActionContext context = mock(ActionContext.class);
        SseEmitter emitter = mock(SseEmitter.class);

        Sinks.Many<Object> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();

        doThrow(new RuntimeException("client disconnected"))
            .when(emitter)
            .send("first");

        AiAgentStreamChatAction
            .createSseHandler(sink.asFlux(), emitterReference, bufferedEvents, context)
            .handle(emitter);

        sink.tryEmitNext("first");
        sink.tryEmitNext("second");

        verify(emitter).send("first");
        verify(emitter, never()).send("second");
        verify(context, atLeastOnce()).log(any());
    }

    @Test
    void testUpstreamErrorPropagatesToEmitterError() {
        Queue<Map<String, @Nullable Object>> bufferedEvents = new ConcurrentLinkedQueue<>();
        AtomicReference<@Nullable SseEmitter> emitterReference = new AtomicReference<>();
        ActionContext context = mock(ActionContext.class);
        SseEmitter emitter = mock(SseEmitter.class);

        RuntimeException upstreamFailure = new RuntimeException("LLM provider timeout");

        Flux<Object> failingUpstream = Flux.error(upstreamFailure);

        AiAgentStreamChatAction
            .createSseHandler(failingUpstream, emitterReference, bufferedEvents, context)
            .handle(emitter);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

        verify(emitter).error(throwableCaptor.capture());
        verify(emitter, never()).complete();

        assertThat(throwableCaptor.getValue()).isSameAs(upstreamFailure);
    }

    @Test
    void testTimeoutListenerCancelsUpstreamSubscription() {
        Queue<Map<String, @Nullable Object>> bufferedEvents = new ConcurrentLinkedQueue<>();
        AtomicReference<@Nullable SseEmitter> emitterReference = new AtomicReference<>();
        ActionContext context = mock(ActionContext.class);
        SseEmitter emitter = mock(SseEmitter.class);

        Sinks.Many<Object> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();

        ArgumentCaptor<Runnable> timeoutListenerCaptor = ArgumentCaptor.forClass(Runnable.class);

        AiAgentStreamChatAction
            .createSseHandler(sink.asFlux(), emitterReference, bufferedEvents, context)
            .handle(emitter);

        verify(emitter).addTimeoutListener(timeoutListenerCaptor.capture());

        sink.tryEmitNext("before-timeout");

        verify(emitter).send("before-timeout");

        timeoutListenerCaptor.getValue()
            .run();

        sink.tryEmitNext("after-timeout");

        verify(emitter, times(1)).send("before-timeout");
        verify(emitter, never()).send("after-timeout");
    }

    @Test
    void testStreamCompletionCallsEmitterComplete() {
        Queue<Map<String, @Nullable Object>> bufferedEvents = new ConcurrentLinkedQueue<>();
        AtomicReference<@Nullable SseEmitter> emitterReference = new AtomicReference<>();
        ActionContext context = mock(ActionContext.class);
        SseEmitter emitter = mock(SseEmitter.class);

        Flux<Object> finiteUpstream = Flux.just("chunk-1", "chunk-2");

        AiAgentStreamChatAction
            .createSseHandler(finiteUpstream, emitterReference, bufferedEvents, context)
            .handle(emitter);

        verify(emitter).send("chunk-1");
        verify(emitter).send("chunk-2");
        verify(emitter).complete();
        verify(emitter, never()).error(any());
    }
}
