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

package com.bytechef.ai.copilot.tool.ask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Tells a callback that wraps an intelligent delegate whether the call it forwarded ended in a specialist question
 * rather than a finished answer. {@link SubAgentAskRelayToolCallback} renders such a question as the delegate's
 * ordinary result, so the result alone cannot say which it was. The wrapper installs a signal in the context it
 * forwards; the relay marks it when a question is pending. A {@link ToolContext}'s map is read-only, so the signal is a
 * mutable holder placed in it.
 *
 * @author Ivica Cardic
 */
public final class SubAgentPendingQuestionSignal {

    private static final String PENDING_QUESTION_SIGNAL_KEY = "bytechef.subAgent.pendingQuestionSignal";

    private SubAgentPendingQuestionSignal() {
    }

    /**
     * Returns a copy of {@code context} carrying a fresh, unmarked signal. The original map is left untouched.
     */
    public static Map<String, Object> withSignal(Map<String, Object> context) {
        Map<String, Object> signalContext = new HashMap<>(context);

        signalContext.put(PENDING_QUESTION_SIGNAL_KEY, new AtomicBoolean());

        return signalContext;
    }

    /**
     * Marks the signal of {@code toolContext} as pending; a no-op when the context carries none.
     */
    public static void markPending(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return;
        }

        Map<String, Object> context = toolContext.getContext();

        if (context != null && context.get(PENDING_QUESTION_SIGNAL_KEY) instanceof AtomicBoolean signal) {
            signal.set(true);
        }
    }

    /**
     * Whether the signal installed in {@code context} was marked; {@code false} when there is no signal.
     */
    public static boolean isPending(Map<String, Object> context) {
        return context.get(PENDING_QUESTION_SIGNAL_KEY) instanceof AtomicBoolean signal && signal.get();
    }
}
