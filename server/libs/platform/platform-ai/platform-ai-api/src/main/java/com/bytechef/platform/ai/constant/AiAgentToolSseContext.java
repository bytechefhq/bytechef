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

package com.bytechef.platform.ai.constant;

import org.jspecify.annotations.Nullable;

/**
 * Bridges the agent's streaming SSE surface — the {@code SSE_EMITTER_REFERENCE} / {@code SSE_BUFFERED_EVENTS} entries
 * of the Spring AI {@code ToolContext} — into component tool functions that are invoked with a component context rather
 * than the raw {@code ToolContext} (e.g. the {@code approval/requestApproval} tool). The agent tool facade binds these
 * for the current thread immediately before invoking such a tool and clears them afterwards, so a tool that produces an
 * interactive card (an approval request) can forward it onto the live agent stream instead of dropping it. Held as
 * {@code Object} so the platform-ai module stays free of a Spring AI dependency; callers cast to the concrete emitter /
 * queue types they already work with.
 *
 * @author Ivica Cardic
 */
public final class AiAgentToolSseContext {

    private static final ThreadLocal<@Nullable Object> EMITTER_REFERENCE_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<@Nullable Object> BUFFERED_EVENTS_HOLDER = new ThreadLocal<>();

    private AiAgentToolSseContext() {
    }

    public static void set(@Nullable Object emitterReference, @Nullable Object bufferedEvents) {
        EMITTER_REFERENCE_HOLDER.set(emitterReference);
        BUFFERED_EVENTS_HOLDER.set(bufferedEvents);
    }

    public static void clear() {
        EMITTER_REFERENCE_HOLDER.remove();
        BUFFERED_EVENTS_HOLDER.remove();
    }

    public static @Nullable Object getEmitterReference() {
        return EMITTER_REFERENCE_HOLDER.get();
    }

    public static @Nullable Object getBufferedEvents() {
        return BUFFERED_EVENTS_HOLDER.get();
    }
}
