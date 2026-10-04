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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.job.sync.SseStreamBridge;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Bridges a job's stream events to a Spring {@link SseEmitter}. Events arrive on message-broker threads concurrently
 * with job-status events and client disconnects. {@link #onComplete()}, {@link #onSuspend()} and
 * {@link #onError(Throwable)} are mutually exclusive and idempotent: only the first call takes effect, and none does
 * once the emitter has completed, timed out or failed. Sends after the emitter closed are skipped on a best-effort
 * basis, and a send that loses the race with a close or a client disconnect is logged at debug level; any other failed
 * send is logged as a warning. A lost {@code ask_user_question} event is always logged as a warning and thrown from
 * {@link #onEvent(Object)}, because the user then never sees the questions the agent is waiting for.
 *
 * @author Ivica Cardic
 */
final class SseEmitterStreamBridge implements SseStreamBridge {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterStreamBridge.class);

    private static final String EVENT = "event";
    private static final String PAYLOAD = "payload";
    private static final String SUSPENDED = "suspended";

    private final AtomicBoolean completed = new AtomicBoolean();
    private final SseEmitter emitter;

    SseEmitterStreamBridge(SseEmitter emitter) {
        this.emitter = emitter;

        emitter.onCompletion(() -> completed.set(true));
        emitter.onTimeout(() -> completed.set(true));
        emitter.onError(throwable -> completed.set(true));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            if (map.get(AiAgentSseEventType.EVENT_TYPE) instanceof String eventType) {
                Map<String, Object> eventData = new LinkedHashMap<>((Map<String, Object>) map);

                eventData.remove(AiAgentSseEventType.EVENT_TYPE);

                if (!sendEvent(eventType, eventData) && AiAgentSseEventType.ASK_USER_QUESTION.equals(eventType)) {
                    throw new IllegalStateException(
                        "The '" + AiAgentSseEventType.ASK_USER_QUESTION + "' SSE event was not delivered");
                }

                return;
            }

            if (map.get(EVENT) instanceof String event) {
                sendEvent(event, getEventData(map));

                return;
            }
        }

        sendEvent("stream", payload);
    }

    @Override
    public void onComplete() {
        if (!completed.compareAndSet(false, true)) {
            return;
        }

        try {
            emitter.complete();
        } catch (Exception exception) {
            log.debug("SseEmitter.complete() failed (likely already completed): {}", exception.getMessage());
        }
    }

    @Override
    public void onSuspend() {
        if (!completed.compareAndSet(false, true)) {
            return;
        }

        try {
            send(SUSPENDED, "");
        } finally {
            try {
                emitter.complete();
            } catch (Exception exception) {
                log.debug(
                    "SseEmitter.complete() failed after onSuspend (likely already completed): {}",
                    exception.getMessage());
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (!completed.compareAndSet(false, true)) {
            return;
        }

        try {
            send("error", Objects.toString(throwable.getMessage(), "An error occurred"));
        } finally {
            try {
                emitter.complete();
            } catch (Exception exception) {
                log.debug(
                    "SseEmitter.complete() failed after onError (likely already completed): {}",
                    exception.getMessage());
            }
        }
    }

    private static @Nullable Object getEventData(Map<?, ?> map) {
        if (map.containsKey(PAYLOAD)) {
            return map.get(PAYLOAD);
        }

        return map.entrySet()
            .stream()
            .filter(entry -> !EVENT.equals(entry.getKey()))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElse(null);
    }

    private boolean sendEvent(String name, @Nullable Object data) {
        if (completed.get()) {
            logDroppedEvent(name, "the SSE connection is already closed", null);

            return false;
        }

        return send(name, data);
    }

    private boolean send(String name, Object data) {
        try {
            emitter.send(
                SseEmitter.event()
                    .name(name)
                    .data(data instanceof String ? JsonUtils.write(data) : data));

            return true;
        } catch (AsyncRequestNotUsableException exception) {
            logDroppedEvent(name, "the client disconnected", exception);
        } catch (IllegalStateException exception) {
            logDroppedEvent(name, "the SSE connection closed while it was sent", exception);
        } catch (Exception exception) {
            log.warn("The '{}' SSE event was not delivered because the send failed", name, exception);
        }

        return false;
    }

    private static void logDroppedEvent(String name, String reason, @Nullable Exception exception) {
        if (AiAgentSseEventType.ASK_USER_QUESTION.equals(name)) {
            log.warn("The '{}' SSE event was not delivered because {}", name, reason, exception);
        } else if (log.isDebugEnabled()) {
            log.debug("The '{}' SSE event was not delivered because {}", name, reason, exception);
        }
    }
}
