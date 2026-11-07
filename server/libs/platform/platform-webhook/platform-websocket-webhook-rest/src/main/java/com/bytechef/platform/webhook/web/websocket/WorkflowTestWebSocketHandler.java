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
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.webhook.annotation.ConditionalOnWebhook;
import com.bytechef.platform.webhook.voice.VoiceSessionConnectionResolver;
import com.bytechef.platform.webhook.voice.VoiceSessionEngine;
import com.bytechef.platform.webhook.voice.VoiceSessionEngine.VoiceSession;
import com.bytechef.platform.webhook.voice.WebSocketEmitter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnWebhook
public class WorkflowTestWebSocketHandler extends AbstractWebSocketHandler {

    static final String NO_VOICE_AGENT_HINT = "Add a Voice Agent to the trigger to test with voice";

    private static final Logger log = LoggerFactory.getLogger(WorkflowTestWebSocketHandler.class);

    private static final ThreadLocal<Boolean> ON_TIMER_THREAD = ThreadLocal.withInitial(() -> false);

    private static final int DEFAULT_SILENCE_TIMEOUT_SECONDS = 120;
    private static final String SESSION_LIMIT_SECONDS = "sessionLimitSeconds";
    private static final long SILENCE_CHECK_INTERVAL_SECONDS = 5;
    private static final String SILENCE_TIMEOUT_SECONDS = "silenceTimeoutSeconds";
    private static final String VOICE_SESSION_TRIGGER_TYPE = "browser/v1/voiceSession";

    private final WorkflowTestVoiceSessionTokenService tokenService;
    private final VoiceSessionConnectionResolver voiceSessionConnectionResolver;
    private final VoiceSessionEngine voiceSessionEngine;
    private final VoiceSessionRegistry voiceSessionRegistry;
    private final WorkflowService workflowService;

    private final long maxSessionDurationSeconds;

    private final Cache<String, ScheduledFuture<?>> sessionLimitsBySessionId;
    private final Cache<String, ScheduledFuture<?>> silenceChecksBySessionId;
    private final Cache<String, String> sessionIdByWebSocketSessionId;
    private final ScheduledExecutorService silenceCheckScheduler;

    @Autowired
    @SuppressFBWarnings("EI")
    public WorkflowTestWebSocketHandler(
        VoiceSessionRegistry voiceSessionRegistry, VoiceSessionEngine voiceSessionEngine,
        WorkflowService workflowService, WorkflowTestVoiceSessionTokenService tokenService,
        VoiceSessionConnectionResolver voiceSessionConnectionResolver) {

        this(
            voiceSessionRegistry, voiceSessionEngine, workflowService, tokenService, voiceSessionConnectionResolver,
            30L * 60L);
    }

    @SuppressFBWarnings("EI")
    public WorkflowTestWebSocketHandler(
        VoiceSessionRegistry voiceSessionRegistry, VoiceSessionEngine voiceSessionEngine,
        WorkflowService workflowService, WorkflowTestVoiceSessionTokenService tokenService,
        VoiceSessionConnectionResolver voiceSessionConnectionResolver, long maxSessionDurationSeconds) {

        this.maxSessionDurationSeconds = maxSessionDurationSeconds;
        this.tokenService = tokenService;
        this.voiceSessionConnectionResolver = voiceSessionConnectionResolver;
        this.voiceSessionEngine = voiceSessionEngine;
        this.voiceSessionRegistry = voiceSessionRegistry;
        this.workflowService = workflowService;
        this.sessionIdByWebSocketSessionId = Caffeine.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();
        this.sessionLimitsBySessionId = Caffeine.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();
        this.silenceChecksBySessionId = Caffeine.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();
        this.silenceCheckScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                () -> {
                    ON_TIMER_THREAD.set(true);

                    runnable.run();
                },
                "voice-test-session-silence");

            thread.setDaemon(true);

            return thread;
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        String workflowId = extractWorkflowId(uri);
        String sessionToken = extractQueryParam(uri, "sessionToken");

        if (workflowId == null || sessionToken == null) {
            log.warn("Test WS upgrade missing workflowId or sessionToken: uri={}", uri);

            sendError(session, "Missing workflowId or sessionToken");
            session.close(CloseStatus.BAD_DATA);

            return;
        }

        OptionalLong tokenEnvironmentId = tokenService.consume(sessionToken, workflowId);

        if (tokenEnvironmentId.isEmpty()) {
            log.warn("Test WS upgrade rejected: invalid or expired token for workflowId={}", workflowId);

            sendError(session, "Invalid or expired session token");
            session.close(CloseStatus.POLICY_VIOLATION);

            return;
        }

        long environmentId = tokenEnvironmentId.getAsLong();

        String sessionId = UUID.randomUUID()
            .toString();

        VoiceSessionRegistry.Session voiceSession = voiceSessionRegistry.register(sessionId, session, null);

        sessionIdByWebSocketSessionId.put(session.getId(), sessionId);

        Thread.startVirtualThread(() -> startSession(session, voiceSession, workflowId, environmentId));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();

        if (VoiceControlFrame.isKeepalive(payload)) {
            String sessionId = sessionIdByWebSocketSessionId.getIfPresent(session.getId());

            if (sessionId != null) {
                voiceSessionRegistry.get(sessionId)
                    .ifPresent(VoiceSessionRegistry.Session::touchInboundAudio);
            }

            return;
        }

        forwardInbound(session, false, emitter -> emitter.dispatchMessage(payload));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        ByteBuffer payload = message.getPayload();
        byte[] bytes = new byte[payload.remaining()];

        payload.get(bytes);

        forwardInbound(session, true, emitter -> emitter.dispatchBinaryMessage(bytes));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Test WS closed: webSocketSessionId={}, status={}", session.getId(), status);

        String sessionId = sessionIdByWebSocketSessionId.getIfPresent(session.getId());

        if (sessionId == null) {
            return;
        }

        sessionIdByWebSocketSessionId.invalidate(session.getId());

        Optional<VoiceSessionRegistry.Session> voiceSessionOptional = voiceSessionRegistry.get(sessionId);

        if (voiceSessionOptional.isEmpty()) {
            return;
        }

        VoiceSessionRegistry.Session voiceSession = voiceSessionOptional.get();
        Long engineSessionId;

        synchronized (voiceSession) {
            voiceSession.markEnded();
            voiceSessionRegistry.remove(sessionId);

            engineSessionId = voiceSession.engineSessionId();
        }

        cancelSilenceCheck(sessionId);
        cancelSessionLimit(sessionId);

        if (engineSessionId == null) {
            return;
        }

        if (ON_TIMER_THREAD.get()) {
            Thread.startVirtualThread(() -> stopEngineSession(sessionId, engineSessionId));

            return;
        }

        stopEngineSession(sessionId, engineSessionId);
    }

    private void stopEngineSession(String sessionId, long engineSessionId) {
        try {
            voiceSessionEngine.stop(engineSessionId);
        } catch (Exception exception) {
            log.warn(
                "Failed to stop test voice session on WS close: sessionId={}, engineSessionId={}", sessionId,
                engineSessionId, exception);
        }
    }

    void closeIfSilent(VoiceSessionRegistry.Session voiceSession, int silenceTimeoutSeconds) {
        Duration silence = Duration.between(voiceSession.lastInboundAudioAt(), voiceSessionRegistry.now());

        if (silence.toSeconds() >= silenceTimeoutSeconds) {
            closeQuietly(
                voiceSession.webSocketSession(),
                CloseStatus.NORMAL.withReason(WebhookWebSocketHandler.SILENCE_TIMEOUT));
        }
    }

    boolean hasSilenceCheck(String sessionId) {
        ScheduledFuture<?> silenceCheck = silenceChecksBySessionId.getIfPresent(sessionId);

        return silenceCheck != null && !silenceCheck.isDone();
    }

    private void startSession(
        WebSocketSession session, VoiceSessionRegistry.Session voiceSession, String workflowId, long environmentId) {

        String sessionId = voiceSession.sessionId();
        WebSocketSession webSocketSession = Objects.requireNonNullElse(voiceSession.webSocketSession(), session);

        try {
            Workflow workflow = workflowService.getWorkflow(workflowId);

            Optional<WorkflowTrigger> workflowTriggerOptional = WorkflowTrigger.of(workflow)
                .stream()
                .filter(workflowTrigger -> VOICE_SESSION_TRIGGER_TYPE.equals(workflowTrigger.getType()))
                .findFirst();

            if (workflowTriggerOptional.isEmpty()) {
                log.warn("Test WS: no {} trigger in workflowId={}", VOICE_SESSION_TRIGGER_TYPE, workflowId);

                sendError(webSocketSession, "The workflow has no browser voice session trigger");
                closeQuietly(webSocketSession, CloseStatus.BAD_DATA);

                return;
            }

            WorkflowTrigger workflowTrigger = workflowTriggerOptional.get();

            Map<String, Object> inputs = new LinkedHashMap<>();

            inputs.put("sessionId", sessionId);
            inputs.put(
                "startedAt", voiceSession.startedAt()
                    .toString());
            inputs.put("testMode", true);

            VoiceSession engineSession = voiceSessionEngine.start(
                workflowTrigger.getExtensions(),
                voiceSessionConnectionResolver.resolveTest(workflowId, workflowTrigger.getName(), environmentId),
                inputs, null, null, environmentId, null, true, emitter -> attachOutboundBridge(voiceSession, emitter));

            int silenceTimeoutSeconds = MapUtils.getInteger(
                workflowTrigger.getParameters(), SILENCE_TIMEOUT_SECONDS, DEFAULT_SILENCE_TIMEOUT_SECONDS);
            boolean open;

            synchronized (voiceSession) {
                voiceSession.engineSessionId(engineSession.sessionId());

                open = voiceSessionRegistry.get(sessionId)
                    .isPresent();

                if (open) {
                    scheduleSilenceCheck(voiceSession, silenceTimeoutSeconds);
                    scheduleSessionLimit(voiceSession, sessionLimitSeconds(workflowTrigger));
                }
            }

            if (!open) {
                voiceSessionEngine.stop(engineSession.sessionId());

                return;
            }

            Map<String, Object> connected = new LinkedHashMap<>();

            connected.put("event", "connected");
            connected.put("workflowId", workflowId);
            connected.put("sessionId", sessionId);
            connected.put("outputSampleRate", engineSession.outputSampleRate());
            connected.put("silenceTimeoutSeconds", silenceTimeoutSeconds);

            sendJson(webSocketSession, connected);

            log.info(
                "Test voice session started: workflowId={}, sessionId={}, engineSessionId={}", workflowId, sessionId,
                engineSession.sessionId());
        } catch (VoiceSessionEngine.NoVoiceAgentException noVoiceAgentException) {
            sendError(webSocketSession, NO_VOICE_AGENT_HINT);
            closeQuietly(
                webSocketSession, CloseStatus.BAD_DATA.withReason(WebhookWebSocketHandler.VOICE_AGENT_MISSING));
        } catch (Exception exception) {
            failSession(webSocketSession, workflowId, exception);
        }
    }

    private void failSession(WebSocketSession session, String workflowId, Exception exception) {
        log.error("Failed to start test voice session: workflowId={}", workflowId, exception);

        sendError(session, "Failed to start the voice session: " + exception.getMessage());
        closeQuietly(session, CloseStatus.SERVER_ERROR);
    }

    private void scheduleSilenceCheck(VoiceSessionRegistry.Session voiceSession, int silenceTimeoutSeconds) {
        if (silenceTimeoutSeconds <= 0) {
            return;
        }

        String sessionId = voiceSession.sessionId();

        ScheduledFuture<?> silenceCheck = silenceCheckScheduler.scheduleWithFixedDelay(() -> {
            try {
                closeIfSilent(voiceSession, silenceTimeoutSeconds);
            } catch (Exception exception) {
                log.warn("Test silence check failed: sessionId={}", sessionId, exception);
            }
        }, SILENCE_CHECK_INTERVAL_SECONDS, SILENCE_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);

        silenceChecksBySessionId.put(sessionId, silenceCheck);
    }

    private void scheduleSessionLimit(VoiceSessionRegistry.Session voiceSession, long sessionLimitSeconds) {
        String sessionId = voiceSession.sessionId();

        ScheduledFuture<?> sessionLimit = silenceCheckScheduler.schedule(() -> {
            log.info("Closing test voice session at its session limit ({}s): sessionId={}", sessionLimitSeconds,
                sessionId);

            closeQuietly(
                voiceSession.webSocketSession(), CloseStatus.NORMAL.withReason(WebhookWebSocketHandler.SESSION_LIMIT));
        }, sessionLimitSeconds, TimeUnit.SECONDS);

        sessionLimitsBySessionId.put(sessionId, sessionLimit);
    }

    private void cancelSessionLimit(String sessionId) {
        ScheduledFuture<?> sessionLimit = sessionLimitsBySessionId.getIfPresent(sessionId);

        if (sessionLimit != null) {
            sessionLimit.cancel(false);
            sessionLimitsBySessionId.invalidate(sessionId);
        }
    }

    private long sessionLimitSeconds(WorkflowTrigger workflowTrigger) {
        int triggerLimitSeconds = MapUtils.getInteger(workflowTrigger.getParameters(), SESSION_LIMIT_SECONDS, 0);

        return triggerLimitSeconds > 0 ? triggerLimitSeconds : maxSessionDurationSeconds;
    }

    private void cancelSilenceCheck(String sessionId) {
        ScheduledFuture<?> silenceCheck = silenceChecksBySessionId.getIfPresent(sessionId);

        if (silenceCheck != null) {
            silenceCheck.cancel(false);
            silenceChecksBySessionId.invalidate(sessionId);
        }
    }

    private void forwardInbound(WebSocketSession session, boolean audio, Consumer<WebSocketEmitter> action) {
        String sessionId = sessionIdByWebSocketSessionId.getIfPresent(session.getId());

        if (sessionId == null) {
            return;
        }

        voiceSessionRegistry.get(sessionId)
            .ifPresent(voiceSession -> {
                if (audio) {
                    voiceSession.touchInboundAudio();
                }

                Long engineSessionId = voiceSession.engineSessionId();

                if (engineSessionId != null) {
                    voiceSessionEngine.emitter(engineSessionId)
                        .ifPresent(action);
                }
            });
    }

    private void attachOutboundBridge(VoiceSessionRegistry.Session voiceSession, WebSocketEmitter emitter) {
        AtomicBoolean providerFailed = new AtomicBoolean();

        emitter.addOutboundListener(payload -> {
            WebSocketSession webSocketSession = voiceSession.webSocketSession();

            if (webSocketSession == null || !webSocketSession.isOpen()) {
                return;
            }

            try {
                if (payload instanceof String stringPayload) {
                    webSocketSession.sendMessage(new TextMessage(stringPayload));
                } else {
                    webSocketSession.sendMessage(new TextMessage(JsonUtils.write(payload)));
                }
            } catch (IOException ioException) {
                log.warn(
                    "Failed to forward outbound text to test WS session: sessionId={}", voiceSession.sessionId(),
                    ioException);
            }
        });

        emitter.addOutboundBinaryListener(bytes -> {
            WebSocketSession webSocketSession = voiceSession.webSocketSession();

            if (webSocketSession == null || !webSocketSession.isOpen()) {
                return;
            }

            try {
                webSocketSession.sendMessage(new BinaryMessage(bytes));
            } catch (IOException ioException) {
                log.warn(
                    "Failed to forward outbound binary to test WS session: sessionId={}", voiceSession.sessionId(),
                    ioException);
            }
        });

        emitter.addErrorListener(throwable -> {
            providerFailed.set(true);

            log.warn("Voice Agent failed in a test session: sessionId={}", voiceSession.sessionId(), throwable);

            closeQuietly(
                voiceSession.webSocketSession(),
                CloseStatus.NORMAL.withReason(WebhookWebSocketHandler.PROVIDER_ERROR));
        });

        emitter.addCompletionListener(() -> {
            if (providerFailed.get() || voiceSession.ended()) {
                return;
            }

            log.info("Voice Agent closed a test session: sessionId={}", voiceSession.sessionId());

            closeQuietly(
                voiceSession.webSocketSession(),
                CloseStatus.NORMAL.withReason(WebhookWebSocketHandler.PROVIDER_CLOSED));
        });
    }

    private static @Nullable String extractWorkflowId(@Nullable URI uri) {
        if (uri == null) {
            return null;
        }

        String path = uri.getPath();

        int wssIndex = path.lastIndexOf("/wss");

        if (wssIndex < 0) {
            return null;
        }

        String head = path.substring(0, wssIndex);
        int slashIndex = head.lastIndexOf('/');

        if (slashIndex < 0 || slashIndex == head.length() - 1) {
            return null;
        }

        return head.substring(slashIndex + 1);
    }

    private static @Nullable String extractQueryParam(@Nullable URI uri, String name) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }

        String prefix = name + "=";

        for (String pair : uri.getQuery()
            .split("&")) {

            if (pair.startsWith(prefix)) {
                return pair.substring(prefix.length());
            }
        }

        return null;
    }

    private static void closeQuietly(@Nullable WebSocketSession session, CloseStatus closeStatus) {
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            session.close(closeStatus);
        } catch (IOException ioException) {
            log.warn("Failed to close test WS session: webSocketSessionId={}", session.getId(), ioException);
        }
    }

    private void sendJson(WebSocketSession session, Map<String, Object> data) {
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            session.sendMessage(new TextMessage(JsonUtils.write(data)));
        } catch (IOException ioException) {
            log.warn("Failed to send test WS message: webSocketSessionId={}", session.getId(), ioException);
        }
    }

    private void sendError(WebSocketSession session, String message) {
        Map<String, Object> error = new LinkedHashMap<>();

        error.put("type", "error");
        error.put("message", message);

        sendJson(session, error);
    }
}
