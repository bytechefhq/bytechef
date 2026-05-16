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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.voice.VoiceSessionEngine;
import com.bytechef.platform.webhook.voice.VoiceSessionEngine.VoiceSession;
import com.bytechef.platform.webhook.voice.WebSocketEmitter;
import com.bytechef.platform.webhook.voice.WebSocketEmitterRegistry;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * WebSocket handler for webhook connections, and the entry point for voice sessions. Establishes connections at
 * /webhooks/{id}.
 *
 * <p>
 * When a connection carries a callSid, the handler reads the trigger's {@code websocketTasks} pipeline and hands it to
 * {@link com.bytechef.platform.webhook.voice.VoiceSessionEngine}. No Atlas job runs while the caller is connected;
 * Atlas re-enters only once the session ends, through the continuation job {@link WorkflowContinuationHelper} creates
 * on close.
 * </p>
 *
 * @author Ivica Cardic
 */
@Component
public class WebhookWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WebhookWebSocketHandler.class);
    private static final int MAX_PENDING_EVENTS = 100;
    private static final String SESSION_LIMIT_SECONDS = "sessionLimitSeconds";

    private final BrowserVoiceSessionTokenService browserVoiceSessionTokenService;
    private final CallSessionRegistry callSessionRegistry;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final ObjectMapper objectMapper;
    private final VoiceMetricsRecorder voiceMetricsRecorder;
    private final WebhookWorkflowExecutor webhookWorkflowExecutor;
    private final VoiceSessionEngine voiceSessionEngine;
    private final WebSocketEmitterRegistry webSocketEmitterRegistry;
    private final WorkflowContinuationHelper workflowContinuationHelper;
    private final WorkflowService workflowService;

    private final long maxSessionDurationSeconds;

    @Value("${bytechef.twilio.stream-token.secret:}")
    private String twilioStreamTokenSecret;

    private final Cache<String, AutoCloseable> streamHandles;
    private final Cache<String, List<Map<String, Object>>> pendingEvents;
    // Twilio streams G.711 mu-law at 8 kHz; the reference linear-PCM voice pipeline consumes 16 kHz input and
    // produces 24 kHz output (deepgram/v1/voiceAgent defaults), so audio is transcoded between those rates.
    private static final int TWILIO_SAMPLE_RATE_HZ = 8000;
    private static final int TWILIO_INBOUND_PCM_RATE_HZ = 16000;
    private static final int TWILIO_OUTBOUND_PCM_RATE_HZ = 24000;

    private final Cache<String, String> sessionIdToCallSid;
    private final Cache<String, String> firstTaskNameByCallSid;
    private final Cache<String, String> streamSidBySessionId;
    private final Cache<String, Long> sessionOpenedAtBySessionId;
    private final ScheduledExecutorService sessionTimeoutScheduler;
    private final Cache<String, ScheduledFuture<?>> sessionTimeoutsBySessionId;

    @Autowired
    @SuppressFBWarnings("EI")
    public WebhookWebSocketHandler(
        BrowserVoiceSessionTokenService browserVoiceSessionTokenService, CallSessionRegistry callSessionRegistry,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, ObjectMapper objectMapper,
        VoiceMetricsRecorder voiceMetricsRecorder,
        WebhookWorkflowExecutor webhookWorkflowExecutor,
        VoiceSessionEngine voiceSessionEngine, WebSocketEmitterRegistry webSocketEmitterRegistry,
        WorkflowContinuationHelper workflowContinuationHelper, WorkflowService workflowService) {

        this(browserVoiceSessionTokenService, callSessionRegistry, jobPrincipalAccessorRegistry, objectMapper,
            voiceMetricsRecorder, webhookWorkflowExecutor, voiceSessionEngine, webSocketEmitterRegistry,
            workflowContinuationHelper, workflowService, 30L * 60L);
    }

    @SuppressFBWarnings("EI")
    public WebhookWebSocketHandler(
        BrowserVoiceSessionTokenService browserVoiceSessionTokenService, CallSessionRegistry callSessionRegistry,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, ObjectMapper objectMapper,
        VoiceMetricsRecorder voiceMetricsRecorder,
        WebhookWorkflowExecutor webhookWorkflowExecutor,
        VoiceSessionEngine voiceSessionEngine, WebSocketEmitterRegistry webSocketEmitterRegistry,
        WorkflowContinuationHelper workflowContinuationHelper, WorkflowService workflowService,
        long maxSessionDurationSeconds) {

        this.browserVoiceSessionTokenService = browserVoiceSessionTokenService;
        this.callSessionRegistry = callSessionRegistry;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.objectMapper = objectMapper;
        this.voiceMetricsRecorder = voiceMetricsRecorder;
        this.webhookWorkflowExecutor = webhookWorkflowExecutor;
        this.voiceSessionEngine = voiceSessionEngine;
        this.webSocketEmitterRegistry = webSocketEmitterRegistry;
        this.workflowContinuationHelper = workflowContinuationHelper;
        this.workflowService = workflowService;
        this.maxSessionDurationSeconds = maxSessionDurationSeconds;
        this.sessionTimeoutScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "voice-session-timeout");

            thread.setDaemon(true);

            return thread;
        });

        this.streamHandles = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .removalListener((key, value, cause) -> {
                if (value != null) {
                    closeHandle((AutoCloseable) value);
                }
            })
            .build();

        this.pendingEvents = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

        this.sessionIdToCallSid = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

        this.firstTaskNameByCallSid = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

        this.streamSidBySessionId = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

        this.sessionOpenedAtBySessionId = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

        this.sessionTimeoutsBySessionId = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        String webhookId = extractId(uri);
        String sessionKey = session.getId();
        String callSid = extractCallSid(uri);
        String sessionToken = extractQueryParam(uri, "sessionToken");
        // Present only for outbound Twilio calls: identifies the workflow (model B) whose trigger carries the
        // realtime websocketTasks pipeline to run for this call.
        String subWorkflowId = extractQueryParam(uri, "subWorkflowId");

        log.info(
            "WebSocket connection established for webhook: {}, sessionId: {}, callSid: {}, hasToken: {}",
            webhookId, sessionKey, callSid, sessionToken != null);

        // Browser-voice path: no callSid, but a single-use sessionToken minted via
        // POST /webhooks/{webhookId}/voice-session-token. Validate the token, then synthesise a callSid
        // so the rest of the handler (registry, sub-workflow spawn, continuation-on-close) works unchanged.
        if (callSid == null && sessionToken != null) {
            if (webhookId == null || !browserVoiceSessionTokenService.consume(sessionToken, webhookId)) {
                log.warn("Browser-voice WS upgrade rejected: invalid token for webhook={}", webhookId);

                Map<String, Object> error = new LinkedHashMap<>();

                error.put("type", "error");
                error.put("message", "Invalid or expired session token");

                sendMessage(session, error);
                session.close(CloseStatus.POLICY_VIOLATION);

                return;
            }

            callSid = "browser-" + UUID.randomUUID();
        }

        // Twilio media-stream path: a WebSocket upgrade cannot carry X-Twilio-Signature, so when a stream-token secret
        // is configured the wss URL must carry a valid signed streamToken bound to this callSid (minted into the TwiML
        // <Stream> at build time). Browser-voice connections are already gated by their single-use sessionToken above.
        if (callSid != null && twilioStreamTokenSecret != null && !twilioStreamTokenSecret.isBlank()
            && !callSid.startsWith("browser-")) {

            String streamToken = extractQueryParam(uri, "streamToken");

            if (!TwilioStreamToken.verify(
                twilioStreamTokenSecret, callSid, streamToken, Instant.now()
                    .getEpochSecond())) {

                log.warn("Twilio media-stream WS upgrade rejected: invalid streamToken for callSid={}", callSid);

                session.close(CloseStatus.POLICY_VIOLATION);

                return;
            }
        }

        if (callSid != null) {

            if (callSessionRegistry.hasSession(callSid)) {
                log.info("Reusing existing session for callSid: {}", callSid);

                Optional<CallSessionRegistry.CallSession> existingSessionOpt = callSessionRegistry.getSessionByCallSid(
                    callSid);

                if (existingSessionOpt.isPresent()) {
                    CallSessionRegistry.CallSession existingCallSession = existingSessionOpt.get();
                    Optional<WebSocketSession> existingWebSocketSessionOpt =
                        callSessionRegistry.getWebSocketSessionById(
                            existingCallSession.getWebSocketSessionId());

                    if (existingWebSocketSessionOpt.isPresent() && existingWebSocketSessionOpt.get()
                        .isOpen()) {

                        WebSocketSession existingWebSocketSession = existingWebSocketSessionOpt.get();

                        log.info(
                            "Existing session is still open, reusing: callSid={}, existingSessionId={}", callSid,
                            existingWebSocketSession.getId());

                        Map<String, Object> connected = new LinkedHashMap<>();

                        connected.put("event", "connected");
                        connected.put("id", webhookId);
                        connected.put("callSid", callSid);
                        connected.put("reused", true);

                        sendMessage(session, connected);

                        session.close(CloseStatus.NORMAL.withReason("Session already exists for this callSid"));

                        return;
                    } else {
                        log.info("Existing session is closed, creating new session: callSid={}", callSid);

                        callSessionRegistry.removeSessionByCallSid(callSid);
                    }
                }
            }

            CallSessionRegistry.CallMetadata metadata = new CallSessionRegistry.CallMetadata(
                null, null, null, null);

            callSessionRegistry.registerSession(callSid, session, metadata);
            sessionIdToCallSid.put(sessionKey, callSid);
            sessionOpenedAtBySessionId.put(sessionKey, System.nanoTime());
            voiceMetricsRecorder.recordSessionOpened();

            scheduleMaxDurationClose(session, sessionKey);

            if (webhookId != null) {
                startWebsocketSubflow(callSid, webhookId, subWorkflowId);
            }
        }

        Map<String, Object> connected = new LinkedHashMap<>();
        connected.put("event", "connected");
        connected.put("id", webhookId);

        if (callSid != null) {
            connected.put("callSid", callSid);
        }

        sendMessage(session, connected);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        // Browser-voice path sends audio as raw binary frames (PCM16). Twilio sends audio as JSON text frames instead
        // (handled in handleTextMessage). Either way the inbound audio is forwarded to the FIRST task's emitter.
        ByteBuffer payload = message.getPayload();
        byte[] bytes = new byte[payload.remaining()];

        payload.get(bytes);

        forwardInboundAudioToFirstTask(session, bytes);
    }

    /**
     * Forwards an inbound audio frame to the first task's emitter in the sub-workflow's linear chain, so the leading
     * realtime component (e.g. {@code deepgram/v1/voiceAgent} or an STT) receives the caller/microphone audio.
     */
    private void forwardInboundAudioToFirstTask(WebSocketSession session, byte[] bytes) {
        String callSid = sessionIdToCallSid.getIfPresent(session.getId());

        if (callSid == null) {
            return;
        }

        Optional<CallSessionRegistry.CallSession> callSessionOpt = callSessionRegistry.getSessionByCallSid(callSid);

        if (callSessionOpt.isEmpty()) {
            return;
        }

        Long voiceSessionId = callSessionOpt.get()
            .getVoiceSessionId();
        String firstTaskName = firstTaskNameByCallSid.getIfPresent(callSid);

        if (voiceSessionId == null || firstTaskName == null) {
            return;
        }

        Optional<WebSocketEmitter> emitterOpt = webSocketEmitterRegistry.get(voiceSessionId, firstTaskName);

        if (emitterOpt.isEmpty()) {
            return;
        }

        emitterOpt.get()
            .dispatchBinaryMessage(bytes);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionKey = session.getId();
        String payload = message.getPayload();

        if (log.isDebugEnabled()) {
            log.debug("Webhook WS inbound: sessionId={}, payload={}", sessionKey, payload);
        }

        try {
            Map<String, Object> request = objectMapper.readValue(payload, new TypeReference<>() {});

            String twilioEvent = TwilioMediaStream.eventType(request);

            if (twilioEvent != null) {
                handleTwilioMediaStreamFrame(session, twilioEvent, request);

                return;
            }

            String action = (String) request.get("action");

            if ("execute".equals(action)) {
                executeWorkflow(session, request);
            } else {
                Map<String, Object> ack = new LinkedHashMap<>();
                ack.put("event", "ack");
                ack.put("data", payload);

                sendMessage(session, ack);
            }
        } catch (Exception exception) {
            log.error("Error processing WebSocket message", exception);

            Map<String, Object> error = new LinkedHashMap<>();
            error.put("event", "error");
            error.put("message", "Error processing message: " + exception.getMessage());

            sendMessage(session, error);
        }
    }

    /**
     * Handles a Twilio Media Streams control/audio frame. On {@code start} the streamSid is captured (needed to frame
     * outbound audio); on {@code media} the base64 audio payload is decoded and forwarded to the sub-workflow; other
     * frames ({@code connected}/{@code stop}) carry no audio to forward.
     */
    private void handleTwilioMediaStreamFrame(WebSocketSession session, String eventType, Map<String, Object> frame) {
        switch (eventType) {
            case TwilioMediaStream.EVENT_START -> {
                String streamSid = TwilioMediaStream.extractStreamSid(frame);

                if (streamSid != null) {
                    streamSidBySessionId.put(session.getId(), streamSid);
                }
            }
            case TwilioMediaStream.EVENT_MEDIA -> {
                byte[] muLawAudio = TwilioMediaStream.decodeMediaPayload(frame);

                if (muLawAudio != null) {
                    // Transcode Twilio mu-law/8 kHz to the linear PCM/16 kHz the pipeline's leading component expects.
                    byte[] pcmAudio = TwilioAudioCodec.resamplePcm16(
                        TwilioAudioCodec.muLawToPcm16(muLawAudio), TWILIO_SAMPLE_RATE_HZ, TWILIO_INBOUND_PCM_RATE_HZ);

                    forwardInboundAudioToFirstTask(session, pcmAudio);
                }
            }
            default -> log.debug(
                "Twilio media-stream {} frame received: sessionId={}", eventType, session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionKey = session.getId();

        log.info("WebSocket connection closed: sessionId={}, status={}", sessionKey, status);

        String callSid = sessionIdToCallSid.getIfPresent(sessionKey);

        if (callSid != null) {
            Optional<CallSessionRegistry.CallSession> callSessionOpt =
                callSessionRegistry.getSessionByCallSid(callSid);

            if (callSessionOpt.isPresent()) {
                CallSessionRegistry.CallSession callSession = callSessionOpt.get();

                if (callSession.isSignalCompletionOnClose()) {
                    log.info(
                        "Signaling workflow completion on WebSocket close: callSid={}", callSid);

                    Long voiceSessionId = callSession.getVoiceSessionId();

                    if (voiceSessionId != null) {
                        try {
                            voiceSessionEngine.stop(voiceSessionId);
                        } catch (Exception exception) {
                            log.warn(
                                "Failed to stop voice session on WebSocket close: callSid={}, voiceSessionId={}",
                                callSid, voiceSessionId, exception);
                        }
                    }

                    callSession.signalCompletion();

                    String workflowExecutionId = callSession.getWorkflowExecutionId();

                    if (workflowExecutionId != null) {
                        Map<String, Object> afterCallData = new LinkedHashMap<>();

                        afterCallData.put("callSid", callSid);
                        afterCallData.put("callStatus", "websocket_closed");
                        afterCallData.put("closeStatusCode", status.getCode());
                        afterCallData.put("closeReason", status.getReason());

                        if (callSession.getCallDuration() != null) {
                            afterCallData.put("callDuration", callSession.getCallDuration());
                        }

                        workflowContinuationHelper.createContinuationJob(
                            workflowExecutionId, afterCallData);
                    }
                }
            }

            callSessionRegistry.removeSessionByCallSid(callSid);
            sessionIdToCallSid.invalidate(sessionKey);
            streamSidBySessionId.invalidate(sessionKey);
        }

        AutoCloseable handle = streamHandles.getIfPresent(sessionKey);

        if (handle != null) {
            closeHandle(handle);
            streamHandles.invalidate(sessionKey);
        }

        pendingEvents.invalidate(sessionKey);

        // Cancel the max-duration scheduled close (this close handler may run before the timer fires, e.g.
        // when the user clicks End or the WS drops naturally).
        ScheduledFuture<?> pendingTimeout = sessionTimeoutsBySessionId.getIfPresent(sessionKey);

        if (pendingTimeout != null) {
            pendingTimeout.cancel(false);
            sessionTimeoutsBySessionId.invalidate(sessionKey);
        }

        Long openedAt = sessionOpenedAtBySessionId.getIfPresent(sessionKey);

        if (openedAt != null) {
            long durationNanos = System.nanoTime() - openedAt;

            voiceMetricsRecorder.recordSessionClosed(java.time.Duration.ofNanos(durationNanos));
            sessionOpenedAtBySessionId.invalidate(sessionKey);
        } else if (status != null && !CloseStatus.NORMAL.equalsCode(status)) {
            voiceMetricsRecorder.recordSessionError();
        }
    }

    private void scheduleMaxDurationClose(WebSocketSession session, String sessionKey) {
        scheduleMaxDurationClose(session, sessionKey, maxSessionDurationSeconds);
    }

    private void scheduleMaxDurationClose(WebSocketSession session, String sessionKey, long durationSeconds) {
        if (durationSeconds <= 0) {
            return;
        }

        ScheduledFuture<?> future = sessionTimeoutScheduler.schedule(() -> {
            if (!session.isOpen()) {
                return;
            }

            log.info(
                "Closing WebSocket due to max session duration ({}s) reached: sessionId={}",
                durationSeconds, sessionKey);

            try {
                session.close(CloseStatus.NORMAL.withReason("Maximum session duration reached"));
            } catch (IOException ioException) {
                log.warn(
                    "Failed to close WS at max duration: sessionId={}", sessionKey, ioException);
            }
        }, durationSeconds, TimeUnit.SECONDS);

        sessionTimeoutsBySessionId.put(sessionKey, future);
    }

    private void executeWorkflow(WebSocketSession session, Map<String, Object> request) throws Exception {
        String sessionKey = session.getId();
        URI uri = session.getUri();
        String webhookId = extractId(uri);

        Map<String, Object> start = new LinkedHashMap<>();
        start.put("event", "start");
        start.put("message", "Workflow execution started");
        start.put("webhookId", webhookId);

        sendMessage(session, start);

        try {
            WebhookRequest webhookRequest = buildWebhookRequest(request);

            WorkflowExecutionId workflowExecutionId = (WorkflowExecutionId) request.get("workflowExecutionId");

            if (workflowExecutionId == null) {
                throw new IllegalArgumentException("workflowExecutionId is required");
            }

            WebSocketStreamBridge streamBridge = new WebSocketStreamBridge(sessionKey);

            webhookWorkflowExecutor.executeAsync(workflowExecutionId, webhookRequest, streamBridge);
        } catch (Exception exception) {
            log.error("Error executing workflow", exception);

            Map<String, Object> error = new LinkedHashMap<>();
            error.put("event", "error");
            error.put("message", exception.getMessage());

            sendMessage(session, error);
        }
    }

    /**
     * Reads the websocket subflow definition from the workflow trigger's extensions and executes it. The subflow
     * definition is stored as a string in the trigger's {@code websocketTasks} extension field within the workflow
     * definition JSON.
     */
    private void startWebsocketSubflow(String callSid, String webhookIdString, @Nullable String subWorkflowId) {
        Optional<CallSessionRegistry.CallSession> callSessionOpt = callSessionRegistry.getSessionByCallSid(callSid);

        if (callSessionOpt.isEmpty()) {
            log.warn("Cannot start websocket subflow: no session found for callSid={}", callSid);

            return;
        }

        CallSessionRegistry.CallSession callSession = callSessionOpt.get();

        callSession.setWorkflowExecutionId(webhookIdString);

        Thread.startVirtualThread(() -> {
            try {
                String websocketSubflowDefinition;
                WorkflowExecutionId workflowExecutionId = null;

                if (subWorkflowId != null && !subWorkflowId.isBlank()) {
                    // Outbound call (model B): resolve the pipeline from the trigger of the workflow chosen in the
                    // makeCall action, not from an inbound trigger.
                    websocketSubflowDefinition = getWebsocketSubflowDefinitionByWorkflowId(subWorkflowId);
                } else {
                    workflowExecutionId = WorkflowExecutionId.parse(webhookIdString);

                    websocketSubflowDefinition = getWebsocketSubflowDefinition(workflowExecutionId);
                }

                if (websocketSubflowDefinition == null || websocketSubflowDefinition.isBlank()) {
                    log.warn(
                        "No websocket subflow definition found in trigger for callSid={}", callSid);

                    return;
                }

                if (workflowExecutionId != null) {
                    applyTriggerSessionLimit(callSid, workflowExecutionId);
                }

                Optional<WebSocketSession> webSocketSessionOpt = callSessionRegistry.getWebSocketSession(callSid);

                if (webSocketSessionOpt.isEmpty()) {
                    log.warn("Cannot start voice session: no open WS session for callSid={}", callSid);

                    return;
                }

                WebSocketSession wsSession = webSocketSessionOpt.get();

                Map<String, Object> inputs = new LinkedHashMap<>();

                inputs.put("callSid", callSid);
                inputs.put("mainWorkflowExecutionId", webhookIdString);

                // Stage order in the definition IS the wiring: inbound audio goes to stage[0], each stage's outbound
                // feeds the next, and the last stage's outbound reaches the caller. No Atlas job runs during the
                // call — see VoiceSessionEngine.
                VoiceSession voiceSession = voiceSessionEngine.start(
                    websocketSubflowDefinition, inputs, null,
                    workflowExecutionId == null ? null : workflowExecutionId.getType(),
                    emitter -> attachOutboundBridge(wsSession, emitter));

                callSession.setVoiceSessionId(voiceSession.sessionId());

                List<String> stageNames = voiceSession.stageNames();

                firstTaskNameByCallSid.put(callSid, stageNames.isEmpty() ? "" : stageNames.getFirst());

                log.info(
                    "Voice session started: callSid={}, sessionId={}, stages={}", callSid, voiceSession.sessionId(),
                    stageNames);
            } catch (Exception exception) {
                log.error("Failed to start websocket subflow: callSid={}", callSid, exception);
            }
        });
    }

    /**
     * Hooks the emitter's outbound channels to the live WS session. Outbound text/JSON from the workflow becomes
     * {@link TextMessage}s, outbound binary (TTS audio) becomes {@link BinaryMessage}s.
     */
    private void attachOutboundBridge(WebSocketSession wsSession, WebSocketEmitter emitter) {
        emitter.addOutboundListener(payload -> {
            if (!wsSession.isOpen()) {
                return;
            }

            try {
                if (payload instanceof String stringPayload) {
                    wsSession.sendMessage(new TextMessage(stringPayload));
                } else {
                    wsSession.sendMessage(new TextMessage(JsonUtils.write(payload)));
                }
            } catch (IOException ioException) {
                log.warn(
                    "Failed to forward outbound text to WS session: sessionId={}", wsSession.getId(), ioException);
            }
        });

        emitter.addOutboundBinaryListener(bytes -> {
            if (!wsSession.isOpen()) {
                return;
            }

            try {
                String streamSid = streamSidBySessionId.getIfPresent(wsSession.getId());

                if (streamSid != null) {
                    // Twilio call: transcode the pipeline's linear PCM/24 kHz output back to mu-law/8 kHz and send it
                    // as
                    // a base64 media text frame, not a raw binary frame.
                    byte[] muLawAudio = TwilioAudioCodec.pcm16ToMuLaw(
                        TwilioAudioCodec.resamplePcm16(bytes, TWILIO_OUTBOUND_PCM_RATE_HZ, TWILIO_SAMPLE_RATE_HZ));

                    wsSession.sendMessage(
                        new TextMessage(JsonUtils.write(TwilioMediaStream.mediaFrame(streamSid, muLawAudio))));
                } else {
                    wsSession.sendMessage(new BinaryMessage(bytes));
                }
            } catch (IOException ioException) {
                log.warn(
                    "Failed to forward outbound binary to WS session: sessionId={}", wsSession.getId(), ioException);
            }
        });

        // Barge-in: when the current turn is cancelled, tell Twilio to flush any audio it has buffered but not played.
        emitter.addOutboundTurnCancelListener(turnId -> {
            String streamSid = streamSidBySessionId.getIfPresent(wsSession.getId());

            if (streamSid == null || !wsSession.isOpen()) {
                return;
            }

            try {
                wsSession.sendMessage(new TextMessage(JsonUtils.write(TwilioMediaStream.clearFrame(streamSid))));
            } catch (IOException ioException) {
                log.warn(
                    "Failed to forward Twilio clear frame to WS session: sessionId={}", wsSession.getId(),
                    ioException);
            }
        });
    }

    /**
     * If the trigger defines a {@code sessionLimitSeconds} parameter (value &gt; 0), cancel the default max-duration
     * timeout that was scheduled at connection time and reschedule with the trigger's value. When the trigger value is
     * 0 the default timeout (set at construction) continues to apply.
     */
    private void applyTriggerSessionLimit(String callSid, WorkflowExecutionId workflowExecutionId) {
        int triggerLimitSeconds = getSessionLimitSeconds(workflowExecutionId);

        if (triggerLimitSeconds <= 0) {
            return;
        }

        Optional<WebSocketSession> wsSessionOpt = callSessionRegistry.getWebSocketSession(callSid);

        if (wsSessionOpt.isEmpty()) {
            return;
        }

        WebSocketSession wsSession = wsSessionOpt.get();
        String sessionKey = wsSession.getId();

        ScheduledFuture<?> existingTimeout = sessionTimeoutsBySessionId.getIfPresent(sessionKey);

        if (existingTimeout != null) {
            existingTimeout.cancel(false);
            sessionTimeoutsBySessionId.invalidate(sessionKey);
        }

        scheduleMaxDurationClose(wsSession, sessionKey, triggerLimitSeconds);
    }

    /**
     * Reads the {@code sessionLimitSeconds} parameter from the trigger. Returns 0 when not set or set to 0 (meaning no
     * limit).
     */
    private int getSessionLimitSeconds(WorkflowExecutionId workflowExecutionId) {
        try {
            WorkflowTrigger workflowTrigger = resolveWorkflowTrigger(workflowExecutionId);

            return MapUtils.getInteger(workflowTrigger.getParameters(), SESSION_LIMIT_SECONDS, 0);
        } catch (Exception exception) {
            log.warn(
                "Failed to read sessionLimitSeconds from trigger for workflowExecutionId={}", workflowExecutionId,
                exception);

            return 0;
        }
    }

    /**
     * Resolves the websocket subflow definition string from the workflow trigger's extensions.
     */
    private String getWebsocketSubflowDefinition(WorkflowExecutionId workflowExecutionId) {
        WorkflowTrigger workflowTrigger = resolveWorkflowTrigger(workflowExecutionId);

        return WebsocketTasks.resolve(workflowTrigger);
    }

    /**
     * Resolves the {@code websocketTasks} realtime pipeline from the triggers of the workflow selected in an outbound
     * call action (model B), returning the first trigger that defines one.
     */
    private @Nullable String getWebsocketSubflowDefinitionByWorkflowId(String subWorkflowId) {
        Workflow workflow = workflowService.getWorkflow(subWorkflowId);

        return WorkflowTrigger.of(workflow)
            .stream()
            .map(WebsocketTasks::resolve)
            .filter(definition -> definition != null && !definition.isBlank())
            .findFirst()
            .orElse(null);
    }

    private WorkflowTrigger resolveWorkflowTrigger(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor =
            jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType());

        String workflowId = jobPrincipalAccessor.getWorkflowId(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());

        Workflow workflow = workflowService.getWorkflow(workflowId);

        return WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);
    }

    private WebhookRequest buildWebhookRequest(Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, List<String>> headers = (Map<String, List<String>>) request.getOrDefault(
            "headers", new LinkedHashMap<>());

        @SuppressWarnings("unchecked")
        Map<String, List<String>> parameters = (Map<String, List<String>>) request.getOrDefault(
            "parameters", new LinkedHashMap<>());

        Object bodyContent = request.getOrDefault("body", new LinkedHashMap<>());

        WebhookRequest.WebhookBodyImpl body = new WebhookRequest.WebhookBodyImpl(
            bodyContent,
            WebhookRequest.WebhookBodyImpl.ContentType.JSON,
            "application/json",
            bodyContent.toString());

        String methodName = (String) request.getOrDefault("method", "POST");

        WebhookMethod method;

        try {
            method = WebhookMethod.valueOf(methodName.toUpperCase());
        } catch (IllegalArgumentException illegalArgumentException) {
            method = WebhookMethod.POST;
        }

        return new WebhookRequest(headers, parameters, body, method);
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> data) {
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            String json = JsonUtils.write(data);

            session.sendMessage(new TextMessage(json));

            if (log.isDebugEnabled()) {
                log.debug("Sent WebSocket message: sessionId={}, event={}", session.getId(), data.get("event"));
            }
        } catch (IOException ioException) {
            log.error("Failed to send WebSocket message to session: {}", session.getId(), ioException);
        }
    }

    private static String extractId(URI uri) {
        if (uri == null) {
            return null;
        }

        String path = uri.getPath();

        if (path == null) {
            return null;
        }

        String[] segments = path.split("/");

        for (int i = segments.length - 1; i >= 0; i--) {
            String segment = segments[i];

            if (!segment.isEmpty() && !"webhooks".equals(segment)) {
                return segment;
            }
        }

        return null;
    }

    private static String extractCallSid(URI uri) {
        return extractQueryParam(uri, "callSid");
    }

    private static String extractQueryParam(URI uri, String paramName) {
        if (uri == null || paramName == null) {
            return null;
        }

        String query = uri.getQuery();

        if (query == null || query.isEmpty()) {
            return null;
        }

        String[] params = query.split("&");

        for (String param : params) {
            String[] keyValue = param.split("=");

            if (keyValue.length == 2 && paramName.equals(keyValue[0])) {
                return keyValue[1];
            }
        }

        return null;
    }

    private static void closeHandle(AutoCloseable handle) {
        try {
            handle.close();
        } catch (Exception exception) {
            if (log.isTraceEnabled()) {
                log.trace("Failed to close handle", exception);
            }
        }
    }

    private class WebSocketStreamBridge implements SseStreamBridge {

        private final String sessionKey;

        public WebSocketStreamBridge(String sessionKey) {
            this.sessionKey = sessionKey;
        }

        @Override
        public void onEvent(Object payload) {
            String callSid = sessionIdToCallSid.getIfPresent(sessionKey);

            if (callSid != null) {
                Optional<WebSocketSession> sessionOpt = callSessionRegistry.getWebSocketSession(callSid);

                if (sessionOpt.isPresent() && sessionOpt.get()
                    .isOpen()) {

                    WebSocketSession session = sessionOpt.get();

                    Map<String, Object> event = new LinkedHashMap<>();
                    event.put("event", "stream");
                    event.put("data", payload);

                    sendMessage(session, event);
                } else {
                    bufferEvent(payload);
                }
            } else {
                bufferEvent(payload);
            }
        }

        @Override
        public void onComplete() {
            String callSid = sessionIdToCallSid.getIfPresent(sessionKey);

            if (callSid != null) {
                Optional<WebSocketSession> sessionOpt = callSessionRegistry.getWebSocketSession(callSid);

                if (sessionOpt.isPresent() && sessionOpt.get()
                    .isOpen()) {

                    WebSocketSession session = sessionOpt.get();

                    Map<String, Object> complete = new LinkedHashMap<>();
                    complete.put("event", "complete");
                    complete.put("message", "Workflow execution completed");

                    sendMessage(session, complete);

                    try {
                        session.close(CloseStatus.NORMAL);
                    } catch (IOException ioException) {
                        log.error("Error closing WebSocket session: {}", sessionKey, ioException);
                    }
                }

                callSessionRegistry.removeSessionByCallSid(callSid);
                sessionIdToCallSid.invalidate(sessionKey);
            }

            AutoCloseable handle = streamHandles.getIfPresent(sessionKey);

            if (handle != null) {
                closeHandle(handle);
                streamHandles.invalidate(sessionKey);
            }

            pendingEvents.invalidate(sessionKey);
        }

        @Override
        public void onError(Throwable throwable) {
            String callSid = sessionIdToCallSid.getIfPresent(sessionKey);

            WebSocketSession session = null;

            if (callSid != null) {
                Optional<WebSocketSession> sessionOpt = callSessionRegistry.getWebSocketSession(callSid);

                session = sessionOpt.orElse(null);
            }

            if (session != null && session.isOpen()) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("event", "error");

                if (throwable instanceof CancellationException) {
                    error.put("message", "Workflow execution aborted");
                } else {
                    error.put("message", throwable.getMessage());
                }

                sendMessage(session, error);

                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException ioException) {
                    log.error("Error closing WebSocket session: {}", sessionKey, ioException);
                }
            }

            if (callSid != null) {
                callSessionRegistry.removeSessionByCallSid(callSid);
                sessionIdToCallSid.invalidate(sessionKey);
            }

            AutoCloseable handle = streamHandles.getIfPresent(sessionKey);

            if (handle != null) {
                closeHandle(handle);
                streamHandles.invalidate(sessionKey);
            }

            pendingEvents.invalidate(sessionKey);
        }

        private void bufferEvent(Object payload) {
            List<Map<String, Object>> events = pendingEvents.get(sessionKey, key -> new ArrayList<>());

            if (events.size() < MAX_PENDING_EVENTS) {
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("event", "stream");
                event.put("data", payload);
                events.add(event);
            }
        }
    }
}
