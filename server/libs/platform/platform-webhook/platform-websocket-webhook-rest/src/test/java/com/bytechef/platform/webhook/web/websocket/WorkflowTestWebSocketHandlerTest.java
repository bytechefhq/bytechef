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

package com.bytechef.platform.webhook.web.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.webhook.voice.SessionTranscript;
import com.bytechef.platform.webhook.voice.VoiceSessionEngine;
import com.bytechef.platform.webhook.voice.VoiceSessionEngine.VoiceSession;
import com.bytechef.platform.webhook.voice.WebSocketEmitter;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowTestWebSocketHandlerTest {

    private final HandlerTestSupport.NoConnections voiceSessionConnectionResolver =
        new HandlerTestSupport.NoConnections();
    private final WorkflowTestVoiceSessionTokenService tokenService = mock(WorkflowTestVoiceSessionTokenService.class);
    private final VoiceSessionEngine voiceSessionEngine = mock(VoiceSessionEngine.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final VoiceSessionRegistry voiceSessionRegistry = new VoiceSessionRegistry();
    private final WorkflowTestWebSocketHandler handler = new WorkflowTestWebSocketHandler(
        voiceSessionRegistry, voiceSessionEngine, workflowService, tokenService, voiceSessionConnectionResolver);

    @Test
    void testProviderErrorClosesTheTestSocketWithProviderError() throws Exception {
        WebSocketSession webSocketSession = socket("tok-1", "&environmentId=1");
        WebSocketEmitter emitter = new WebSocketEmitter();
        Workflow workflow = HandlerTestSupport.voiceWorkflow();

        when(tokenService.consume("tok-1", "wf-1")).thenReturn(OptionalLong.of(1L));
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(invocation -> HandlerTestSupport.startLikeTheEngine(invocation, emitter, 5L));

        handler.afterConnectionEstablished(webSocketSession);

        verify(webSocketSession, timeout(2000)).sendMessage(any(TextMessage.class));

        emitter.error(new RuntimeException("provider down"));

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);

        verify(webSocketSession, times(2)).sendMessage(sent.capture());

        assertThat(sent.getAllValues()
            .getLast()
            .getPayload())
                .contains("\"type\":\"error\"")
                .contains("provider down");

        verify(webSocketSession).close(CloseStatus.NORMAL.withReason("provider_error"));
    }

    @Test
    void testProviderCloseClosesTheTestSocketWithProviderClosed() throws Exception {
        WebSocketSession webSocketSession = socket("tok-1", "&environmentId=1");
        WebSocketEmitter emitter = new WebSocketEmitter();
        Workflow workflow = HandlerTestSupport.voiceWorkflow();

        when(tokenService.consume("tok-1", "wf-1")).thenReturn(OptionalLong.of(1L));
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(invocation -> HandlerTestSupport.startLikeTheEngine(invocation, emitter, 5L));

        handler.afterConnectionEstablished(webSocketSession);

        verify(webSocketSession, timeout(2000)).sendMessage(any(TextMessage.class));

        emitter.complete();

        verify(webSocketSession).close(CloseStatus.NORMAL.withReason("provider_closed"));
    }

    @Test
    void testClosingTheTestSocketIsNotReportedAsProviderClosed() throws Exception {
        WebSocketSession webSocketSession = socket("tok-1", "&environmentId=1");
        WebSocketEmitter emitter = new WebSocketEmitter();
        Workflow workflow = HandlerTestSupport.voiceWorkflow();

        when(tokenService.consume("tok-1", "wf-1")).thenReturn(OptionalLong.of(1L));
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(invocation -> HandlerTestSupport.startLikeTheEngine(invocation, emitter, 5L));
        doAnswer(invocation -> {
            emitter.complete();

            return null;
        }).when(voiceSessionEngine)
            .stop(5L);

        handler.afterConnectionEstablished(webSocketSession);

        verify(webSocketSession, timeout(2000)).sendMessage(any(TextMessage.class));

        handler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL);

        verify(voiceSessionEngine).stop(5L);
        verify(webSocketSession, never()).close(any(CloseStatus.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testKeepaliveResetsTheSilenceClockWithoutReachingTheProvider() throws Exception {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-09-08T10:00:00Z"));
        VoiceSessionRegistry clockedRegistry = new VoiceSessionRegistry(now::get);
        WorkflowTestWebSocketHandler clockedHandler = new WorkflowTestWebSocketHandler(
            clockedRegistry, voiceSessionEngine, workflowService, tokenService, voiceSessionConnectionResolver);
        WebSocketSession webSocketSession = socket("tok-1", "");
        WebSocketEmitter emitter = new WebSocketEmitter();
        List<Object> providerMessages = new CopyOnWriteArrayList<>();
        Workflow workflow = HandlerTestSupport.voiceWorkflow();

        emitter.addMessageListener(providerMessages::add);

        when(tokenService.consume("tok-1", "wf-1")).thenReturn(OptionalLong.of(1L));
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(5L, emitter, 24000, new SessionTranscript()));
        when(voiceSessionEngine.emitter(5L)).thenReturn(Optional.of(emitter));

        clockedHandler.afterConnectionEstablished(webSocketSession);

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);

        verify(webSocketSession, timeout(2000)).sendMessage(sent.capture());

        Map<String, Object> connected = (Map<String, Object>) JsonUtils.read(sent.getValue()
            .getPayload(), Map.class);
        String sessionId = String.valueOf(connected.get("sessionId"));

        now.set(now.get()
            .plusSeconds(30));

        clockedHandler.handleTextMessage(
            webSocketSession, new TextMessage("{\"action\":\"keepalive\",\"type\":\"control\"}"));

        assertThat(clockedRegistry.get(sessionId))
            .get()
            .extracting(VoiceSessionRegistry.Session::lastInboundAudioAt)
            .isEqualTo(now.get());
        assertThat(providerMessages).isEmpty();
    }

    @Test
    void testSessionLimitClosesTheTestSocketAtTheServerMaximumWhenTheTriggerSetsNone() throws Exception {
        WorkflowTestWebSocketHandler limitedHandler = new WorkflowTestWebSocketHandler(
            voiceSessionRegistry, voiceSessionEngine, workflowService, tokenService, voiceSessionConnectionResolver,
            1L);
        WebSocketSession webSocketSession = socket("tok-1", "");
        Workflow workflow = HandlerTestSupport.voiceWorkflow();

        when(tokenService.consume("tok-1", "wf-1")).thenReturn(OptionalLong.of(1L));
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(5L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        limitedHandler.afterConnectionEstablished(webSocketSession);

        verify(webSocketSession, timeout(2000)).sendMessage(any(TextMessage.class));
        verify(webSocketSession, timeout(4000)).close(CloseStatus.NORMAL.withReason("session_limit"));
    }

    @Test
    void testRegisteredSocketIsWrappedInAConcurrentDecorator() throws Exception {
        WebSocketSession webSocketSession = socket("tok-1", "&environmentId=1");
        Workflow workflow = HandlerTestSupport.voiceWorkflow();
        HandlerTestSupport.BlockingStart blockingStart = new HandlerTestSupport.BlockingStart(5L);

        when(tokenService.consume("tok-1", "wf-1")).thenReturn(OptionalLong.of(1L));
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(blockingStart);

        handler.afterConnectionEstablished(webSocketSession);

        String sessionId = blockingStart.awaitEntered();

        HandlerTestSupport.assertDecorates(
            voiceSessionRegistry.get(sessionId)
                .orElseThrow()
                .webSocketSession(),
            webSocketSession);

        blockingStart.release();

        verify(webSocketSession, timeout(2000)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testSocketClosedWhileStartingStopsTheEngineAndLeavesNoSilenceCheck() throws Exception {
        WebSocketSession webSocketSession = socket("tok-1", "&environmentId=1");
        Workflow workflow = HandlerTestSupport.voiceWorkflow();
        HandlerTestSupport.BlockingStart blockingStart = new HandlerTestSupport.BlockingStart(5L);

        when(tokenService.consume("tok-1", "wf-1")).thenReturn(OptionalLong.of(1L));
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(blockingStart);

        handler.afterConnectionEstablished(webSocketSession);

        String sessionId = blockingStart.awaitEntered();

        handler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL);

        blockingStart.release();

        verify(voiceSessionEngine, timeout(2000)).stop(5L);

        assertThat(handler.hasSilenceCheck(sessionId)).isFalse();
    }

    @Test
    void testStartsTheDraftTriggerWithTestModeInputs() throws Exception {
        WebSocketSession webSocketSession = socket("tok-1", "&environmentId=1");

        when(tokenService.consume("tok-1", "wf-1")).thenReturn(OptionalLong.of(1L));

        Workflow workflow = HandlerTestSupport.voiceWorkflow();

        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), eq(true), any()))
            .thenReturn(new VoiceSession(5L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(webSocketSession);

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);

        verify(webSocketSession, timeout(2000)).sendMessage(sent.capture());

        assertThat(sent.getValue()
            .getPayload())
                .contains("\"event\":\"connected\"")
                .contains("\"outputSampleRate\":24000")
                .contains("\"sessionId\"");

        handler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL);

        verify(voiceSessionEngine).stop(5L);
    }

    @Test
    void testEnvironmentIdTheTokenWasMintedForSelectsTheTestConnections() throws Exception {
        WebSocketSession webSocketSession = socket("tok-1", "");

        when(tokenService.consume("tok-1", "wf-1")).thenReturn(OptionalLong.of(3L));

        Workflow workflow = HandlerTestSupport.voiceWorkflow();

        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(6L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(webSocketSession);

        ArgumentCaptor<Map<String, ?>> inputs = ArgumentCaptor.captor();

        verify(voiceSessionEngine, timeout(2000)).start(
            any(), anyMap(), inputs.capture(), any(), any(), eq(3L), any(), eq(true), any());

        assertThat(voiceSessionConnectionResolver.testCalls).containsExactly("wf-1:trigger_1:3");
        assertThat(new HashMap<String, Object>(inputs.getValue()))
            .containsEntry("testMode", true)
            .containsKeys("sessionId", "startedAt");
    }

    @Test
    void testEnvironmentIdOtherThanTheTokensIsIgnored() throws Exception {
        WebSocketSession webSocketSession = socket("tok-1", "&environmentId=5");

        when(tokenService.consume("tok-1", "wf-1")).thenReturn(OptionalLong.of(3L));

        Workflow workflow = HandlerTestSupport.voiceWorkflow();

        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(7L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(webSocketSession);

        verify(voiceSessionEngine, timeout(2000)).start(
            any(), anyMap(), anyMap(), any(), any(), eq(3L), any(), eq(true), any());
        verify(webSocketSession, never()).close(CloseStatus.POLICY_VIOLATION);

        assertThat(voiceSessionConnectionResolver.testCalls).containsExactly("wf-1:trigger_1:3");
    }

    @Test
    void testStartFailureQuotingTheNoVoiceAgentMessageIsAFailureNotAHint() throws Exception {
        WebSocketSession webSocketSession = socket("tok-1", "&environmentId=1");
        Workflow workflow = HandlerTestSupport.voiceWorkflow();

        when(tokenService.consume("tok-1", "wf-1")).thenReturn(OptionalLong.of(1L));
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenThrow(new IllegalStateException(VoiceSessionEngine.NO_VOICE_AGENT_MESSAGE + " quoted by a provider"));

        handler.afterConnectionEstablished(webSocketSession);

        verify(webSocketSession, timeout(2000)).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void testWorkflowWithoutVoiceAgentIsRejectedWithAHint() throws Exception {
        WebSocketSession webSocketSession = socket("tok-1", "&environmentId=1");

        when(tokenService.consume("tok-1", "wf-1")).thenReturn(OptionalLong.of(1L));

        Workflow workflow = HandlerTestSupport.voiceWorkflow();

        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenThrow(new VoiceSessionEngine.NoVoiceAgentException());

        handler.afterConnectionEstablished(webSocketSession);

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);

        verify(webSocketSession, timeout(2000)).sendMessage(sent.capture());

        assertThat(sent.getValue()
            .getPayload()).contains("Add a Voice Agent to the trigger to test with voice");

        verify(webSocketSession, timeout(2000)).close(CloseStatus.BAD_DATA.withReason("voice_agent_missing"));
    }

    @Test
    void testInvalidTokenIsRejected() throws Exception {
        WebSocketSession webSocketSession = socket("bad", "&environmentId=1");

        when(tokenService.consume("bad", "wf-1")).thenReturn(OptionalLong.empty());

        handler.afterConnectionEstablished(webSocketSession);

        verify(webSocketSession).close(CloseStatus.POLICY_VIOLATION);
        verify(voiceSessionEngine, never()).start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(),
            any());
    }

    @Test
    void testSessionLimitCloseStopsTheProviderOffTheTimerThread() throws Exception {
        WorkflowTestWebSocketHandler timedHandler = new WorkflowTestWebSocketHandler(
            voiceSessionRegistry, voiceSessionEngine, workflowService, tokenService, voiceSessionConnectionResolver,
            1L);
        WebSocketSession webSocketSession = socket("tok-1", "&environmentId=1");
        Workflow workflow = HandlerTestSupport.voiceWorkflow();
        AtomicReference<Thread> stoppingThread = new AtomicReference<>();
        CountDownLatch stopped = new CountDownLatch(1);

        doAnswer(invocation -> {
            timedHandler.afterConnectionClosed(webSocketSession, invocation.getArgument(0));

            return null;
        }).when(webSocketSession)
            .close(any(CloseStatus.class));
        when(tokenService.consume("tok-1", "wf-1")).thenReturn(OptionalLong.of(1L));
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(5L, new WebSocketEmitter(), 24000, new SessionTranscript()));
        doAnswer(invocation -> {
            stoppingThread.set(Thread.currentThread());
            stopped.countDown();

            return null;
        }).when(voiceSessionEngine)
            .stop(5L);

        timedHandler.afterConnectionEstablished(webSocketSession);

        verify(webSocketSession, timeout(5000)).close(CloseStatus.NORMAL.withReason("session_limit"));

        assertThat(stopped.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(stoppingThread.get()
            .isVirtual()).as("provider stop runs off the timer thread")
                .isTrue();
    }

    @Test
    void testSilenceTimeoutClosesTheTestSocketWithTheSilenceReason() throws Exception {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-09-08T10:00:00Z"));
        VoiceSessionRegistry clockedRegistry = new VoiceSessionRegistry(now::get);
        WorkflowTestWebSocketHandler silentHandler = new WorkflowTestWebSocketHandler(
            clockedRegistry, voiceSessionEngine, workflowService, tokenService, voiceSessionConnectionResolver);
        WebSocketSession webSocketSession = socket("tok-1", "&environmentId=1");
        Workflow workflow = HandlerTestSupport.voiceWorkflow();

        when(tokenService.consume("tok-1", "wf-1")).thenReturn(OptionalLong.of(1L));
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(5L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        silentHandler.afterConnectionEstablished(webSocketSession);

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);

        verify(webSocketSession, timeout(2000)).sendMessage(sent.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> connected = JsonUtils.read(sent.getValue()
            .getPayload(), Map.class);
        VoiceSessionRegistry.Session voiceSession = clockedRegistry.get(String.valueOf(connected.get("sessionId")))
            .orElseThrow();

        assertThat(connected).containsEntry("silenceTimeoutSeconds", 120);

        now.set(now.get()
            .plusSeconds(119));

        silentHandler.closeIfSilent(voiceSession, 120);

        verify(webSocketSession, never()).close(any(CloseStatus.class));

        now.set(now.get()
            .plusSeconds(2));

        silentHandler.closeIfSilent(voiceSession, 120);

        verify(webSocketSession).close(CloseStatus.NORMAL.withReason("silence_timeout"));
    }

    private static WebSocketSession socket(String token, String extraQuery) {
        WebSocketSession webSocketSession = mock(WebSocketSession.class);

        when(webSocketSession.getId()).thenReturn("ws-" + token);
        when(webSocketSession.getUri()).thenReturn(
            URI.create(
                "ws://localhost/internal/workflow-tests/wf-1/wss?sessionToken=" + token + "&sampleRate=16000" +
                    extraQuery));
        when(webSocketSession.isOpen()).thenReturn(true);

        return webSocketSession;
    }
}
