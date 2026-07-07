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

package com.bytechef.ai.agent.tool;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Thread-local agent identity binding used by the {@code UsageObservationHandler} (and the expensive tool callbacks) to
 * attribute LLM and tool calls to the right agent — including the parent-vs-subagent split.
 *
 * @author Ivica Cardic
 */
public final class CurrentAgentContext {

    /**
     * Immutable two-tuple of the agent and (optionally) the parent agent. {@code parentAgent} is {@code null} for
     * top-level ai_hub calls.
     */
    public record AgentBinding(Agent agentName, @Nullable Agent parentAgent) {
    }

    private static final InheritableThreadLocal<AgentBinding> HOLDER = new InheritableThreadLocal<>();

    private CurrentAgentContext() {
    }

    /**
     * Returns the agent binding currently in effect on this thread, or {@code null} when none has been bound.
     */
    public static AgentBinding current() {
        return HOLDER.get();
    }

    /**
     * Pushes a binding for the duration of {@code runnable}. The previous binding (if any) is restored even if the
     * runnable throws.
     */
    public static void runWith(Agent agentName, @Nullable Agent parentAgent, Runnable runnable) {
        AgentBinding previous = HOLDER.get();

        HOLDER.set(new AgentBinding(agentName, parentAgent));

        try {
            runnable.run();
        } finally {
            if (previous == null) {
                HOLDER.remove();
            } else {
                HOLDER.set(previous);
            }
        }
    }

    /**
     * Variant of {@link #runWith(Agent, Agent, Runnable)} that returns the supplier's value.
     */
    public static <T> T callWith(Agent agentName, @Nullable Agent parentAgent, Supplier<T> supplier) {
        AgentBinding previous = HOLDER.get();

        HOLDER.set(new AgentBinding(agentName, parentAgent));

        try {
            return supplier.get();
        } finally {
            if (previous == null) {
                HOLDER.remove();
            } else {
                HOLDER.set(previous);
            }
        }
    }
}
