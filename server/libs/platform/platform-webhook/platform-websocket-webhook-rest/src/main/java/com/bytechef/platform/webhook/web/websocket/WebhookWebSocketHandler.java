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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.webhook.annotation.ConditionalOnWebhook;
import com.bytechef.platform.webhook.voice.VoiceSessionConnectionResolver;
import com.bytechef.platform.webhook.voice.VoiceSessionEngine;
import com.bytechef.platform.webhook.voice.VoiceSessionEngine.VoiceSession;
import com.bytechef.platform.webhook.voice.WebSocketEmitter;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.WebSocketSessionDecorator;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnWebhook
public class WebhookWebSocketHandler extends AbstractWebSocketHandler implements SmartLifecycle {

    static final String CLIENT_CLOSED = "client_closed";
    static final String PROVIDER_CLOSED = "provider_closed";
    static final String PROVIDER_ERROR = "provider_error";
    static final String SERVER_SHUTDOWN = "server_shutdown";
    static final String SESSION_LIMIT = "session_limit";
    static final String SILENCE_TIMEOUT = "silence_timeout";

    static final String VOICE_AGENT_MISSING = "voice_agent_missing";

    static final String WORKFLOW_DISABLED = "workflow_disabled";

    private static final Logger log = LoggerFactory.getLogger(WebhookWebSocketHandler.class);

    private static final ThreadLocal<Boolean> ON_TIMER_THREAD = ThreadLocal.withInitial(() -> false);

    private static final String CLIENT_END_REASON = CLIENT_CLOSED;

    private static final int DEFAULT_SILENCE_TIMEOUT_SECONDS = 120;

    private static final Duration SHUTDOWN_WAIT = Duration.ofSeconds(30);

    private static final String SESSION_LIMIT_SECONDS = "sessionLimitSeconds";
    private static final long SILENCE_CHECK_INTERVAL_SECONDS = 5;
    private static final String SILENCE_TIMEOUT_SECONDS = "silenceTimeoutSeconds";

    private final BrowserVoiceSessionTokenService browserVoiceSessionTokenService;
    private final TriggerResolver triggerResolver;
    private final VoiceMetricsRecorder voiceMetricsRecorder;
    private final VoiceSessionConnectionResolver voiceSessionConnectionResolver;
    private final VoiceSessionEngine voiceSessionEngine;
    private final VoiceSessionRegistry voiceSessionRegistry;
    private final WorkflowContinuationHelper workflowContinuationHelper;

    private final long maxSessionDurationSeconds;

    private final Cache<String, LiveSession> liveSessionsBySessionId;

    private final Map<String, String> serverCloseReasonsByWebSocketSessionId = new ConcurrentHashMap<>();
    private final Cache<String, String> sessionIdByWebSocketSessionId;
    private final ScheduledExecutorService sessionTimeoutScheduler;
    private final Cache<String, List<ScheduledFuture<?>>> sessionTimersBySessionId;

    private final Set<Thread> handOffThreads = ConcurrentHashMap.newKeySet();

    private volatile boolean running;

    private volatile boolean shuttingDown;

    @Autowired
    @SuppressFBWarnings("EI")
    public WebhookWebSocketHandler(
        BrowserVoiceSessionTokenService browserVoiceSessionTokenService, TriggerResolver triggerResolver,
        VoiceMetricsRecorder voiceMetricsRecorder, VoiceSessionConnectionResolver voiceSessionConnectionResolver,
        VoiceSessionEngine voiceSessionEngine, VoiceSessionRegistry voiceSessionRegistry,
        WorkflowContinuationHelper workflowContinuationHelper) {

        this(
            browserVoiceSessionTokenService, triggerResolver, voiceMetricsRecorder, voiceSessionConnectionResolver,
            voiceSessionEngine, voiceSessionRegistry, workflowContinuationHelper, 30L * 60L);
    }

    @SuppressFBWarnings("EI")
    public WebhookWebSocketHandler(
        BrowserVoiceSessionTokenService browserVoiceSessionTokenService, TriggerResolver triggerResolver,
        VoiceMetricsRecorder voiceMetricsRecorder, VoiceSessionConnectionResolver voiceSessionConnectionResolver,
        VoiceSessionEngine voiceSessionEngine, VoiceSessionRegistry voiceSessionRegistry,
        WorkflowContinuationHelper workflowContinuationHelper, long maxSessionDurationSeconds) {

        this.browserVoiceSessionTokenService = browserVoiceSessionTokenService;
        this.triggerResolver = triggerResolver;
        this.voiceMetricsRecorder = voiceMetricsRecorder;
        this.voiceSessionConnectionResolver = voiceSessionConnectionResolver;
        this.voiceSessionEngine = voiceSessionEngine;
        this.voiceSessionRegistry = voiceSessionRegistry;
        this.workflowContinuationHelper = workflowContinuationHelper;
        this.maxSessionDurationSeconds = maxSessionDurationSeconds;
        this.sessionTimeoutScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                () -> {
                    ON_TIMER_THREAD.set(true);

                    runnable.run();
                },
                "voice-session-timeout");

            thread.setDaemon(true);

            return thread;
        });

        this.sessionIdByWebSocketSessionId = Caffeine.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();

        this.liveSessionsBySessionId = Caffeine.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();

        this.sessionTimersBySessionId = Caffeine.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();
    }

    @Override
    public void start() {
        shuttingDown = false;
        running = true;
    }

    @Override
    public void stop() {
        running = false;

        shuttingDown = true;

        for (String sessionId : List.copyOf(liveSessionsBySessionId.asMap()
            .keySet())) {

            try {
                finalizeSession(sessionId, SERVER_SHUTDOWN, true);
            } catch (RuntimeException runtimeException) {
                log.warn("Failed to end voice session on shutdown: sessionId={}", sessionId, runtimeException);
            }
        }

        awaitHandOffs();
    }

    private void awaitHandOffs() {
        long deadline = System.nanoTime() + SHUTDOWN_WAIT.toNanos();

        while (!handOffThreads.isEmpty()) {
            long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());

            if (remainingMillis <= 0) {
                log.warn(
                    "Stopped waiting for voice sessions after {}s: {} session start(s) or end(s) still running",
                    SHUTDOWN_WAIT.toSeconds(), handOffThreads.size());

                return;
            }

            for (Thread handOffThread : List.copyOf(handOffThreads)) {
                try {
                    handOffThread.join(Math.max(1L, remainingMillis));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread()
                        .interrupt();

                    return;
                }
            }
        }
    }

    private void startHandOff(Runnable task) {
        Thread handOffThread = Thread.ofVirtual()
            .unstarted(() -> {
                try {
                    task.run();
                } finally {
                    handOffThreads.remove(Thread.currentThread());
                }
            });

        handOffThreads.add(handOffThread);

        handOffThread.start();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        String webhookId = extractId(uri);
        String sessionToken = extractQueryParam(uri, "sessionToken");

        log.info(
            "WebSocket connection established for webhook: {}, webSocketSessionId: {}, hasToken: {}", webhookId,
            session.getId(), sessionToken != null);

        if (shuttingDown) {
            log.info("Voice WS upgrade refused, the server is shutting down: webSocketSessionId={}", session.getId());

            session.close(CloseStatus.SERVICE_RESTARTED.withReason(SERVER_SHUTDOWN));

            return;
        }

        if (sessionToken == null) {
            log.warn("Webhook WS upgrade rejected: no session token for webhook={}", webhookId);

            Map<String, Object> error = new LinkedHashMap<>();

            error.put("type", "error");
            error.put("message", "A voice session token is required");

            sendMessage(session, error);
            session.close(CloseStatus.POLICY_VIOLATION);

            return;
        }

        if (webhookId == null || !browserVoiceSessionTokenService.consume(sessionToken, webhookId)) {
            log.warn("Browser-voice WS upgrade rejected: invalid token for webhook={}", webhookId);

            Map<String, Object> error = new LinkedHashMap<>();

            error.put("type", "error");
            error.put("message", "Invalid or expired session token");

            sendMessage(session, error);
            session.close(CloseStatus.POLICY_VIOLATION);

            return;
        }

        String resumeSessionId = extractQueryParam(uri, "resumeSessionId");

        if (resumeSessionId != null && resumeSession(session, resumeSessionId, webhookId)) {
            return;
        }

        String sessionId = UUID.randomUUID()
            .toString();

        VoiceSessionRegistry.Session voiceSession = voiceSessionRegistry.register(sessionId, session, webhookId);

        sessionIdByWebSocketSessionId.put(session.getId(), sessionId);
        voiceMetricsRecorder.recordSessionOpened();

        startHandOff(() -> startSession(session, voiceSession, webhookId));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        ByteBuffer payload = message.getPayload();
        byte[] bytes = new byte[payload.remaining()];

        payload.get(bytes);

        String sessionId = sessionIdByWebSocketSessionId.getIfPresent(session.getId());

        if (sessionId == null) {
            return;
        }

        voiceSessionRegistry.get(sessionId)
            .ifPresent(voiceSession -> {
                voiceSession.touchInboundAudio();

                Long engineSessionId = voiceSession.engineSessionId();

                if (engineSessionId != null) {
                    voiceSessionEngine.emitter(engineSessionId)
                        .ifPresent(emitter -> emitter.dispatchBinaryMessage(bytes));
                }
            });
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

        if (log.isDebugEnabled()) {
            log.debug("Voice WS control frame: webSocketSessionId={}, payload={}", session.getId(), payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String webSocketSessionId = session.getId();

        log.info("WebSocket connection closed: webSocketSessionId={}, status={}", webSocketSessionId, status);

        String serverCloseReason = serverCloseReasonsByWebSocketSessionId.remove(webSocketSessionId);
        String sessionId = sessionIdByWebSocketSessionId.getIfPresent(webSocketSessionId);

        if (sessionId == null) {
            return;
        }

        sessionIdByWebSocketSessionId.invalidate(webSocketSessionId);

        Optional<VoiceSessionRegistry.Session> voiceSessionOptional = voiceSessionRegistry.get(sessionId);

        if (voiceSessionOptional.isEmpty()) {
            return;
        }

        VoiceSessionRegistry.Session voiceSession = voiceSessionOptional.get();

        if (WebSocketSessionDecorator.unwrap(voiceSession.webSocketSession()) != session) {
            return;
        }

        String endReason = serverCloseReason;

        if (endReason == null && isClientHangUp(status)) {
            endReason = CLIENT_END_REASON;
        }

        if (endReason == null) {
            Instant detachedAt;

            synchronized (voiceSession) {
                voiceSessionRegistry.detach(sessionId);
                cancelTimers(sessionId);

                detachedAt = voiceSession.detachedAt();
            }

            sessionTimeoutScheduler.schedule(
                () -> finalizeIfStillDetached(sessionId, detachedAt), VoiceSessionRegistry.RESUME_WINDOW.toMillis(),
                TimeUnit.MILLISECONDS);

            log.info("Voice session detached, awaiting resume: sessionId={}", sessionId);

            return;
        }

        finalizeSession(sessionId, endReason, false);
    }

    void closeIfSilent(String sessionId) {
        LiveSession liveSession = liveSessionsBySessionId.getIfPresent(sessionId);

        if (liveSession == null || liveSession.silenceTimeoutSeconds() <= 0) {
            return;
        }

        voiceSessionRegistry.get(sessionId)
            .ifPresent(voiceSession -> {
                Duration silence = Duration.between(voiceSession.lastInboundAudioAt(), voiceSessionRegistry.now());

                if (silence.toSeconds() >= liveSession.silenceTimeoutSeconds()) {
                    log.info("Closing voice session after {}s of silence: sessionId={}", silence.toSeconds(),
                        sessionId);

                    closeForServerReason(voiceSession.webSocketSession(),
                        CloseStatus.NORMAL.withReason(SILENCE_TIMEOUT));
                }
            });
    }

    void finalizeIfStillDetached(String sessionId, @Nullable Instant detachedAt) {
        if (detachedAt == null) {
            return;
        }

        Optional<VoiceSessionRegistry.Session> voiceSessionOptional = voiceSessionRegistry.get(sessionId);

        if (voiceSessionOptional.isEmpty()) {
            return;
        }

        VoiceSessionRegistry.Session voiceSession = voiceSessionOptional.get();
        LiveSession liveSession;

        synchronized (voiceSession) {
            if (!detachedAt.equals(voiceSession.detachedAt())) {
                return;
            }

            liveSession = liveSessionsBySessionId.getIfPresent(sessionId);

            voiceSession.markEnded();
            liveSessionsBySessionId.invalidate(sessionId);
            voiceSessionRegistry.remove(sessionId);
        }

        endSession(voiceSession, liveSession, CLIENT_CLOSED);
    }

    void recordServerClose(String webSocketSessionId, String reason) {
        serverCloseReasonsByWebSocketSessionId.put(webSocketSessionId, reason);
    }

    boolean hasLiveSession(String sessionId) {
        return liveSessionsBySessionId.getIfPresent(sessionId) != null;
    }

    int scheduledTimerCount(String sessionId) {
        List<ScheduledFuture<?>> timers = sessionTimersBySessionId.getIfPresent(sessionId);

        if (timers == null) {
            return 0;
        }

        return (int) timers.stream()
            .filter(timer -> !timer.isDone())
            .count();
    }

    static Map<String, Object> sessionOutput(
        VoiceSessionRegistry.Session voiceSession, @Nullable VoiceSession engineSession, String endReason,
        Instant endedAt) {

        Map<String, Object> output = new LinkedHashMap<>();

        output.put("sessionId", voiceSession.sessionId());
        output.put(
            "startedAt", voiceSession.startedAt()
                .toString());
        output.put(
            "durationSeconds", Duration.between(voiceSession.startedAt(), endedAt)
                .toSeconds());
        output.put("endReason", endReason);
        output.put(
            "transcript", engineSession == null ? List.of()
                : engineSession.transcript()
                    .entries());
        output.put(
            "toolCalls", engineSession == null ? List.of()
                : engineSession.transcript()
                    .toolCalls());

        return output;
    }

    private void startSession(WebSocketSession session, VoiceSessionRegistry.Session voiceSession, String webhookId) {
        WorkflowExecutionId workflowExecutionId;

        try {
            workflowExecutionId = WorkflowExecutionId.parse(webhookId);
        } catch (RuntimeException runtimeException) {
            failStart(session, voiceSession, runtimeException);

            return;
        }

        TenantContext.runWithTenantId(
            workflowExecutionId.getTenantId(),
            () -> startSessionInTenant(session, voiceSession, webhookId, workflowExecutionId));
    }

    private void startSessionInTenant(
        WebSocketSession session, VoiceSessionRegistry.Session voiceSession, String webhookId,
        WorkflowExecutionId workflowExecutionId) {

        String sessionId = voiceSession.sessionId();

        try {
            if (!triggerResolver.isWorkflowEnabled(workflowExecutionId)) {
                refuseDisabledWorkflow(session, voiceSession);

                return;
            }

            WorkflowTrigger workflowTrigger = triggerResolver.resolve(workflowExecutionId);

            Map<String, Object> inputs = new LinkedHashMap<>();

            inputs.put("sessionId", sessionId);
            inputs.put(
                "startedAt", voiceSession.startedAt()
                    .toString());
            inputs.put("mainWorkflowExecutionId", webhookId);

            VoiceSession engineSession = voiceSessionEngine.start(
                workflowTrigger.getExtensions(),
                voiceSessionConnectionResolver.resolveDeployed(workflowExecutionId, workflowTrigger), inputs,
                workflowExecutionId.getJobPrincipalId(), null, triggerResolver.getEnvironmentId(workflowExecutionId),
                workflowExecutionId.getType(), false, emitter -> attachOutboundBridge(voiceSession, emitter));

            voiceSession.engineSessionId(engineSession.sessionId());

            LiveSession liveSession = new LiveSession(
                engineSession,
                MapUtils.getInteger(
                    workflowTrigger.getParameters(), SILENCE_TIMEOUT_SECONDS, DEFAULT_SILENCE_TIMEOUT_SECONDS),
                sessionLimitSeconds(workflowTrigger));

            boolean attached;

            synchronized (voiceSession) {
                liveSessionsBySessionId.put(sessionId, liveSession);

                attached = voiceSessionRegistry.isAttached(sessionId);

                if (attached) {
                    scheduleTimers(voiceSession, liveSession);
                }
            }

            if (shuttingDown) {
                if (!finalizeSession(sessionId, SERVER_SHUTDOWN, true)) {
                    stopIfEndedWhileStarting(sessionId, liveSession);
                }

                return;
            }

            if (!attached) {
                stopIfEndedWhileStarting(sessionId, liveSession);

                return;
            }

            sendMessage(voiceSession.webSocketSession(), connectedEvent(webhookId, sessionId, liveSession, false));
        } catch (VoiceSessionEngine.NoVoiceAgentException noVoiceAgentException) {
            refuseWithoutVoiceAgent(session, voiceSession);
        } catch (Exception exception) {
            failStart(session, voiceSession, exception);
        }
    }

    private void stopIfEndedWhileStarting(String sessionId, LiveSession liveSession) {
        if (voiceSessionRegistry.get(sessionId)
            .isPresent()) {

            return;
        }

        if (liveSessionsBySessionId.asMap()
            .remove(sessionId, liveSession)) {

            voiceSessionEngine.stop(liveSession.engineSession()
                .sessionId());
        }
    }

    private void refuseWithoutVoiceAgent(WebSocketSession session, VoiceSessionRegistry.Session voiceSession) {
        log.warn("Voice session refused, the trigger has no Voice Agent: sessionId={}", voiceSession.sessionId());

        voiceMetricsRecorder.recordSessionError();

        synchronized (voiceSession) {
            voiceSession.markEnded();
            voiceSessionRegistry.remove(voiceSession.sessionId());
        }

        Map<String, Object> error = new LinkedHashMap<>();

        error.put("type", "error");
        error.put("message", "No Voice Agent configured on the trigger");

        WebSocketSession currentSocket = Objects.requireNonNullElse(voiceSession.webSocketSession(), session);

        sendMessage(currentSocket, error);

        closeQuietly(currentSocket, CloseStatus.POLICY_VIOLATION.withReason(VOICE_AGENT_MISSING));
    }

    private void failStart(WebSocketSession session, VoiceSessionRegistry.Session voiceSession, Exception exception) {
        log.error("Failed to start voice session: sessionId={}", voiceSession.sessionId(), exception);

        voiceMetricsRecorder.recordSessionError();

        Map<String, Object> error = new LinkedHashMap<>();

        error.put("type", "error");
        error.put("message", Objects.toString(exception.getMessage(), "Failed to start the voice session"));

        WebSocketSession currentSocket = Objects.requireNonNullElse(voiceSession.webSocketSession(), session);

        sendMessage(currentSocket, error);
        closeForServerReason(currentSocket, CloseStatus.POLICY_VIOLATION.withReason(PROVIDER_ERROR));
    }

    private void refuseDisabledWorkflow(WebSocketSession session, VoiceSessionRegistry.Session voiceSession) {
        log.info("Voice session refused, the workflow is disabled: sessionId={}", voiceSession.sessionId());

        voiceMetricsRecorder.recordSessionError();

        synchronized (voiceSession) {
            voiceSession.markEnded();
            voiceSessionRegistry.remove(voiceSession.sessionId());
        }

        Map<String, Object> error = new LinkedHashMap<>();

        error.put("type", "error");
        error.put("message", "The workflow behind this voice session is disabled.");

        WebSocketSession currentSocket = Objects.requireNonNullElse(voiceSession.webSocketSession(), session);

        sendMessage(currentSocket, error);

        closeQuietly(currentSocket, CloseStatus.POLICY_VIOLATION.withReason(WORKFLOW_DISABLED));
    }

    private boolean resumeSession(WebSocketSession session, String resumeSessionId, String webhookId) {
        boolean sameWebhook = voiceSessionRegistry.get(resumeSessionId)
            .map(VoiceSessionRegistry.Session::workflowExecutionId)
            .filter(webhookId::equals)
            .isPresent();

        if (!sameWebhook) {
            log.info(
                "Voice session could not be resumed, starting a new one: resumeSessionId={} is not a session of this " +
                    "webhook",
                resumeSessionId);

            return false;
        }

        Optional<VoiceSessionRegistry.Session> resumed = voiceSessionRegistry.resume(resumeSessionId, session);

        if (resumed.isEmpty()) {
            log.info("Voice session could not be resumed, starting a new one: resumeSessionId={}", resumeSessionId);

            return false;
        }

        VoiceSessionRegistry.Session voiceSession = resumed.get();
        LiveSession liveSession = liveSessionsBySessionId.getIfPresent(resumeSessionId);

        if (liveSession == null) {
            voiceSessionRegistry.remove(resumeSessionId);

            return false;
        }

        sessionIdByWebSocketSessionId.put(session.getId(), resumeSessionId);

        scheduleTimers(voiceSession, liveSession);

        sendMessage(voiceSession.webSocketSession(), connectedEvent(webhookId, resumeSessionId, liveSession, true));

        log.info("Voice session resumed: sessionId={}", resumeSessionId);

        return true;
    }

    private boolean finalizeSession(String sessionId, String endReason, boolean closeSocket) {
        Optional<VoiceSessionRegistry.Session> voiceSessionOptional = voiceSessionRegistry.get(sessionId);

        if (voiceSessionOptional.isEmpty()) {
            return false;
        }

        VoiceSessionRegistry.Session voiceSession = voiceSessionOptional.get();
        LiveSession liveSession;
        WebSocketSession attachedSocket;

        synchronized (voiceSession) {
            if (voiceSessionRegistry.get(sessionId)
                .isEmpty()) {

                return false;
            }

            liveSession = liveSessionsBySessionId.getIfPresent(sessionId);
            attachedSocket = voiceSession.webSocketSession();

            voiceSession.markEnded();
            liveSessionsBySessionId.invalidate(sessionId);
            voiceSessionRegistry.remove(sessionId);
            cancelTimers(sessionId);
        }

        if (closeSocket) {
            closeForServerReason(attachedSocket, CloseStatus.NORMAL.withReason(endReason));
        }

        endSession(voiceSession, liveSession, endReason);

        return true;
    }

    private void endSession(
        VoiceSessionRegistry.Session voiceSession, @Nullable LiveSession liveSession, String endReason) {

        String tenantId = tenantIdOf(voiceSession);

        if (ON_TIMER_THREAD.get()) {
            startHandOff(
                () -> TenantContext.runWithTenantId(
                    tenantId, () -> endSessionInTenant(voiceSession, liveSession, endReason)));

            return;
        }

        TenantContext.runWithTenantId(tenantId, () -> endSessionInTenant(voiceSession, liveSession, endReason));
    }

    private static String tenantIdOf(VoiceSessionRegistry.Session voiceSession) {
        String workflowExecutionId = voiceSession.workflowExecutionId();

        if (workflowExecutionId == null) {
            return TenantContext.getCurrentTenantId();
        }

        try {
            return WorkflowExecutionId.parse(workflowExecutionId)
                .getTenantId();
        } catch (RuntimeException runtimeException) {
            return TenantContext.getCurrentTenantId();
        }
    }

    private void endSessionInTenant(
        VoiceSessionRegistry.Session voiceSession, @Nullable LiveSession liveSession, String endReason) {

        cancelTimers(voiceSession.sessionId());

        Long engineSessionId = voiceSession.engineSessionId();

        if (engineSessionId != null) {
            try {
                voiceSessionEngine.stop(engineSessionId);
            } catch (Exception exception) {
                log.warn(
                    "Failed to stop voice session: sessionId={}, engineSessionId={}", voiceSession.sessionId(),
                    engineSessionId, exception);
            }
        }

        Instant endedAt = voiceSessionRegistry.now();
        String workflowExecutionId = voiceSession.workflowExecutionId();

        if (workflowExecutionId != null) {
            workflowContinuationHelper.createContinuationJob(
                workflowExecutionId,
                sessionOutput(
                    voiceSession, liveSession == null ? null : liveSession.engineSession(), endReason, endedAt));
        }

        voiceMetricsRecorder.recordSessionClosed(Duration.between(voiceSession.startedAt(), endedAt));

        log.info("Voice session ended: sessionId={}, endReason={}", voiceSession.sessionId(), endReason);
    }

    private void scheduleTimers(VoiceSessionRegistry.Session voiceSession, LiveSession liveSession) {
        String sessionId = voiceSession.sessionId();

        cancelTimers(sessionId);

        List<ScheduledFuture<?>> timers = new CopyOnWriteArrayList<>();

        if (liveSession.sessionLimitSeconds() > 0) {
            long elapsedSeconds = Duration.between(voiceSession.startedAt(), voiceSessionRegistry.now())
                .toSeconds();
            long remainingSeconds = Math.max(0, liveSession.sessionLimitSeconds() - elapsedSeconds);

            timers.add(
                sessionTimeoutScheduler.schedule(
                    () -> closeAtSessionLimit(sessionId, liveSession.sessionLimitSeconds()), remainingSeconds,
                    TimeUnit.SECONDS));
        }

        if (liveSession.silenceTimeoutSeconds() > 0) {
            timers.add(
                sessionTimeoutScheduler.scheduleWithFixedDelay(
                    () -> runSilenceCheck(sessionId), SILENCE_CHECK_INTERVAL_SECONDS, SILENCE_CHECK_INTERVAL_SECONDS,
                    TimeUnit.SECONDS));
        }

        sessionTimersBySessionId.put(sessionId, timers);
    }

    private void cancelTimers(String sessionId) {
        List<ScheduledFuture<?>> timers = sessionTimersBySessionId.getIfPresent(sessionId);

        if (timers == null) {
            return;
        }

        for (ScheduledFuture<?> timer : timers) {
            timer.cancel(false);
        }

        sessionTimersBySessionId.invalidate(sessionId);
    }

    private void closeAtSessionLimit(String sessionId, long sessionLimitSeconds) {
        voiceSessionRegistry.get(sessionId)
            .ifPresent(voiceSession -> {
                log.info(
                    "Closing voice session at its session limit ({}s): sessionId={}", sessionLimitSeconds, sessionId);

                closeForServerReason(voiceSession.webSocketSession(), CloseStatus.NORMAL.withReason(SESSION_LIMIT));
            });
    }

    private void runSilenceCheck(String sessionId) {
        try {
            closeIfSilent(sessionId);
        } catch (Exception exception) {
            log.warn("Silence check failed: sessionId={}", sessionId, exception);
        }
    }

    private long sessionLimitSeconds(WorkflowTrigger workflowTrigger) {
        int triggerLimitSeconds = MapUtils.getInteger(workflowTrigger.getParameters(), SESSION_LIMIT_SECONDS, 0);

        return triggerLimitSeconds > 0 ? triggerLimitSeconds : maxSessionDurationSeconds;
    }

    private static Map<String, Object> connectedEvent(
        String webhookId, String sessionId, LiveSession liveSession, boolean resumed) {

        Map<String, Object> connected = new LinkedHashMap<>();

        connected.put("event", "connected");
        connected.put("id", webhookId);
        connected.put("sessionId", sessionId);
        connected.put(
            "outputSampleRate", liveSession.engineSession()
                .outputSampleRate());
        connected.put("silenceTimeoutSeconds", liveSession.silenceTimeoutSeconds());

        if (resumed) {
            connected.put("resumed", true);
        }

        return connected;
    }

    private static boolean isClientHangUp(@Nullable CloseStatus status) {
        return status != null && status.getCode() == CloseStatus.NORMAL.getCode() &&
            CLIENT_END_REASON.equals(status.getReason());
    }

    private void closeForServerReason(@Nullable WebSocketSession session, CloseStatus closeStatus) {
        if (session == null || !session.isOpen()) {
            return;
        }

        String reason = closeStatus.getReason();

        if (reason != null) {
            recordServerClose(session.getId(), reason);
        }

        closeQuietly(session, closeStatus);
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
                    "Failed to forward outbound text to WS session: sessionId={}", voiceSession.sessionId(),
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
                    "Failed to forward outbound binary to WS session: sessionId={}", voiceSession.sessionId(),
                    ioException);
            }
        });

        emitter.addErrorListener(throwable -> {
            providerFailed.set(true);

            log.warn("Voice Agent failed mid-session: sessionId={}", voiceSession.sessionId(), throwable);

            if (voiceSession.webSocketSession() == null) {
                finalizeSession(voiceSession.sessionId(), PROVIDER_ERROR, true);

                return;
            }

            closeForServerReason(voiceSession.webSocketSession(), CloseStatus.NORMAL.withReason(PROVIDER_ERROR));
        });

        emitter.addCompletionListener(() -> {
            if (providerFailed.get() || voiceSession.ended()) {
                return;
            }

            log.info("Voice Agent closed the session: sessionId={}", voiceSession.sessionId());

            finalizeSession(voiceSession.sessionId(), PROVIDER_CLOSED, true);
        });
    }

    private void sendMessage(@Nullable WebSocketSession session, Map<String, Object> data) {
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

    private static void closeQuietly(@Nullable WebSocketSession session, CloseStatus closeStatus) {
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            session.close(closeStatus);
        } catch (IOException ioException) {
            log.warn("Failed to close WS session: webSocketSessionId={}", session.getId(), ioException);
        }
    }

    private static @Nullable String extractId(@Nullable URI uri) {
        if (uri == null) {
            return null;
        }

        String path = uri.getPath();

        if (path == null) {
            return null;
        }

        String[] segments = path.split("/");

        for (int index = segments.length - 1; index >= 0; index--) {
            String segment = segments[index];

            if (!segment.isEmpty() && !"webhooks".equals(segment) && !"wss".equals(segment)) {
                return segment;
            }
        }

        return null;
    }

    private static @Nullable String extractQueryParam(@Nullable URI uri, String paramName) {
        if (uri == null) {
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

    private record LiveSession(VoiceSession engineSession, int silenceTimeoutSeconds, long sessionLimitSeconds) {
    }
}
