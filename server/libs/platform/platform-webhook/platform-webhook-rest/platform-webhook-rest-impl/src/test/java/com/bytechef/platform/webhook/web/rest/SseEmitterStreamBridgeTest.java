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

package com.bytechef.platform.webhook.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SseEmitterStreamBridgeTest {

    private SseEmitter sseEmitter;
    private SseEmitterStreamBridge sseEmitterStreamBridge;

    @BeforeEach
    void beforeEach() {
        sseEmitter = mock(SseEmitter.class);

        sseEmitterStreamBridge = new SseEmitterStreamBridge(sseEmitter);
    }

    @Test
    void testOnEventSendsEventTypeAsNamedEventWithoutDiscriminator() throws IOException {
        sseEmitterStreamBridge.onEvent(
            Map.of(
                AiAgentSseEventType.EVENT_TYPE, AiAgentSseEventType.ASK_USER_QUESTION,
                "questions", List.of(Map.of("question", "Which library?")),
                "resumeUrl", "https://example.com/api/job/resume/abc"));

        List<Object> sentParts = getSentParts();

        assertThat(sentParts.getFirst()).asString()
            .contains("event:" + AiAgentSseEventType.ASK_USER_QUESTION);
        assertThat(sentParts.get(1)).isEqualTo(
            Map.of(
                "questions", List.of(Map.of("question", "Which library?")),
                "resumeUrl", "https://example.com/api/job/resume/abc"));
    }

    @Test
    void testOnEventSendsToolExecutionAsNamedEvent() throws IOException {
        sseEmitterStreamBridge.onEvent(
            Map.of(AiAgentSseEventType.EVENT_TYPE, AiAgentSseEventType.TOOL_EXECUTION, "toolName", "search"));

        List<Object> sentParts = getSentParts();

        assertThat(sentParts.getFirst()).asString()
            .contains("event:" + AiAgentSseEventType.TOOL_EXECUTION);
        assertThat(sentParts.get(1)).isEqualTo(Map.of("toolName", "search"));
    }

    @Test
    void testOnEventSendsNonStringEventTypeAsStream() throws IOException {
        Map<String, Object> payload = Map.of(AiAgentSseEventType.EVENT_TYPE, 42, "text", "hello");

        sseEmitterStreamBridge.onEvent(payload);

        List<Object> sentParts = getSentParts();

        assertThat(sentParts.getFirst()).asString()
            .contains("event:stream");
        assertThat(sentParts.get(1)).isEqualTo(payload);
    }

    @Test
    void testOnEventSendsNonStringEventValueAsStream() throws IOException {
        Map<String, Object> payload = Map.of("event", 42, "payload", "hello");

        assertThatCode(() -> sseEmitterStreamBridge.onEvent(payload)).doesNotThrowAnyException();

        List<Object> sentParts = getSentParts();

        assertThat(sentParts.getFirst()).asString()
            .contains("event:stream");
        assertThat(sentParts.get(1)).isEqualTo(payload);
    }

    @Test
    void testOnEventSendsSingleRemainingValueOfEventMap() throws IOException {
        sseEmitterStreamBridge.onEvent(Map.of("event", "delta", "payload", "Hello"));

        List<Object> sentParts = getSentParts();

        assertThat(sentParts.getFirst()).asString()
            .contains("event:delta");
        assertThat(sentParts.get(1)).isEqualTo("\"Hello\"");
    }

    @Test
    void testOnEventSendsPayloadValueWhenEventMapHasSeveralOtherEntries() throws IOException {
        sseEmitterStreamBridge.onEvent(Map.of("event", "delta", "payload", "Hello", "index", 3));

        List<Object> sentParts = getSentParts();

        assertThat(sentParts.getFirst()).asString()
            .contains("event:delta");
        assertThat(sentParts.get(1)).isEqualTo("\"Hello\"");
    }

    @Test
    void testOnEventSendsFirstRemainingValueWhenEventMapHasNoPayload() throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("event", "delta");
        payload.put("text", "Hello");
        payload.put("index", 3);

        sseEmitterStreamBridge.onEvent(payload);

        List<Object> sentParts = getSentParts();

        assertThat(sentParts.getFirst()).asString()
            .contains("event:delta");
        assertThat(sentParts.get(1)).isEqualTo("\"Hello\"");
    }

    @Test
    void testOnCompleteIsIdempotent() {
        sseEmitterStreamBridge.onComplete();
        sseEmitterStreamBridge.onComplete();

        verify(sseEmitter, times(1)).complete();
    }

    @Test
    void testOnSuspendSendsSuspendedEventAndCompletes() throws IOException {
        sseEmitterStreamBridge.onSuspend();

        assertThat(getSentParts().getFirst()).asString()
            .contains("event:suspended");

        verify(sseEmitter).complete();
    }

    @Test
    void testOnSuspendAfterOnCompleteIsNoOp() throws IOException {
        sseEmitterStreamBridge.onComplete();

        sseEmitterStreamBridge.onSuspend();

        verify(sseEmitter, times(1)).complete();
        verify(sseEmitter, never()).send(any(SseEventBuilder.class));
    }

    @Test
    void testOnErrorAfterOnCompleteIsNoOp() throws IOException {
        sseEmitterStreamBridge.onComplete();

        sseEmitterStreamBridge.onError(new RuntimeException("boom"));

        verify(sseEmitter, times(1)).complete();
        verify(sseEmitter, never()).send(any(SseEventBuilder.class));
    }

    @Test
    void testOnErrorSendsErrorEventAndCompletes() throws IOException {
        sseEmitterStreamBridge.onError(new RuntimeException("boom"));

        List<Object> sentParts = getSentParts();

        assertThat(sentParts.getFirst()).asString()
            .contains("event:error");
        assertThat(sentParts.get(1)).isEqualTo("\"boom\"");

        verify(sseEmitter).complete();
    }

    @Test
    void testOnErrorFallsBackToDefaultMessageWhenMessageIsNull() throws IOException {
        sseEmitterStreamBridge.onError(new RuntimeException());

        List<Object> sentParts = getSentParts();

        assertThat(sentParts.getFirst()).asString()
            .contains("event:error");
        assertThat(sentParts.get(1)).isEqualTo("\"An error occurred\"");
    }

    @Test
    void testOnEventIsNotSentAfterEmitterCompletionCallback() throws IOException {
        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);

        verify(sseEmitter).onCompletion(runnableArgumentCaptor.capture());

        Runnable onCompletion = runnableArgumentCaptor.getValue();

        onCompletion.run();

        assertNothingSentAfterClose();
    }

    @Test
    void testOnEventIsNotSentAfterEmitterTimeoutCallback() throws IOException {
        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);

        verify(sseEmitter).onTimeout(runnableArgumentCaptor.capture());

        Runnable onTimeout = runnableArgumentCaptor.getValue();

        onTimeout.run();

        assertNothingSentAfterClose();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testOnEventIsNotSentAfterEmitterErrorCallback() throws IOException {
        ArgumentCaptor<Consumer<Throwable>> consumerArgumentCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(sseEmitter).onError(consumerArgumentCaptor.capture());

        Consumer<Throwable> onError = consumerArgumentCaptor.getValue();

        onError.accept(new IOException("Broken pipe"));

        assertNothingSentAfterClose();
    }

    @Test
    void testOnEventSwallowsAsyncRequestNotUsableException() throws IOException {
        doThrow(new AsyncRequestNotUsableException("Client disconnected"))
            .when(sseEmitter)
            .send(any(SseEventBuilder.class));

        assertThatCode(() -> sseEmitterStreamBridge.onEvent(Map.of("event", "delta", "payload", "Hello")))
            .doesNotThrowAnyException();

        verify(sseEmitter).send(any(SseEventBuilder.class));
    }

    @Test
    void testOnEventThrowsWhenAnAskUserQuestionEventIsNotDelivered() throws IOException {
        doThrow(new AsyncRequestNotUsableException("Client disconnected"))
            .when(sseEmitter)
            .send(any(SseEventBuilder.class));

        assertThatThrownBy(
            () -> sseEmitterStreamBridge.onEvent(
                Map.of(
                    AiAgentSseEventType.EVENT_TYPE, AiAgentSseEventType.ASK_USER_QUESTION,
                    "resumeUrl", "https://example.com/api/job/resume/abc")))
                        .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testOnEventLogsASendThatLostTheRaceWithACloseAtDebugLevel() throws IOException {
        doThrow(new IllegalStateException("ResponseBodyEmitter has already completed"))
            .when(sseEmitter)
            .send(any(SseEventBuilder.class));

        Logger logger = (Logger) LoggerFactory.getLogger(SseEmitterStreamBridge.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        Level originalLevel = logger.getLevel();

        listAppender.start();

        logger.addAppender(listAppender);
        logger.setLevel(Level.DEBUG);

        try {
            assertThatCode(() -> sseEmitterStreamBridge.onEvent(Map.of("event", "delta", "payload", "Hello")))
                .doesNotThrowAnyException();
        } finally {
            logger.detachAppender(listAppender);
            logger.setLevel(originalLevel);
        }

        assertThat(listAppender.list)
            .extracting(ILoggingEvent::getLevel)
            .containsExactly(Level.DEBUG);
    }

    private void assertNothingSentAfterClose() throws IOException {
        sseEmitterStreamBridge.onEvent(Map.of("event", "delta", "payload", "Hello"));

        assertThatThrownBy(
            () -> sseEmitterStreamBridge.onEvent(
                Map.of(
                    AiAgentSseEventType.EVENT_TYPE, AiAgentSseEventType.ASK_USER_QUESTION,
                    "resumeUrl", "https://example.com/api/job/resume/abc")))
                        .isInstanceOf(IllegalStateException.class);

        sseEmitterStreamBridge.onError(new RuntimeException("boom"));
        sseEmitterStreamBridge.onComplete();

        verify(sseEmitter, never()).send(any(SseEventBuilder.class));
        verify(sseEmitter, never()).complete();
    }

    private List<Object> getSentParts() throws IOException {
        ArgumentCaptor<SseEventBuilder> sseEventBuilderArgumentCaptor = ArgumentCaptor.forClass(SseEventBuilder.class);

        verify(sseEmitter).send(sseEventBuilderArgumentCaptor.capture());

        SseEventBuilder sseEventBuilder = sseEventBuilderArgumentCaptor.getValue();

        return sseEventBuilder.build()
            .stream()
            .map(ResponseBodyEmitter.DataWithMediaType::getData)
            .toList();
    }
}
