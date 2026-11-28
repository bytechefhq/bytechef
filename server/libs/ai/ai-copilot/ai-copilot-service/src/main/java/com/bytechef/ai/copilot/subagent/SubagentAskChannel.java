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

package com.bytechef.ai.copilot.subagent;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Thread-local channel carrying a question a specialist subagent raised during one delegation back out to the delegate
 * {@code ToolCallback}, which returns it as its own tool result so the parent agent's stream carries it to the client.
 *
 * <p>
 * Mirrors {@code SubagentProgressChannel}'s binding discipline (a {@link ThreadLocal} restored rather than cleared on
 * exit, so nesting is LIFO-safe) and differs in holding <b>at most one</b> payload. {@link #offer(String)} returns
 * {@code false} when one is already pending, letting the ask tool turn a second question in a single delegation into a
 * tool error: two simultaneous questions have no sensible rendering, and the specialist should ask one thing, stop, and
 * continue once answered.
 * </p>
 *
 * <p>
 * Read {@link #pending()} from <b>inside</b> the {@link #runWithChannel(Supplier)} scope. Stashing the payload
 * somewhere that outlives the binding so it can be read afterwards would put shared mutable state on a singleton
 * serving every concurrent delegation, where one conversation's question could surface in another's.
 * </p>
 *
 * @author Ivica Cardic
 */
public final class SubagentAskChannel {

    private static final ThreadLocal<String[]> HOLDER = new ThreadLocal<>();

    private SubagentAskChannel() {
    }

    public static <T> T runWithChannel(Supplier<T> supplier) {
        String[] previous = HOLDER.get();

        HOLDER.set(new String[1]);

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

    public static boolean offer(String payloadJson) {
        String[] slot = HOLDER.get();

        if (slot == null || slot[0] != null) {
            return false;
        }

        slot[0] = payloadJson;

        return true;
    }

    public static @Nullable String pending() {
        String[] slot = HOLDER.get();

        return slot == null ? null : slot[0];
    }
}
