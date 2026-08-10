/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Thread-local channel carrying a question a specialist subagent raised during one delegation back out to the delegate
 * {@code ToolCallback}, which returns it as its own tool result so the parent agent's stream carries it to the client.
 *
 * <p>
 * Mirrors {@code SubagentProgressChannel}'s binding discipline — a {@link ThreadLocal} restored rather than cleared on
 * exit, so nesting is LIFO-safe — and differs in holding <b>at most one</b> payload. {@link #offer(String)} returns
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
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SubagentAskChannel {

    private static final ThreadLocal<String[]> HOLDER = new ThreadLocal<>();

    private SubagentAskChannel() {
    }

    /**
     * Binds a fresh single-slot channel to the current thread, runs {@code supplier}, then restores the previous
     * binding. Takes a {@link Supplier} rather than a {@code Runnable} because the delegate callback needs both the
     * wrapped call's return value and whatever question it raised.
     */
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

    /**
     * Records {@code payloadJson} as this delegation's pending question, returning {@code false} when no channel is
     * bound or one is already pending. An existing pending question is never overwritten.
     */
    public static boolean offer(String payloadJson) {
        String[] slot = HOLDER.get();

        if (slot == null || slot[0] != null) {
            return false;
        }

        slot[0] = payloadJson;

        return true;
    }

    /**
     * The question raised in the current delegation, or {@code null} when none was raised or no channel is bound.
     */
    public static @Nullable String pending() {
        String[] slot = HOLDER.get();

        return slot == null ? null : slot[0];
    }
}
