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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * What the AI agent passes to its tools through Spring AI's {@link ToolContext}, stored under {@link #KEY}: the agent's
 * action context, and for a streaming agent the {@link SseTransport} of the response.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AiAgentToolContext(ActionContext actionContext, @Nullable SseTransport sseTransport) {

    public static final String KEY = "bytechef_aiAgentToolContext";

    public AiAgentToolContext {
        Objects.requireNonNull(actionContext, "actionContext");
    }

    public AiAgentToolContext(ActionContext actionContext) {
        this(actionContext, null);
    }

    public static @Nullable AiAgentToolContext fetch(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> toolContextMap = toolContext.getContext();

        return toolContextMap.get(KEY) instanceof AiAgentToolContext aiAgentToolContext ? aiAgentToolContext : null;
    }

    public Map<String, Object> toMap() {
        return Map.of(KEY, this);
    }

    /**
     * How a streaming agent and its tools send events on the response: through the SSE emitter once it is attached,
     * otherwise queued until then. Events go out in the order they were sent.
     */
    public static final class SseTransport {

        private final Queue<Map<String, @Nullable Object>> bufferedEvents = new ArrayDeque<>();
        private final ReentrantLock lock = new ReentrantLock();
        private @Nullable SseEmitter sseEmitter;

        /**
         * Attaches the response's emitter and sends the queued events through it; a queued event that cannot be sent is
         * reported to {@code bufferedEventFailureHandler}. An emitter can be attached only once.
         */
        public void attach(SseEmitter sseEmitter, Consumer<Exception> bufferedEventFailureHandler) {
            Objects.requireNonNull(sseEmitter, "sseEmitter");
            Objects.requireNonNull(bufferedEventFailureHandler, "bufferedEventFailureHandler");

            lock.lock();

            try {
                if (this.sseEmitter != null) {
                    throw new IllegalStateException("An SSE emitter is already attached");
                }

                this.sseEmitter = sseEmitter;

                Map<String, @Nullable Object> bufferedEvent;

                while ((bufferedEvent = bufferedEvents.poll()) != null) {
                    try {
                        sseEmitter.send(bufferedEvent);
                    } catch (Exception exception) {
                        bufferedEventFailureHandler.accept(exception);
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        /**
         * Sends the event through the attached emitter, or queues it until an emitter is attached. A failure to send
         * through an already attached emitter is thrown to the caller; a queued event that later fails to send goes to
         * the failure handler given to {@link #attach}.
         */
        public void send(Map<String, @Nullable Object> event) {
            lock.lock();

            try {
                if (sseEmitter == null) {
                    bufferedEvents.add(event);

                    return;
                }

                sseEmitter.send(event);
            } finally {
                lock.unlock();
            }
        }
    }
}
