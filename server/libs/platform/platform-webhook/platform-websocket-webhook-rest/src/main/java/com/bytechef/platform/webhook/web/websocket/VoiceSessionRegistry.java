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

import com.bytechef.platform.webhook.annotation.ConditionalOnWebhook;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnWebhook
public class VoiceSessionRegistry {

    public static final Duration RESUME_WINDOW = Duration.ofSeconds(30);

    static final int SEND_TIME_LIMIT_MILLIS = 10_000;

    static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    private final Cache<String, Session> sessions = Caffeine.newBuilder()
        .expireAfterWrite(4, TimeUnit.HOURS)
        .maximumSize(10000)
        .build();
    private final Supplier<Instant> clock;

    @Autowired
    public VoiceSessionRegistry() {
        this(Instant::now);
    }

    VoiceSessionRegistry(Supplier<Instant> clock) {
        this.clock = clock;
    }

    public Session register(
        String sessionId, WebSocketSession webSocketSession, @Nullable String workflowExecutionId) {

        Session session = new Session(sessionId, concurrent(webSocketSession), workflowExecutionId, clock.get());

        sessions.put(sessionId, session);

        return session;
    }

    public Optional<Session> get(String sessionId) {
        return Optional.ofNullable(sessions.getIfPresent(sessionId));
    }

    public boolean isAttached(String sessionId) {
        Session session = sessions.getIfPresent(sessionId);

        return session != null && session.detachedAt == null;
    }

    public void detach(String sessionId) {
        Session session = sessions.getIfPresent(sessionId);

        if (session != null) {
            synchronized (session) {
                session.webSocketSession = null;
                session.detachedAt = clock.get();
            }
        }
    }

    public Optional<Session> resume(String sessionId, WebSocketSession webSocketSession) {
        Session session = sessions.getIfPresent(sessionId);

        if (session == null) {
            return Optional.empty();
        }

        synchronized (session) {
            if (session.detachedAt == null) {
                return Optional.empty();
            }

            Instant now = clock.get();

            if (session.detachedAt.plus(RESUME_WINDOW)
                .isBefore(now)) {

                sessions.invalidate(sessionId);

                return Optional.empty();
            }

            session.webSocketSession = concurrent(webSocketSession);
            session.detachedAt = null;
            session.lastInboundAudioAt = now;

            return Optional.of(session);
        }
    }

    public void remove(String sessionId) {
        Session session = sessions.getIfPresent(sessionId);

        if (session == null) {
            return;
        }

        synchronized (session) {
            sessions.invalidate(sessionId);
        }
    }

    Instant now() {
        return clock.get();
    }

    private static WebSocketSession concurrent(WebSocketSession webSocketSession) {
        if (webSocketSession instanceof ConcurrentWebSocketSessionDecorator) {
            return webSocketSession;
        }

        return new ConcurrentWebSocketSessionDecorator(
            webSocketSession, SEND_TIME_LIMIT_MILLIS, BUFFER_SIZE_LIMIT_BYTES);
    }

    public final class Session {

        private final String sessionId;
        private final Instant startedAt;
        private final @Nullable String workflowExecutionId;
        private volatile @Nullable WebSocketSession webSocketSession;
        private volatile @Nullable Long engineSessionId;
        private volatile @Nullable Instant detachedAt;
        private volatile Instant lastInboundAudioAt;
        private volatile boolean ended;

        private Session(
            String sessionId, WebSocketSession webSocketSession, @Nullable String workflowExecutionId,
            Instant startedAt) {

            this.sessionId = sessionId;
            this.webSocketSession = webSocketSession;
            this.workflowExecutionId = workflowExecutionId;
            this.startedAt = startedAt;
            this.lastInboundAudioAt = startedAt;
        }

        public String sessionId() {
            return sessionId;
        }

        public Instant startedAt() {
            return startedAt;
        }

        public @Nullable String workflowExecutionId() {
            return workflowExecutionId;
        }

        @SuppressFBWarnings("EI_EXPOSE_REP")
        public @Nullable WebSocketSession webSocketSession() {
            return webSocketSession;
        }

        public @Nullable Long engineSessionId() {
            return engineSessionId;
        }

        public void engineSessionId(long engineSessionId) {
            this.engineSessionId = engineSessionId;
        }

        public @Nullable Instant detachedAt() {
            return detachedAt;
        }

        public Instant lastInboundAudioAt() {
            return lastInboundAudioAt;
        }

        public void touchInboundAudio() {
            lastInboundAudioAt = clock.get();
        }

        public boolean ended() {
            return ended;
        }

        public void markEnded() {
            ended = true;
        }
    }
}
