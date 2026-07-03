/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Thread-local channel for buffering subagent tool-call progress events.
 *
 * <p>
 * {@link ProgressReportingToolCallback} publishes events here while executing a subagent.
 * {@link com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent} drains the channel after each subagent tool call returns and
 * emits the accumulated events as AG-UI {@link com.agui.core.event.CustomEvent}s.
 * </p>
 *
 * <p>
 * Use {@link #runWithChannel(Runnable)} to bind a fresh channel to the current thread. The binding is removed
 * automatically when the runnable exits (LIFO-safe for nested calls).
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SubagentProgressChannel {

    private static final ThreadLocal<Deque<SubagentProgressEvent>> HOLDER = new ThreadLocal<>();

    private SubagentProgressChannel() {
    }

    /**
     * Binds a fresh progress channel to the current thread, runs {@code runnable}, then removes the binding.
     */
    public static void runWithChannel(Runnable runnable) {
        Deque<SubagentProgressEvent> previous = HOLDER.get();

        HOLDER.set(new ArrayDeque<>());

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
     * Returns {@code true} when a channel is bound to the current thread.
     */
    public static boolean isActive() {
        return HOLDER.get() != null;
    }

    /**
     * Publishes a progress event to the channel bound to the current thread. If no channel is active the call is
     * silently ignored.
     */
    public static void publish(String subagentName, String text) {
        Deque<SubagentProgressEvent> channel = HOLDER.get();

        if (channel != null) {
            channel.addLast(new SubagentProgressEvent(subagentName, text, Instant.now()));
        }
    }

    /**
     * Removes and returns all events that have been published since the last drain (or since the channel was created).
     * Returns an empty list if no channel is active.
     */
    public static List<SubagentProgressEvent> drain() {
        Deque<SubagentProgressEvent> channel = HOLDER.get();

        if (channel == null || channel.isEmpty()) {
            return List.of();
        }

        List<SubagentProgressEvent> events = new ArrayList<>(channel);

        channel.clear();

        return events;
    }

    /**
     * Immutable snapshot of one subagent progress notification.
     *
     * @param subagentName the subagent that produced the event (e.g. {@code "research"})
     * @param text         human-readable status text shown in the UI (e.g. {@code "Searching the web…"})
     * @param timestamp    wall-clock time when the event was produced
     */
    public record SubagentProgressEvent(String subagentName, String text, Instant timestamp) {
    }
}
