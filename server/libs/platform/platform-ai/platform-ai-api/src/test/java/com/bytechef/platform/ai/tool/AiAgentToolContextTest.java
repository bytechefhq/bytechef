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

package com.bytechef.platform.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
class AiAgentToolContextTest {

    private final ActionContext actionContext = mock(ActionContext.class);

    @Test
    void testFetchReturnsTheAgentToolContextStoredInTheToolContext() {
        AiAgentToolContext aiAgentToolContext = new AiAgentToolContext(actionContext);

        ToolContext toolContext = new ToolContext(aiAgentToolContext.toMap());

        assertThat(AiAgentToolContext.fetch(toolContext)).isSameAs(aiAgentToolContext);
    }

    @Test
    void testFetchReturnsNullWithoutAnAgentToolContext() {
        assertThat(AiAgentToolContext.fetch(new ToolContext(Map.of("other", "value")))).isNull();
        assertThat(AiAgentToolContext.fetch(null)).isNull();
    }

    @Test
    void testActionContextIsRequired() {
        assertThatThrownBy(() -> new AiAgentToolContext(null, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testSseTransportQueuesEventsUntilAnEmitterIsAttachedAndThenSendsThemInOrder() {
        AiAgentToolContext.SseTransport sseTransport = new AiAgentToolContext.SseTransport();
        SseEmitter sseEmitter = mock(SseEmitter.class);

        Map<String, @Nullable Object> firstEvent = Map.of("index", 1);
        Map<String, @Nullable Object> secondEvent = Map.of("index", 2);
        Map<String, @Nullable Object> thirdEvent = Map.of("index", 3);

        sseTransport.send(firstEvent);
        sseTransport.send(secondEvent);

        verify(sseEmitter, never()).send(any());

        sseTransport.attach(sseEmitter, exception -> {});
        sseTransport.send(thirdEvent);

        InOrder inOrder = inOrder(sseEmitter);

        inOrder.verify(sseEmitter)
            .send(firstEvent);
        inOrder.verify(sseEmitter)
            .send(secondEvent);
        inOrder.verify(sseEmitter)
            .send(thirdEvent);
    }

    @Test
    void testSseTransportReportsAQueuedEventThatFailsToSendAndSendsTheRest() {
        AiAgentToolContext.SseTransport sseTransport = new AiAgentToolContext.SseTransport();
        SseEmitter sseEmitter = mock(SseEmitter.class);
        List<Exception> failures = new ArrayList<>();

        Map<String, @Nullable Object> failingEvent = Map.of("index", 1);
        Map<String, @Nullable Object> nextEvent = Map.of("index", 2);

        doThrow(new IllegalStateException("closed")).when(sseEmitter)
            .send(failingEvent);

        sseTransport.send(failingEvent);
        sseTransport.send(nextEvent);
        sseTransport.attach(sseEmitter, failures::add);

        assertThat(failures).hasSize(1);

        verify(sseEmitter).send(nextEvent);
    }

    @Test
    void testSseTransportThrowsAFailureToSendThroughTheAttachedEmitter() {
        AiAgentToolContext.SseTransport sseTransport = new AiAgentToolContext.SseTransport();
        SseEmitter sseEmitter = mock(SseEmitter.class);

        doThrow(new IllegalStateException("closed")).when(sseEmitter)
            .send(any());

        sseTransport.attach(sseEmitter, exception -> {});

        assertThatThrownBy(() -> sseTransport.send(Map.of("index", 1)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testSseTransportRejectsASecondEmitter() {
        AiAgentToolContext.SseTransport sseTransport = new AiAgentToolContext.SseTransport();

        sseTransport.attach(mock(SseEmitter.class), exception -> {});

        assertThatThrownBy(() -> sseTransport.attach(mock(SseEmitter.class), exception -> {}))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testSseTransportSendsAnEventSentWhileQueuedEventsAreSentAfterThem() throws Exception {
        AiAgentToolContext.SseTransport sseTransport = new AiAgentToolContext.SseTransport();
        SseEmitter sseEmitter = mock(SseEmitter.class);
        List<Object> sentEvents = new CopyOnWriteArrayList<>();
        CountDownLatch queuedEventSending = new CountDownLatch(1);
        CountDownLatch releaseQueuedEvent = new CountDownLatch(1);

        Map<String, @Nullable Object> queuedEvent = Map.of("index", 1);
        Map<String, @Nullable Object> directEvent = Map.of("index", 2);

        doAnswer(invocation -> {
            Object event = invocation.getArgument(0);

            if (event == queuedEvent) {
                queuedEventSending.countDown();

                assertThat(releaseQueuedEvent.await(5, TimeUnit.SECONDS)).isTrue();
            }

            sentEvents.add(event);

            return null;
        }).when(sseEmitter)
            .send(any());

        sseTransport.send(queuedEvent);

        Thread attachThread = Thread.ofVirtual()
            .start(() -> sseTransport.attach(sseEmitter, exception -> {}));

        assertThat(queuedEventSending.await(5, TimeUnit.SECONDS)).isTrue();

        Thread sendThread = Thread.ofVirtual()
            .start(() -> sseTransport.send(directEvent));

        sendThread.join(200);

        releaseQueuedEvent.countDown();

        attachThread.join(5000);
        sendThread.join(5000);

        assertThat(sentEvents).containsExactly(queuedEvent, directEvent);
    }

    @Test
    void testSseTransportIsPresentOnlyForAStreamingAgent() {
        assertThat(new AiAgentToolContext(actionContext).sseTransport()).isNull();
        assertThat(new AiAgentToolContext(actionContext, new AiAgentToolContext.SseTransport()).sseTransport())
            .isNotNull();
    }
}
