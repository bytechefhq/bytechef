/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.event.CustomEvent;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-local holder for the parent agent's {@link AgentSubscriber}.
 *
 * <p>
 * {@link com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent} stashes its subscriber here before each subagent tool-call
 * executes. {@link ProgressReportingToolCallback} (and any other tool that wants to surface real-time progress) can
 * call {@link #emitProgress(String, String)} to emit a {@code subagent-progress} AG-UI {@link CustomEvent} on that
 * subscriber without having a direct reference to it.
 * </p>
 *
 * <p>
 * Uses a plain {@link ThreadLocal} (not {@code InheritableThreadLocal}) because the parent agent's tool callbacks run
 * synchronously on the same thread that set the holder.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SubagentProgressEmitter {

    /**
     * The event name sent in AG-UI {@link CustomEvent#getName()} so the client can identify progress messages.
     */
    public static final String EVENT_NAME = "subagent-progress";

    private static final Logger log = LoggerFactory.getLogger(SubagentProgressEmitter.class);

    private static final ThreadLocal<AgentSubscriber> HOLDER = new ThreadLocal<>();

    /**
     * Tracks whether we have already logged the "no subscriber bound" warning for this thread. Without this guard,
     * every emitProgress / drainAndEmit call from an unwired caller would spam the log; once-per-thread is enough to
     * surface a wiring regression without flooding the log.
     */
    private static final ThreadLocal<Boolean> NO_SUBSCRIBER_LOGGED = ThreadLocal.withInitial(() -> false);

    private SubagentProgressEmitter() {
    }

    /**
     * Binds the given subscriber to the current thread, runs {@code runnable}, then restores the previous binding.
     * Resets the once-per-thread missing-subscriber log flag both before and after the runnable so a pooled thread
     * cannot carry a latched flag between requests — without the {@code finally} reset, a thread that hit a wiring gap
     * once would silence WARNs for every later request that runs on the same pooled thread.
     */
    public static void runWithSubscriber(AgentSubscriber subscriber, Runnable runnable) {
        AgentSubscriber previous = HOLDER.get();

        HOLDER.set(subscriber);
        NO_SUBSCRIBER_LOGGED.set(false);

        try {
            runnable.run();
        } finally {
            if (previous == null) {
                HOLDER.remove();
            } else {
                HOLDER.set(previous);
            }

            NO_SUBSCRIBER_LOGGED.remove();
        }
    }

    /**
     * Emits a single subagent-progress {@link CustomEvent} on the subscriber currently bound to this thread. If no
     * subscriber is bound the call is silently ignored — but logs a WARN once per thread so a wiring regression (a
     * future caller forgetting to wrap with {@link #runWithSubscriber}) is observable in production logs without
     * needing DEBUG to be enabled.
     *
     * @param subagentName the name of the subagent (e.g. {@code "research"})
     * @param text         human-readable status text (e.g. {@code "Searching the web…"})
     */
    public static void emitProgress(String subagentName, String text) {
        AgentSubscriber subscriber = HOLDER.get();

        if (subscriber == null) {
            logMissingSubscriberOnce("emitProgress", subagentName);

            return;
        }

        CustomEvent event = new CustomEvent(
            EVENT_NAME,
            Map.of("subagentName", subagentName, "text", text));

        deliverIsolated(subscriber, event);
    }

    /**
     * Drains all events buffered in {@link SubagentProgressChannel} and emits each one via the subscriber bound to this
     * thread. No-op if no subscriber is bound or the channel is empty (logs WARN once per thread on missing subscriber
     * — see {@link #emitProgress(String, String)}).
     */
    public static void drainAndEmit() {
        AgentSubscriber subscriber = HOLDER.get();

        if (subscriber == null) {
            logMissingSubscriberOnce("drainAndEmit", null);

            return;
        }

        List<SubagentProgressChannel.SubagentProgressEvent> events = SubagentProgressChannel.drain();

        for (SubagentProgressChannel.SubagentProgressEvent progressEvent : events) {
            CustomEvent customEvent = new CustomEvent(
                EVENT_NAME,
                Map.of("subagentName", progressEvent.subagentName(), "text", progressEvent.text()));

            deliverIsolated(subscriber, customEvent);
        }
    }

    /**
     * Logs a single WARN line per thread the first time {@link #emitProgress} or {@link #drainAndEmit} runs without a
     * bound subscriber. The runtime no-op is intentional (unit tests / direct callers); the once-per-thread log
     * surfaces a wiring regression in production without flooding the log. WARN (not DEBUG) so the regression is
     * observable in default production log configurations.
     */
    private static void logMissingSubscriberOnce(String entryPoint, @org.jspecify.annotations.Nullable String detail) {
        if (NO_SUBSCRIBER_LOGGED.get()) {
            return;
        }

        NO_SUBSCRIBER_LOGGED.set(true);

        if (detail == null) {
            log.warn("{} called with no AgentSubscriber bound; events dropped silently", entryPoint);
        } else {
            log.warn(
                "{} called with no AgentSubscriber bound (detail={}); events dropped silently",
                entryPoint, detail);
        }
    }

    /**
     * Delivers the event to the subscriber, swallowing any exception thrown by the subscriber's handlers. Progress is
     * observability — a closed SSE channel or a misbehaving handler must not abort the surrounding tool call. Failures
     * are logged at WARN so ops still has signal.
     */
    private static void deliverIsolated(AgentSubscriber subscriber, CustomEvent event) {
        try {
            subscriber.onEvent(event);
        } catch (RuntimeException exception) {
            log.warn("Subscriber.onEvent threw while emitting subagent progress; ignored", exception);
        }

        try {
            subscriber.onCustomEvent(event);
        } catch (RuntimeException exception) {
            log.warn("Subscriber.onCustomEvent threw while emitting subagent progress; ignored", exception);
        }
    }
}
