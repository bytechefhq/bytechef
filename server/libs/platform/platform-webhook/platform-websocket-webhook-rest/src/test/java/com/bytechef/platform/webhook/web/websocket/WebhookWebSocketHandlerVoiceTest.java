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

import static com.bytechef.platform.webhook.web.websocket.HandlerTestSupport.OTHER_WEBHOOK_ID;
import static com.bytechef.platform.webhook.web.websocket.HandlerTestSupport.WEBHOOK_ID;
import static com.bytechef.platform.webhook.web.websocket.HandlerTestSupport.assertDecorates;
import static com.bytechef.platform.webhook.web.websocket.HandlerTestSupport.startLikeTheEngine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.webhook.voice.SessionTranscript;
import com.bytechef.platform.webhook.voice.VoiceSessionEngine;
import com.bytechef.platform.webhook.voice.VoiceSessionEngine.VoiceSession;
import com.bytechef.platform.webhook.voice.WebSocketEmitter;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
class WebhookWebSocketHandlerVoiceTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-09-08T10:00:00Z"));
    private final BrowserVoiceSessionTokenService tokenService = mock(BrowserVoiceSessionTokenService.class);
    private final VoiceSessionEngine voiceSessionEngine = mock(VoiceSessionEngine.class);
    private final VoiceSessionRegistry voiceSessionRegistry = new VoiceSessionRegistry(now::get);
    private final WorkflowContinuationHelper workflowContinuationHelper = mock(WorkflowContinuationHelper.class);
    private final HandlerTestSupport.NoConnections voiceSessionConnectionResolver =
        new HandlerTestSupport.NoConnections();
    private final WebhookWebSocketHandler handler = createHandler(new HandlerTestSupport.FixedTriggerResolver());

    @Test
    void testFreshSessionStartsTheEngineAndAnnouncesTheOutputSampleRate() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(webSocketSession);

        Map<String, Object> connected = awaitConnected(webSocketSession);

        assertThat(connected)
            .containsEntry("event", "connected")
            .containsEntry("id", WEBHOOK_ID)
            .containsEntry("outputSampleRate", 24000)
            .containsEntry("silenceTimeoutSeconds", 0)
            .containsKey("sessionId");

        ArgumentCaptor<Map<String, ?>> inputs = ArgumentCaptor.captor();

        verify(voiceSessionEngine).start(
            any(), anyMap(), inputs.capture(), any(), any(), any(), eq(PlatformType.AUTOMATION), eq(false), any());

        assertThat(new HashMap<String, Object>(inputs.getValue()))
            .containsEntry("sessionId", connected.get("sessionId"))
            .containsEntry("mainWorkflowExecutionId", WEBHOOK_ID)
            .containsKey("startedAt");
        assertThat(voiceSessionConnectionResolver.deployedCalls).containsExactly("wf-uuid:trigger_1");
    }

    @Test
    void testDeployedSessionStartsWithTheJobPrincipalAndItsEnvironment() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(webSocketSession);
        awaitConnected(webSocketSession);

        verify(voiceSessionEngine).start(
            any(), anyMap(), anyMap(), eq(7L), any(), eq(HandlerTestSupport.ENVIRONMENT_ID),
            eq(PlatformType.AUTOMATION), eq(false), any());
    }

    @Test
    void testABlockedContinuationJobDoesNotHoldBackAnotherSessionsTimer() throws Exception {
        CountDownLatch releaseContinuationJob = new CountDownLatch(1);
        AtomicBoolean firstContinuationJob = new AtomicBoolean(true);

        @SuppressWarnings("unchecked")
        WebhookWebSocketHandler timedHandler = new WebhookWebSocketHandler(
            tokenService, new HandlerTestSupport.FixedTriggerResolver(),
            new VoiceMetricsRecorder(mock(ObjectProvider.class)), voiceSessionConnectionResolver, voiceSessionEngine,
            voiceSessionRegistry, workflowContinuationHelper, 1L);
        WebSocketSession firstSocket = closingSocket(timedHandler, "tok-1");
        WebSocketSession secondSocket = closingSocket(timedHandler, "tok-2");

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(tokenService.consume("tok-2", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()))
            .thenReturn(new VoiceSession(2L, new WebSocketEmitter(), 24000, new SessionTranscript()));
        doAnswer(invocation -> {
            if (firstContinuationJob.compareAndSet(true, false)) {
                awaitLatch(releaseContinuationJob);
            }

            return null;
        }).when(workflowContinuationHelper)
            .createContinuationJob(eq(WEBHOOK_ID), any());

        timedHandler.afterConnectionEstablished(firstSocket);
        timedHandler.afterConnectionEstablished(secondSocket);

        awaitConnected(firstSocket);
        awaitConnected(secondSocket);

        try {
            CloseStatus sessionLimit = CloseStatus.NORMAL.withReason("session_limit");

            verify(firstSocket, timeout(4000)).close(sessionLimit);
            verify(secondSocket, timeout(4000)).close(sessionLimit);
        } finally {
            releaseContinuationJob.countDown();
        }

        verify(workflowContinuationHelper, timeout(2000).times(2)).createContinuationJob(eq(WEBHOOK_ID), any());
    }

    private static WebSocketSession closingSocket(WebhookWebSocketHandler webhookWebSocketHandler, String token)
        throws Exception {

        WebSocketSession webSocketSession = openSocket(token, null);

        doAnswer(invocation -> {
            webhookWebSocketHandler.afterConnectionClosed(webSocketSession, invocation.getArgument(0));

            return null;
        }).when(webSocketSession)
            .close(any(CloseStatus.class));

        return webSocketSession;
    }

    @Test
    void testShutdownFinalizesEveryLiveSessionAsServerShutdown() throws Exception {
        WebSocketSession firstSocket = openSocket("tok-1", null);
        WebSocketSession secondSocket = openSocket("tok-2", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(tokenService.consume("tok-2", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()))
            .thenReturn(new VoiceSession(2L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(firstSocket);
        handler.afterConnectionEstablished(secondSocket);

        awaitConnected(firstSocket);
        awaitConnected(secondSocket);

        handler.stop();

        ArgumentCaptor<Map<String, Object>> output = ArgumentCaptor.captor();

        verify(workflowContinuationHelper, times(2)).createContinuationJob(eq(WEBHOOK_ID), output.capture());
        verify(voiceSessionEngine).stop(1L);
        verify(voiceSessionEngine).stop(2L);
        verify(firstSocket).close(CloseStatus.NORMAL.withReason("server_shutdown"));
        verify(secondSocket).close(CloseStatus.NORMAL.withReason("server_shutdown"));

        assertThat(output.getAllValues())
            .extracting(sessionOutput -> sessionOutput.get("endReason"))
            .containsExactly("server_shutdown", "server_shutdown");
        assertThat(handler.isRunning()).isFalse();
    }

    @Test
    void testDisabledWorkflowIsRefusedAtStartWithoutAContinuationJob() throws Exception {
        WebhookWebSocketHandler disabledHandler = createHandler(new HandlerTestSupport.FixedTriggerResolver(0, false));
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);

        disabledHandler.afterConnectionEstablished(webSocketSession);

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);

        verify(webSocketSession, timeout(2000)).sendMessage(sent.capture());

        assertThat(sent.getValue()
            .getPayload())
                .contains("\"type\":\"error\"")
                .contains("disabled");

        verify(webSocketSession, timeout(2000)).close(CloseStatus.POLICY_VIOLATION.withReason("workflow_disabled"));

        disabledHandler.afterConnectionClosed(webSocketSession,
            CloseStatus.POLICY_VIOLATION.withReason("workflow_disabled"));

        verify(voiceSessionEngine, never()).start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(),
            any());
        verify(workflowContinuationHelper, after(300).never()).createContinuationJob(any(), any());
    }

    @Test
    void testSessionStartAndEndRunInTheWebhooksTenant() throws Exception {
        String tenantWebhookId = TenantContext.callWithTenantId(
            "tenant_a", () -> WorkflowExecutionId.of(PlatformType.AUTOMATION, 7L, "wf-uuid", "trigger_1")
                .toString());
        HandlerTestSupport.FixedTriggerResolver triggerResolver = new HandlerTestSupport.FixedTriggerResolver();
        WebhookWebSocketHandler tenantHandler = createHandler(triggerResolver);
        WebSocketSession webSocketSession = openSocket(tenantWebhookId, "tok-1", null);
        List<String> continuationTenantIds = new CopyOnWriteArrayList<>();

        when(tokenService.consume("tok-1", tenantWebhookId)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));
        doAnswer(invocation -> {
            continuationTenantIds.add(TenantContext.getCurrentTenantId());

            return null;
        }).when(workflowContinuationHelper)
            .createContinuationJob(eq(tenantWebhookId), any());

        tenantHandler.afterConnectionEstablished(webSocketSession);
        awaitConnected(webSocketSession);

        tenantHandler.recordServerClose(webSocketSession.getId(), "session_limit");
        tenantHandler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL.withReason("session_limit"));

        verify(workflowContinuationHelper, timeout(2000)).createContinuationJob(eq(tenantWebhookId), any());

        assertThat(triggerResolver.resolvedTenantIds).containsExactly("tenant_a");
        assertThat(continuationTenantIds).containsExactly("tenant_a");
    }

    @Test
    void testInvalidTokenIsRejected() throws Exception {
        WebSocketSession webSocketSession = openSocket("bad", null);

        when(tokenService.consume("bad", WEBHOOK_ID)).thenReturn(false);

        handler.afterConnectionEstablished(webSocketSession);

        verify(webSocketSession).close(CloseStatus.POLICY_VIOLATION);
        verify(voiceSessionEngine, never()).start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(),
            any());
    }

    @Test
    void testEngineStartFailureSendsTheErrorAndClosesAsProviderError() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenThrow(new IllegalStateException("Failed to start the Voice Agent: provider unreachable"));

        handler.afterConnectionEstablished(webSocketSession);

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);

        verify(webSocketSession, timeout(2000)).sendMessage(sent.capture());

        assertThat(sent.getValue()
            .getPayload())
                .contains("\"type\":\"error\"")
                .contains("provider unreachable");

        verify(webSocketSession, timeout(2000)).close(CloseStatus.POLICY_VIOLATION.withReason("provider_error"));
    }

    @Test
    void testStartFailureQuotingTheNoVoiceAgentMessageIsStillAProviderError() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenThrow(new IllegalStateException(VoiceSessionEngine.NO_VOICE_AGENT_MESSAGE + " quoted by a provider"));

        handler.afterConnectionEstablished(webSocketSession);

        verify(webSocketSession, timeout(2000)).close(CloseStatus.POLICY_VIOLATION.withReason("provider_error"));
    }

    @Test
    void testRefusedSessionWithoutAVoiceAgentIsCountedAsAnError() throws Exception {
        VoiceMetricsRecorder recorder = mock(VoiceMetricsRecorder.class);
        WebhookWebSocketHandler countingHandler =
            createHandler(new HandlerTestSupport.FixedTriggerResolver(), recorder);
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenThrow(new VoiceSessionEngine.NoVoiceAgentException());

        countingHandler.afterConnectionEstablished(webSocketSession);

        verify(webSocketSession, timeout(2000)).close(any(CloseStatus.class));
        verify(recorder).recordSessionOpened();
        verify(recorder).recordSessionError();
    }

    @Test
    void testRefusedSessionOfADisabledWorkflowIsCountedAsAnError() throws Exception {
        VoiceMetricsRecorder recorder = mock(VoiceMetricsRecorder.class);
        WebhookWebSocketHandler countingHandler = createHandler(
            new HandlerTestSupport.FixedTriggerResolver(0, false), recorder);
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);

        countingHandler.afterConnectionEstablished(webSocketSession);

        verify(webSocketSession, timeout(2000)).close(any(CloseStatus.class));
        verify(recorder).recordSessionOpened();
        verify(recorder).recordSessionError();
    }

    @Test
    void testTriggerWithoutAVoiceAgentIsRefusedWithoutAContinuationJob() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenThrow(new VoiceSessionEngine.NoVoiceAgentException());

        handler.afterConnectionEstablished(webSocketSession);

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);

        verify(webSocketSession, timeout(2000)).sendMessage(sent.capture());
        verify(webSocketSession, timeout(2000)).close(CloseStatus.POLICY_VIOLATION.withReason("voice_agent_missing"));

        assertThat(sent.getValue()
            .getPayload())
                .contains("\"type\":\"error\"")
                .contains("No Voice Agent configured on the trigger");

        handler.afterConnectionClosed(webSocketSession, CloseStatus.POLICY_VIOLATION.withReason("voice_agent_missing"));

        verify(workflowContinuationHelper, after(300).never()).createContinuationJob(any(), any());
    }

    @Test
    void testInboundAudioReachesTheElementAndResetsTheSilenceClock() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        WebSocketEmitter emitter = new WebSocketEmitter();
        AtomicReference<byte[]> received = new AtomicReference<>();

        emitter.addBinaryMessageListener(received::set);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, emitter, 24000, new SessionTranscript()));
        when(voiceSessionEngine.emitter(1L)).thenReturn(Optional.of(emitter));

        handler.afterConnectionEstablished(webSocketSession);

        String sessionId = String.valueOf(awaitConnected(webSocketSession).get("sessionId"));

        now.set(now.get()
            .plusSeconds(42));

        handler.handleBinaryMessage(webSocketSession, new BinaryMessage("audio".getBytes(StandardCharsets.UTF_8)));

        assertThat(received.get()).isEqualTo("audio".getBytes(StandardCharsets.UTF_8));
        assertThat(voiceSessionRegistry.get(sessionId))
            .get()
            .extracting(VoiceSessionRegistry.Session::lastInboundAudioAt)
            .isEqualTo(now.get());
    }

    @Test
    void testSilenceTimeoutClosesTheSocketWithTheSilenceReason() throws Exception {
        WebhookWebSocketHandler silentHandler = createHandler(new HandlerTestSupport.FixedTriggerResolver(10));
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        silentHandler.afterConnectionEstablished(webSocketSession);

        Map<String, Object> connected = awaitConnected(webSocketSession);
        String sessionId = String.valueOf(connected.get("sessionId"));

        assertThat(connected).containsEntry("silenceTimeoutSeconds", 10);

        now.set(now.get()
            .plusSeconds(9));

        silentHandler.closeIfSilent(sessionId);

        verify(webSocketSession, never()).close(any(CloseStatus.class));

        now.set(now.get()
            .plusSeconds(2));

        silentHandler.closeIfSilent(sessionId);

        verify(webSocketSession).close(CloseStatus.NORMAL.withReason("silence_timeout"));
    }

    @Test
    void testServerInitiatedCloseFinalizesWithTranscriptAndReason() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        SessionTranscript transcript = new SessionTranscript();

        transcript.user("hello");
        transcript.toolCall("lookupOrder", Map.of("id", "4411"), "shipped");

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, transcript));

        handler.afterConnectionEstablished(webSocketSession);

        String sessionId = String.valueOf(awaitConnected(webSocketSession).get("sessionId"));

        now.set(now.get()
            .plusSeconds(184));

        handler.recordServerClose(webSocketSession.getId(), "silence_timeout");
        handler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL.withReason("silence_timeout"));

        ArgumentCaptor<Map<String, Object>> output = ArgumentCaptor.captor();

        verify(workflowContinuationHelper).createContinuationJob(eq(WEBHOOK_ID), output.capture());
        verify(voiceSessionEngine).stop(1L);

        assertThat(output.getValue())
            .containsEntry("sessionId", sessionId)
            .containsEntry("endReason", "silence_timeout")
            .containsEntry("startedAt", "2026-09-08T10:00:00Z")
            .containsEntry("durationSeconds", 184L)
            .containsKeys("durationSeconds", "transcript", "toolCalls");
        assertThat((List<?>) output.getValue()
            .get("transcript")).hasSize(1);
        assertThat((List<?>) output.getValue()
            .get("toolCalls")).hasSize(1);
        assertThat(voiceSessionRegistry.get(sessionId)).isEmpty();
    }

    @Test
    void testSessionLimitCloseIsAlsoServerInitiated() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(webSocketSession);
        awaitConnected(webSocketSession);

        handler.recordServerClose(webSocketSession.getId(), "session_limit");
        handler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL.withReason("session_limit"));

        ArgumentCaptor<Map<String, Object>> output = ArgumentCaptor.captor();

        verify(workflowContinuationHelper).createContinuationJob(eq(WEBHOOK_ID), output.capture());

        assertThat(output.getValue()).containsEntry("endReason", "session_limit");
    }

    @Test
    void testClientCloseFrameCarryingAServerReasonIsDetachedNotFinalized() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(webSocketSession);

        String sessionId = String.valueOf(awaitConnected(webSocketSession).get("sessionId"));

        handler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL.withReason("silence_timeout"));

        verify(voiceSessionEngine, never()).stop(1L);
        verify(workflowContinuationHelper, never()).createContinuationJob(any(), any());

        assertThat(voiceSessionRegistry.get(sessionId)
            .orElseThrow()
            .detachedAt()).isNotNull();
    }

    @Test
    void testServerCloseEndsTheSessionEvenWhenTheContainerReportsNoCloseFrame() throws Exception {
        WebhookWebSocketHandler silentHandler = createHandler(new HandlerTestSupport.FixedTriggerResolver(10));
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        silentHandler.afterConnectionEstablished(webSocketSession);

        String sessionId = String.valueOf(awaitConnected(webSocketSession).get("sessionId"));

        now.set(now.get()
            .plusSeconds(11));

        silentHandler.closeIfSilent(sessionId);

        silentHandler.afterConnectionClosed(webSocketSession, CloseStatus.NO_CLOSE_FRAME);

        ArgumentCaptor<Map<String, Object>> output = ArgumentCaptor.captor();

        verify(workflowContinuationHelper, timeout(2000)).createContinuationJob(eq(WEBHOOK_ID), output.capture());

        assertThat(output.getValue()).containsEntry("endReason", "silence_timeout");
    }

    @Test
    void testUpgradeDuringShutdownIsRefusedWithoutConsumingTheToken() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        handler.stop();

        handler.afterConnectionEstablished(webSocketSession);

        verify(webSocketSession).close(CloseStatus.SERVICE_RESTARTED.withReason("server_shutdown"));
        verify(tokenService, never()).consume(any(), any());
        verify(voiceSessionEngine, never()).start(
            any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void testRestartedHandlerAcceptsUpgradesAgain() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.stop();
        handler.start();

        handler.afterConnectionEstablished(webSocketSession);

        assertThat(awaitConnected(webSocketSession)).containsEntry("event", "connected");

        verify(webSocketSession, never()).close(CloseStatus.SERVICE_RESTARTED.withReason("server_shutdown"));
    }

    @Test
    void testStartThatLosesTheRaceToShutdownEndsAsServerShutdown() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        HandlerTestSupport.BlockingStart blockingStart = new HandlerTestSupport.BlockingStart(1L);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(blockingStart);

        handler.afterConnectionEstablished(webSocketSession);

        blockingStart.awaitEntered();

        Thread stopping = new Thread(handler::stop);

        stopping.start();
        stopping.join(300);

        assertThat(stopping.isAlive()).as("stop() waits for the in-flight start")
            .isTrue();

        blockingStart.release();

        stopping.join(5000);

        assertThat(stopping.isAlive()).isFalse();

        ArgumentCaptor<Map<String, Object>> output = ArgumentCaptor.captor();

        verify(workflowContinuationHelper).createContinuationJob(eq(WEBHOOK_ID), output.capture());
        verify(voiceSessionEngine).stop(1L);
        verify(webSocketSession).close(CloseStatus.NORMAL.withReason("server_shutdown"));

        assertThat(output.getValue()).containsEntry("endReason", "server_shutdown");
    }

    @Test
    void testSessionEndedWhileStartingIsStillStoppedWhenShutdownBegins() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        HandlerTestSupport.BlockingStart blockingStart = new HandlerTestSupport.BlockingStart(1L);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(blockingStart);

        handler.afterConnectionEstablished(webSocketSession);

        String sessionId = blockingStart.awaitEntered();

        handler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL.withReason("client_closed"));

        assertThat(voiceSessionRegistry.get(sessionId)).isEmpty();
        verify(voiceSessionEngine, never()).stop(1L);

        Thread stopping = new Thread(handler::stop);

        stopping.start();

        awaitJoining(stopping);

        blockingStart.release();

        stopping.join(5000);

        assertThat(stopping.isAlive()).isFalse();

        verify(voiceSessionEngine).stop(1L);

        assertThat(handler.hasLiveSession(sessionId)).isFalse();
    }

    @Test
    void testStopWaitsForAnEndHandedOffTheTimerThread() throws Exception {
        CountDownLatch continuationJobEntered = new CountDownLatch(1);
        CountDownLatch releaseContinuationJob = new CountDownLatch(1);

        @SuppressWarnings("unchecked")
        WebhookWebSocketHandler timedHandler = new WebhookWebSocketHandler(
            tokenService, new HandlerTestSupport.FixedTriggerResolver(),
            new VoiceMetricsRecorder(mock(ObjectProvider.class)), voiceSessionConnectionResolver, voiceSessionEngine,
            voiceSessionRegistry, workflowContinuationHelper, 1L);
        WebSocketSession webSocketSession = closingSocket(timedHandler, "tok-1");

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));
        doAnswer(invocation -> {
            continuationJobEntered.countDown();

            assertThat(releaseContinuationJob.await(5, TimeUnit.SECONDS)).isTrue();

            return null;
        }).when(workflowContinuationHelper)
            .createContinuationJob(eq(WEBHOOK_ID), any());

        timedHandler.afterConnectionEstablished(webSocketSession);

        assertThat(continuationJobEntered.await(5, TimeUnit.SECONDS)).isTrue();

        Thread stopping = new Thread(timedHandler::stop);

        stopping.start();
        stopping.join(300);

        assertThat(stopping.isAlive()).as("stop() waits for the handed-off end")
            .isTrue();

        releaseContinuationJob.countDown();

        stopping.join(5000);

        assertThat(stopping.isAlive()).isFalse();
    }

    @Test
    void testClientDropIsDetachedNotFinalized() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(webSocketSession);

        String sessionId = String.valueOf(awaitConnected(webSocketSession).get("sessionId"));

        handler.afterConnectionClosed(webSocketSession, CloseStatus.NO_CLOSE_FRAME);

        verify(voiceSessionEngine, never()).stop(1L);
        verify(workflowContinuationHelper, never()).createContinuationJob(any(), any());

        assertThat(voiceSessionRegistry.get(sessionId))
            .get()
            .extracting(VoiceSessionRegistry.Session::detachedAt)
            .isNotNull();
    }

    @Test
    void testDeliberateClientHangUpFinalizesWithoutAResumeWindow() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(webSocketSession);

        String sessionId = String.valueOf(awaitConnected(webSocketSession).get("sessionId"));

        handler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL.withReason("client_closed"));

        ArgumentCaptor<Map<String, Object>> output = ArgumentCaptor.captor();

        verify(workflowContinuationHelper, timeout(2000)).createContinuationJob(eq(WEBHOOK_ID), output.capture());
        verify(voiceSessionEngine, timeout(2000)).stop(1L);

        assertThat(output.getValue()).containsEntry("endReason", "client_closed");
        assertThat(voiceSessionRegistry.get(sessionId)).isEmpty();
    }

    @Test
    void testNormalCloseWithoutAReasonIsDetachedNotFinalized() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(webSocketSession);

        String sessionId = String.valueOf(awaitConnected(webSocketSession).get("sessionId"));

        handler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL);

        verify(voiceSessionEngine, after(300).never()).stop(1L);
        verify(workflowContinuationHelper, never()).createContinuationJob(any(), any());

        assertThat(voiceSessionRegistry.get(sessionId))
            .get()
            .extracting(VoiceSessionRegistry.Session::detachedAt)
            .isNotNull();
    }

    @Test
    void testClientClosedReasonOnAnAbnormalCloseCodeIsDetachedNotFinalized() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(webSocketSession);

        String sessionId = String.valueOf(awaitConnected(webSocketSession).get("sessionId"));

        handler.afterConnectionClosed(webSocketSession, CloseStatus.GOING_AWAY.withReason("client_closed"));

        verify(voiceSessionEngine, after(300).never()).stop(1L);
        verify(workflowContinuationHelper, never()).createContinuationJob(any(), any());

        assertThat(voiceSessionRegistry.get(sessionId))
            .get()
            .extracting(VoiceSessionRegistry.Session::detachedAt)
            .isNotNull();
    }

    @Test
    void testDetachedSessionIsFinalizedAsClientClosedOnceTheResumeWindowElapses() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(webSocketSession);

        String sessionId = String.valueOf(awaitConnected(webSocketSession).get("sessionId"));

        handler.afterConnectionClosed(webSocketSession, CloseStatus.NO_CLOSE_FRAME);

        Instant detachedAt = voiceSessionRegistry.get(sessionId)
            .orElseThrow()
            .detachedAt();

        now.set(now.get()
            .plus(VoiceSessionRegistry.RESUME_WINDOW)
            .plus(Duration.ofSeconds(1)));

        handler.finalizeIfStillDetached(sessionId, detachedAt);

        ArgumentCaptor<Map<String, Object>> output = ArgumentCaptor.captor();

        verify(voiceSessionEngine).stop(1L);
        verify(workflowContinuationHelper).createContinuationJob(eq(WEBHOOK_ID), output.capture());

        assertThat(output.getValue()).containsEntry("endReason", "client_closed");
        assertThat(voiceSessionRegistry.get(sessionId)).isEmpty();
    }

    @Test
    void testResumeReattachesTheLiveSessionWithoutRestartingTheEngine() throws Exception {
        WebSocketSession firstSocket = openSocket("tok-1", null);
        WebSocketEmitter emitter = new WebSocketEmitter();
        AtomicReference<byte[]> received = new AtomicReference<>();

        emitter.addBinaryMessageListener(received::set);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(tokenService.consume("tok-2", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(invocation -> {
                Consumer<WebSocketEmitter> bridge = invocation.getArgument(8);

                bridge.accept(emitter);

                return new VoiceSession(1L, emitter, 24000, new SessionTranscript());
            });
        when(voiceSessionEngine.emitter(1L)).thenReturn(Optional.of(emitter));

        handler.afterConnectionEstablished(firstSocket);

        String sessionId = String.valueOf(awaitConnected(firstSocket).get("sessionId"));

        handler.afterConnectionClosed(firstSocket, CloseStatus.NO_CLOSE_FRAME);

        WebSocketSession secondSocket = openSocket("tok-2", sessionId);

        handler.afterConnectionEstablished(secondSocket);

        Map<String, Object> connected = awaitConnected(secondSocket);

        assertThat(connected)
            .containsEntry("sessionId", sessionId)
            .containsEntry("resumed", true)
            .containsEntry("outputSampleRate", 24000);

        verify(voiceSessionEngine, times(1)).start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(),
            any());

        emitter.send(Map.of("type", "assistant_text", "text", "still here"));

        ArgumentCaptor<TextMessage> toSecond = ArgumentCaptor.forClass(TextMessage.class);

        verify(secondSocket, times(2)).sendMessage(toSecond.capture());
        verify(firstSocket, times(1)).sendMessage(any(TextMessage.class));

        assertThat(toSecond.getValue()
            .getPayload()).contains("still here");

        handler.handleBinaryMessage(secondSocket, new BinaryMessage("again".getBytes(StandardCharsets.UTF_8)));

        assertThat(received.get()).isEqualTo("again".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testResumeWithATokenForAnotherWebhookStartsAFreshSessionAndLeavesTheOriginalDetached() throws Exception {
        WebSocketSession firstSocket = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(tokenService.consume("tok-2", OTHER_WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()))
            .thenReturn(new VoiceSession(2L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(firstSocket);

        String sessionId = String.valueOf(awaitConnected(firstSocket).get("sessionId"));

        handler.afterConnectionClosed(firstSocket, CloseStatus.NO_CLOSE_FRAME);

        WebSocketSession otherWebhookSocket = openSocket(OTHER_WEBHOOK_ID, "tok-2", sessionId);

        handler.afterConnectionEstablished(otherWebhookSocket);

        Map<String, Object> connected = awaitConnected(otherWebhookSocket);

        assertThat(connected.get("sessionId")).isNotEqualTo(sessionId);
        assertThat(connected)
            .containsEntry("id", OTHER_WEBHOOK_ID)
            .doesNotContainKey("resumed");

        verify(voiceSessionEngine, times(2)).start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(),
            any());

        VoiceSessionRegistry.Session originalSession = voiceSessionRegistry.get(sessionId)
            .orElseThrow();

        assertThat(originalSession.detachedAt()).isNotNull();
        assertThat(originalSession.webSocketSession()).isNull();
    }

    @Test
    void testResumeOfAnUnknownSessionStartsAFreshOne() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", "no-such-session");

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(webSocketSession);

        Map<String, Object> connected = awaitConnected(webSocketSession);

        assertThat(connected.get("sessionId")).isNotEqualTo("no-such-session");
        assertThat(connected).doesNotContainKey("resumed");
    }

    @Test
    void testSocketWithoutSessionTokenIsRefused() throws Exception {
        WebSocketSession webSocketSession = mock(WebSocketSession.class);

        when(webSocketSession.getId()).thenReturn("ws-plain");
        when(webSocketSession.getUri()).thenReturn(URI.create("ws://localhost/webhooks/" + WEBHOOK_ID + "/wss"));
        when(webSocketSession.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(webSocketSession);
        handler.handleTextMessage(webSocketSession, new TextMessage("{\"action\":\"execute\"}"));

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);

        verify(webSocketSession).sendMessage(sent.capture());
        verify(webSocketSession).close(CloseStatus.POLICY_VIOLATION);

        assertThat(sent.getValue()
            .getPayload())
                .contains("\"type\":\"error\"")
                .contains("session token");

        handler.afterConnectionClosed(webSocketSession, CloseStatus.POLICY_VIOLATION);

        verify(voiceSessionEngine, never()).start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(),
            any());
        verify(workflowContinuationHelper, never()).createContinuationJob(any(), any());
    }

    @Test
    void testKeepaliveFromAMutedCallerResetsTheSilenceClockWithoutReachingTheProvider() throws Exception {
        WebhookWebSocketHandler silentHandler = createHandler(new HandlerTestSupport.FixedTriggerResolver(10));
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        WebSocketEmitter emitter = new WebSocketEmitter();
        List<Object> providerMessages = new CopyOnWriteArrayList<>();

        emitter.addMessageListener(providerMessages::add);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, emitter, 24000, new SessionTranscript()));
        when(voiceSessionEngine.emitter(1L)).thenReturn(Optional.of(emitter));

        silentHandler.afterConnectionEstablished(webSocketSession);

        String sessionId = String.valueOf(awaitConnected(webSocketSession).get("sessionId"));

        now.set(now.get()
            .plusSeconds(9));

        silentHandler.handleTextMessage(
            webSocketSession, new TextMessage("{\"action\":\"keepalive\",\"type\":\"control\"}"));

        now.set(now.get()
            .plusSeconds(9));

        silentHandler.closeIfSilent(sessionId);

        verify(webSocketSession, never()).close(any(CloseStatus.class));

        assertThat(providerMessages).isEmpty();
    }

    @Test
    void testTextOnAVoiceSessionIsNotEchoedBack() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        handler.afterConnectionEstablished(webSocketSession);
        awaitConnected(webSocketSession);

        handler.handleTextMessage(webSocketSession, new TextMessage("{\"action\":\"end\",\"type\":\"control\"}"));

        verify(webSocketSession, after(300).times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testProviderErrorMidSessionSendsAnErrorAndEndsTheSessionAsProviderError() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        WebSocketEmitter emitter = new WebSocketEmitter();

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(invocation -> startLikeTheEngine(invocation, emitter, 1L));

        handler.afterConnectionEstablished(webSocketSession);
        awaitConnected(webSocketSession);

        emitter.error(new RuntimeException("provider down"));

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);

        verify(webSocketSession, times(2)).sendMessage(sent.capture());

        assertThat(sent.getAllValues()
            .getLast()
            .getPayload())
                .contains("\"type\":\"error\"")
                .contains("provider down");

        CloseStatus providerError = CloseStatus.NORMAL.withReason("provider_error");

        verify(webSocketSession).close(providerError);

        handler.afterConnectionClosed(webSocketSession, providerError);

        ArgumentCaptor<Map<String, Object>> output = ArgumentCaptor.captor();

        verify(workflowContinuationHelper).createContinuationJob(eq(WEBHOOK_ID), output.capture());
        verify(voiceSessionEngine).stop(1L);

        assertThat(output.getValue()).containsEntry("endReason", "provider_error");
    }

    @Test
    void testProviderCloseEndsTheSessionAsProviderClosed() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        WebSocketEmitter emitter = new WebSocketEmitter();

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(invocation -> startLikeTheEngine(invocation, emitter, 1L));

        handler.afterConnectionEstablished(webSocketSession);

        String sessionId = String.valueOf(awaitConnected(webSocketSession).get("sessionId"));

        emitter.complete();

        verify(webSocketSession, timeout(2000)).close(CloseStatus.NORMAL.withReason("provider_closed"));

        ArgumentCaptor<Map<String, Object>> output = ArgumentCaptor.captor();

        verify(workflowContinuationHelper, timeout(2000)).createContinuationJob(eq(WEBHOOK_ID), output.capture());

        assertThat(output.getValue()).containsEntry("endReason", "provider_closed");
        assertThat(voiceSessionRegistry.get(sessionId)).isEmpty();
    }

    @Test
    void testProviderCloseWhileDetachedEndsTheSessionAsProviderClosed() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        WebSocketEmitter emitter = new WebSocketEmitter();

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(invocation -> startLikeTheEngine(invocation, emitter, 1L));

        handler.afterConnectionEstablished(webSocketSession);

        String sessionId = String.valueOf(awaitConnected(webSocketSession).get("sessionId"));

        handler.afterConnectionClosed(webSocketSession, CloseStatus.NO_CLOSE_FRAME);

        emitter.complete();

        ArgumentCaptor<Map<String, Object>> output = ArgumentCaptor.captor();

        verify(workflowContinuationHelper, timeout(2000)).createContinuationJob(eq(WEBHOOK_ID), output.capture());

        assertThat(output.getValue()).containsEntry("endReason", "provider_closed");
        assertThat(voiceSessionRegistry.get(sessionId)).isEmpty();
    }

    @Test
    void testProviderErrorWhileDetachedEndsTheSessionAsProviderError() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        WebSocketEmitter emitter = new WebSocketEmitter();

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(invocation -> startLikeTheEngine(invocation, emitter, 1L));

        handler.afterConnectionEstablished(webSocketSession);

        String sessionId = String.valueOf(awaitConnected(webSocketSession).get("sessionId"));

        handler.afterConnectionClosed(webSocketSession, CloseStatus.NO_CLOSE_FRAME);

        emitter.error(new RuntimeException("provider down"));

        ArgumentCaptor<Map<String, Object>> output = ArgumentCaptor.captor();

        verify(workflowContinuationHelper, timeout(2000)).createContinuationJob(eq(WEBHOOK_ID), output.capture());

        assertThat(output.getValue()).containsEntry("endReason", "provider_error");
        assertThat(voiceSessionRegistry.get(sessionId)).isEmpty();
    }

    @Test
    void testProviderOutputWhileDetachedIsDroppedWithoutReachingTheOldSocket() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        WebSocketEmitter emitter = new WebSocketEmitter();

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(invocation -> startLikeTheEngine(invocation, emitter, 1L));

        handler.afterConnectionEstablished(webSocketSession);
        awaitConnected(webSocketSession);

        handler.afterConnectionClosed(webSocketSession, CloseStatus.NO_CLOSE_FRAME);

        emitter.send(Map.of("type", "assistant_text", "text", "into the gap"));
        emitter.sendBinary(new byte[] {
            1, 0
        });

        verify(webSocketSession, after(300).times(1)).sendMessage(any());
    }

    @Test
    void testHandlerInitiatedStopIsNotReportedAsProviderClosed() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        WebSocketEmitter emitter = new WebSocketEmitter();

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(invocation -> startLikeTheEngine(invocation, emitter, 1L));
        doAnswer(invocation -> {
            emitter.complete();

            return null;
        }).when(voiceSessionEngine)
            .stop(1L);

        handler.afterConnectionEstablished(webSocketSession);
        awaitConnected(webSocketSession);

        handler.recordServerClose(webSocketSession.getId(), "session_limit");
        handler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL.withReason("session_limit"));

        ArgumentCaptor<Map<String, Object>> output = ArgumentCaptor.captor();

        verify(workflowContinuationHelper, timeout(2000)).createContinuationJob(eq(WEBHOOK_ID), output.capture());
        verify(voiceSessionEngine, timeout(2000)).stop(1L);
        verify(webSocketSession, after(300).never()).close(CloseStatus.NORMAL.withReason("provider_closed"));
        verify(workflowContinuationHelper, times(1)).createContinuationJob(any(), any());

        assertThat(output.getValue()).containsEntry("endReason", "session_limit");
    }

    @Test
    void testRegisteredAndResumedSocketsAreWrappedInAConcurrentDecorator() throws Exception {
        WebSocketSession firstSocket = openSocket("tok-1", null);
        WebSocketEmitter emitter = new WebSocketEmitter();

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(tokenService.consume("tok-2", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(invocation -> startLikeTheEngine(invocation, emitter, 1L));

        handler.afterConnectionEstablished(firstSocket);

        String sessionId = String.valueOf(awaitConnected(firstSocket).get("sessionId"));

        assertDecorates(
            voiceSessionRegistry.get(sessionId)
                .orElseThrow()
                .webSocketSession(),
            firstSocket);

        handler.afterConnectionClosed(firstSocket, CloseStatus.NO_CLOSE_FRAME);

        WebSocketSession secondSocket = openSocket("tok-2", sessionId);

        handler.afterConnectionEstablished(secondSocket);
        awaitConnected(secondSocket);

        assertDecorates(
            voiceSessionRegistry.get(sessionId)
                .orElseThrow()
                .webSocketSession(),
            secondSocket);
    }

    @Test
    void testConnectedAndBridgeOutputAreSentThroughTheDecorator() throws Exception {
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        WebSocketEmitter emitter = new WebSocketEmitter();

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(invocation -> {
                VoiceSession voiceSession = startLikeTheEngine(invocation, emitter, 1L);
                Map<String, ?> inputs = invocation.getArgument(2);

                WebSocketSession storedSocket = voiceSessionRegistry.get(String.valueOf(inputs.get("sessionId")))
                    .map(VoiceSessionRegistry.Session::webSocketSession)
                    .orElseThrow();

                storedSocket.close(CloseStatus.GOING_AWAY);

                return voiceSession;
            });

        handler.afterConnectionEstablished(webSocketSession);

        verify(voiceSessionEngine, timeout(2000)).start(any(), anyMap(), anyMap(), any(), any(), any(), any(),
            anyBoolean(), any());
        verify(webSocketSession).close(CloseStatus.GOING_AWAY);

        emitter.send(Map.of("type", "assistant_text", "text", "bridge output"));

        verify(webSocketSession, after(500).never()).sendMessage(any());
    }

    @Test
    void testSessionDetachedWhileStartingKeepsNoTimersOnceFinalized() throws Exception {
        WebhookWebSocketHandler silentHandler = createHandler(new HandlerTestSupport.FixedTriggerResolver(10));
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        HandlerTestSupport.BlockingStart blockingStart = new HandlerTestSupport.BlockingStart(1L);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(blockingStart);

        silentHandler.afterConnectionEstablished(webSocketSession);

        String sessionId = blockingStart.awaitEntered();

        silentHandler.afterConnectionClosed(webSocketSession, CloseStatus.NO_CLOSE_FRAME);

        Instant detachedAt = voiceSessionRegistry.get(sessionId)
            .orElseThrow()
            .detachedAt();

        blockingStart.release();

        awaitLiveSession(silentHandler, sessionId);

        now.set(now.get()
            .plus(VoiceSessionRegistry.RESUME_WINDOW)
            .plusSeconds(1));

        silentHandler.finalizeIfStillDetached(sessionId, detachedAt);

        verify(voiceSessionEngine).stop(1L);

        assertThat(silentHandler.scheduledTimerCount(sessionId)).isZero();
    }

    @Test
    void testClientDropRacingStartLeavesNoTimersOnTheDetachedSession() throws Exception {
        AtomicReference<WebhookWebSocketHandler> handlerReference = new AtomicReference<>();
        CountDownLatch detachEntered = new CountDownLatch(1);

        VoiceSessionRegistry gatedRegistry = new VoiceSessionRegistry(now::get) {

            @Override
            public void detach(String sessionId) {
                detachEntered.countDown();

                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);

                while (handlerReference.get()
                    .scheduledTimerCount(sessionId) == 0 && System.nanoTime() < deadline) {

                    Thread.onSpinWait();
                }

                super.detach(sessionId);
            }
        };

        WebhookWebSocketHandler racingHandler = createHandler(
            new HandlerTestSupport.FixedTriggerResolver(10), gatedRegistry);
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        HandlerTestSupport.BlockingStart blockingStart = new HandlerTestSupport.BlockingStart(1L);

        handlerReference.set(racingHandler);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(blockingStart);

        racingHandler.afterConnectionEstablished(webSocketSession);

        String sessionId = blockingStart.awaitEntered();

        Thread drop = new Thread(
            () -> racingHandler.afterConnectionClosed(webSocketSession, CloseStatus.NO_CLOSE_FRAME));

        drop.start();

        assertThat(detachEntered.await(2, TimeUnit.SECONDS)).isTrue();

        blockingStart.release();

        drop.join(5000);

        assertThat(drop.isAlive()).isFalse();

        long settleDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);

        while (!racingHandler.hasLiveSession(sessionId) && System.nanoTime() < settleDeadline) {
            Thread.onSpinWait();
        }

        VoiceSessionRegistry.Session voiceSession = gatedRegistry.get(sessionId)
            .orElseThrow();

        synchronized (voiceSession) {
            assertThat(voiceSession.detachedAt()).isNotNull();
        }

        assertThat(racingHandler.scheduledTimerCount(sessionId)).isZero();
    }

    @Test
    void testSessionEndedWhileStartingStopsTheEngineAndKeepsNoTimers() throws Exception {
        WebhookWebSocketHandler silentHandler = createHandler(new HandlerTestSupport.FixedTriggerResolver(10));
        WebSocketSession webSocketSession = openSocket("tok-1", null);
        HandlerTestSupport.BlockingStart blockingStart = new HandlerTestSupport.BlockingStart(1L);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(blockingStart);

        silentHandler.afterConnectionEstablished(webSocketSession);

        String sessionId = blockingStart.awaitEntered();

        silentHandler.recordServerClose(webSocketSession.getId(), "session_limit");
        silentHandler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL.withReason("session_limit"));

        blockingStart.release();

        verify(voiceSessionEngine, timeout(2000)).stop(1L);

        assertThat(silentHandler.scheduledTimerCount(sessionId)).isZero();
        assertThat(silentHandler.hasLiveSession(sessionId)).isFalse();
    }

    private void awaitLiveSession(WebhookWebSocketHandler webhookWebSocketHandler, String sessionId)
        throws InterruptedException {

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);

        while (!webhookWebSocketHandler.hasLiveSession(sessionId)) {
            assertThat(System.nanoTime()).as("element start finished in time")
                .isLessThan(deadline);

            Thread.sleep(5);
        }

        VoiceSessionRegistry.Session voiceSession = voiceSessionRegistry.get(sessionId)
            .orElseThrow();

        synchronized (voiceSession) {
            assertThat(voiceSession.sessionId()).isEqualTo(sessionId);
        }
    }

    @Test
    void testServerEndBetweenEngineStartAndTimerSchedulingLeavesNoTimers() throws Exception {
        Thread testThread = Thread.currentThread();
        AtomicBoolean gateConsumed = new AtomicBoolean();
        CountDownLatch schedulingEntered = new CountDownLatch(1);
        CountDownLatch releaseScheduling = new CountDownLatch(1);
        AtomicReference<Thread> serverEndThread = new AtomicReference<>();
        AtomicInteger serverEndLookups = new AtomicInteger();

        VoiceSessionRegistry gatedRegistry = new VoiceSessionRegistry(() -> {
            if (Thread.currentThread() != testThread && gateConsumed.compareAndSet(false, true)) {
                schedulingEntered.countDown();

                awaitLatch(releaseScheduling);
            }

            return now.get();
        }) {

            @Override
            public Optional<VoiceSessionRegistry.Session> get(String sessionId) {
                if (Thread.currentThread() == serverEndThread.get()) {
                    serverEndLookups.incrementAndGet();
                }

                return super.get(sessionId);
            }
        };

        WebhookWebSocketHandler gatedHandler = createHandler(
            new HandlerTestSupport.FixedTriggerResolver(10), gatedRegistry);
        WebSocketSession webSocketSession = openSocket("tok-1", null);

        when(tokenService.consume("tok-1", WEBHOOK_ID)).thenReturn(true);
        when(voiceSessionEngine.start(any(), anyMap(), anyMap(), any(), any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new VoiceSession(1L, new WebSocketEmitter(), 24000, new SessionTranscript()));

        gatedHandler.afterConnectionEstablished(webSocketSession);

        assertThat(schedulingEntered.await(2, TimeUnit.SECONDS)).isTrue();

        ArgumentCaptor<Map<String, ?>> inputs = ArgumentCaptor.captor();

        verify(voiceSessionEngine).start(any(), anyMap(), inputs.capture(), any(), any(), any(), any(), anyBoolean(),
            any());

        String sessionId = String.valueOf(inputs.getValue()
            .get("sessionId"));

        gatedHandler.recordServerClose(webSocketSession.getId(), "provider_error");

        Thread serverEnd = new Thread(
            () -> gatedHandler.afterConnectionClosed(
                webSocketSession, CloseStatus.NORMAL.withReason("provider_error")));

        serverEndThread.set(serverEnd);
        serverEnd.start();

        awaitBlockedOnSessionOrTerminated(serverEnd, serverEndLookups);

        releaseScheduling.countDown();

        verify(webSocketSession, timeout(2000)).sendMessage(any(TextMessage.class));

        serverEnd.join(5000);

        assertThat(serverEnd.isAlive()).isFalse();

        verify(voiceSessionEngine).stop(1L);

        assertThat(gatedHandler.scheduledTimerCount(sessionId)).isZero();
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(interruptedException);
        }
    }

    private static void awaitJoining(Thread thread) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);

        while (thread.getState() != Thread.State.TIMED_WAITING && thread.getState() != Thread.State.WAITING) {
            assertThat(System.nanoTime())
                .as("stop() reached its wait for in-flight starts in time; state=%s", thread.getState())
                .isLessThan(deadline);

            Thread.sleep(5);
        }
    }

    private static void awaitBlockedOnSessionOrTerminated(Thread thread, AtomicInteger lookups)
        throws InterruptedException {

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);

        while (thread.getState() != Thread.State.TERMINATED &&
            !(lookups.get() >= 2 && thread.getState() == Thread.State.BLOCKED)) {

            assertThat(System.nanoTime())
                .as("server end finished or blocked on the session in time; state=%s", thread.getState())
                .isLessThan(deadline);

            Thread.sleep(5);
        }
    }

    private WebhookWebSocketHandler createHandler(TriggerResolver triggerResolver) {
        return createHandler(triggerResolver, voiceSessionRegistry);
    }

    private WebhookWebSocketHandler createHandler(TriggerResolver triggerResolver, VoiceMetricsRecorder recorder) {
        return new WebhookWebSocketHandler(
            tokenService, triggerResolver, recorder, voiceSessionConnectionResolver, voiceSessionEngine,
            voiceSessionRegistry, workflowContinuationHelper, 150L);
    }

    @SuppressWarnings("unchecked")
    private WebhookWebSocketHandler createHandler(
        TriggerResolver triggerResolver, VoiceSessionRegistry registry) {

        return new WebhookWebSocketHandler(
            tokenService, triggerResolver,
            new VoiceMetricsRecorder(mock(ObjectProvider.class)), voiceSessionConnectionResolver, voiceSessionEngine,
            registry, workflowContinuationHelper, 150L);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> awaitConnected(WebSocketSession webSocketSession) throws Exception {
        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);

        verify(webSocketSession, timeout(2000)).sendMessage(sent.capture());

        return (Map<String, Object>) JsonUtils.read(sent.getValue()
            .getPayload(), Map.class);
    }

    private static WebSocketSession openSocket(String token, String resumeSessionId) {
        return openSocket(WEBHOOK_ID, token, resumeSessionId);
    }

    private static WebSocketSession openSocket(String webhookId, String token, String resumeSessionId) {
        WebSocketSession webSocketSession = mock(WebSocketSession.class);
        String query = "sessionToken=" + token + (resumeSessionId == null ? "" : "&resumeSessionId=" + resumeSessionId);

        when(webSocketSession.getId()).thenReturn("ws-" + token);
        when(webSocketSession.getUri())
            .thenReturn(URI.create("ws://localhost/webhooks/" + webhookId + "/wss?" + query));
        when(webSocketSession.isOpen()).thenReturn(true);

        return webSocketSession;
    }
}
