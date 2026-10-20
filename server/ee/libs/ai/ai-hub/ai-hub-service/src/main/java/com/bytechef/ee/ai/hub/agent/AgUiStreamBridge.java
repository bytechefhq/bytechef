/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.CustomEvent;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.event.RunFinishedEvent;
import com.agui.core.event.TextMessageContentEvent;
import com.agui.core.event.TextMessageEndEvent;
import com.agui.core.event.TextMessageStartEvent;
import com.agui.core.event.ToolCallEndEvent;
import com.agui.core.event.ToolCallStartEvent;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.job.sync.SseStreamBridge;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link SseStreamBridge} implementation that translates the events of one workflow chat turn into AG-UI subscriber
 * events, so the AI Hub client renders a workflow run the same way it renders an agent run.
 *
 * <p>
 * Translation table:
 * </p>
 * <ul>
 * <li><b>{@code onEvent(String chunk)}</b>: emit {@link TextMessageContentEvent} (delta). The first chunk also fires
 * {@link TextMessageStartEvent} so the client knows a new message is starting.</li>
 * <li><b>{@code onEvent(Map {event=start})}</b>: report the job id to the {@code onJobId} callback, for the first start
 * event only. Not surfaced to the client.</li>
 * <li><b>{@code onEvent(Map {event=task_started})}</b>: emit a tool-call start/end pair labelled with the task name so
 * the chat shows live step progress.</li>
 * <li><b>{@code onEvent(Map {event=result})}</b>: render the final reply when nothing was streamed before it.</li>
 * <li><b>{@code onEvent(Map ask_user_question)}</b>: register the resume URL and translate to a {@link CustomEvent}
 * named {@code "ask-workflow-question"}. A question without a resume URL (the editor environment) is still shown, but
 * no resume URL is registered and the run ends normally.</li>
 * <li><b>{@code onEvent(Map <ai-agent-sse-event-type>)}</b>: pass through as a {@link CustomEvent} named after the
 * event type.</li>
 * <li><b>{@code onEvent(Map other)}</b>: forward as a generic {@code CustomEvent("workflow-event", map)}.</li>
 * <li><b>{@code onComplete()}</b>: emit {@link TextMessageEndEvent} (when a message was started) followed by
 * {@link RunFinishedEvent}.</li>
 * <li><b>{@code onError(Throwable)}</b>: run the {@code onStreamError} callback, which stops this turn's own job, then
 * emit {@link RunErrorEvent} with the throwable's message.</li>
 * </ul>
 *
 * <p>
 * <b>Idempotency:</b> {@code onComplete()} and {@code onError(...)} both flip an internal {@code completed} flag and
 * skip duplicate emissions, because the webhook executor can deliver both a stream-level and a future-level terminal
 * signal under some failure modes. Both run {@code onFinalize} last, exactly once. Event dispatch and the terminal
 * transition share one lock, so a chunk arriving on the stream thread is either emitted before the terminal event or
 * dropped, never emitted after it.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AgUiStreamBridge implements SseStreamBridge {

    private static final Logger log = LoggerFactory.getLogger(AgUiStreamBridge.class);

    /**
     * Custom event name the client renders as an inline follow-up question form when a workflow emits
     * {@code ask_user_question}.
     */
    public static final String ASK_WORKFLOW_QUESTION_EVENT_NAME = "ask-workflow-question";

    /**
     * Generic fallback custom event name for stream payloads that do not match a known shape.
     */
    public static final String WORKFLOW_EVENT_NAME = "workflow-event";

    private final AgentSubscriber subscriber;
    private final String messageId;
    private final String threadId;
    private final String runId;
    private final WebhookResumeRegistry resumeRegistry;
    private final LongConsumer onJobId;
    private final Runnable onStreamError;
    private final Runnable onFinalize;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicBoolean jobIdReported = new AtomicBoolean(false);
    private final Object dispatchLock = new Object();
    private Long pendingJobId;
    private final StringBuilder assistantTextBuilder = new StringBuilder();

    /**
     * @param threadId       the chat's thread id, also the {@link WebhookResumeRegistry} key
     * @param resumeRegistry where {@code ask_user_question} resume URLs land
     * @param onJobId        called once with the job id from the {@code start} event
     * @param onStreamError  called by {@link #onError} before the error is emitted, to stop this turn's own job
     * @param onFinalize     called as the last step of {@link #onComplete} / {@link #onError}, exactly once
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AgUiStreamBridge(
        AgentSubscriber subscriber, String messageId, String threadId, String runId,
        WebhookResumeRegistry resumeRegistry, LongConsumer onJobId, Runnable onStreamError, Runnable onFinalize) {

        this.subscriber = subscriber;
        this.messageId = messageId;
        this.threadId = threadId;
        this.runId = runId;
        this.resumeRegistry = resumeRegistry;
        this.onJobId = onJobId;
        this.onStreamError = onStreamError;
        this.onFinalize = onFinalize;
    }

    /**
     * Whether at least one text-message event was emitted through this bridge.
     */
    public boolean hasStarted() {
        return started.get();
    }

    /**
     * Returns the assistant text accumulated from streamed chunks (or the final result), or null when nothing was
     * emitted. Used to persist the assistant reply after a run finishes.
     */
    @Nullable
    public String getAccumulatedAssistantText() {
        if (assistantTextBuilder.length() == 0) {
            return null;
        }

        return assistantTextBuilder.toString();
    }

    @Override
    public void onEvent(Object payload) {
        Long jobIdToReport;

        synchronized (dispatchLock) {
            dispatchEvent(payload);

            jobIdToReport = pendingJobId;
            pendingJobId = null;
        }

        if (jobIdToReport != null) {
            onJobId.accept(jobIdToReport);
        }
    }

    private void dispatchEvent(Object payload) {
        if (completed.get()) {
            if (log.isDebugEnabled()) {
                log.debug("AgUiStreamBridge dropping post-complete event: {}", payload);
            }

            return;
        }

        if (payload instanceof Map<?, ?> map) {
            onMapEvent(map);

            return;
        }

        if (!(payload instanceof String stringPayload)) {
            if (log.isDebugEnabled()) {
                log.debug(
                    "Dropping non-text bridge event: {}", payload == null ? "null" : payload.getClass()
                        .getSimpleName());
            }

            return;
        }

        ensureStarted();

        TextMessageContentEvent event = new TextMessageContentEvent();

        event.setMessageId(messageId);
        event.setDelta(stringPayload);

        dispatch(event);

        assistantTextBuilder.append(stringPayload);
    }

    @Override
    public void onComplete() {
        if (!markCompleted()) {
            return;
        }

        if (started.get()) {
            TextMessageEndEvent end = new TextMessageEndEvent();

            end.setMessageId(messageId);

            dispatch(end);
        }

        RunFinishedEvent finished = new RunFinishedEvent();

        finished.setThreadId(threadId);
        finished.setRunId(runId);

        dispatch(finished);

        runFinalize();
    }

    @Override
    public void onError(Throwable throwable) {
        if (!markCompleted()) {
            return;
        }

        try {
            onStreamError.run();
        } catch (RuntimeException exception) {
            log.warn("AgUiStreamBridge.onStreamError callback threw for thread {}", threadId, exception);
        }

        RunErrorEvent runErrorEvent = new RunErrorEvent();

        runErrorEvent.setError(Objects.toString(throwable.getMessage(), "Workflow execution failed"));

        dispatch(runErrorEvent);

        runFinalize();
    }

    /**
     * Flips the bridge to completed under {@link #dispatchLock}, so it waits for an event dispatch in progress and no
     * event dispatch starts after it. Returns {@code false} when the bridge was already completed.
     */
    private boolean markCompleted() {
        synchronized (dispatchLock) {
            return completed.compareAndSet(false, true);
        }
    }

    private void onMapEvent(Map<?, ?> map) {
        if (registerJobIdFromStartEvent(map)) {
            return;
        }

        if ("task_started".equals(map.get("event"))) {
            if (map.get("payload") instanceof Map<?, ?> payloadMap) {
                emitTaskStartedToolCall(payloadMap);
            }

            return;
        }

        if ("result".equals(map.get("event"))) {
            emitResult(map);

            return;
        }

        if (map.containsKey("ask_user_question") || map.containsKey("questions")) {
            onAskUserQuestion(map);

            return;
        }

        if (emitAiAgentSseEvent(map)) {
            return;
        }

        emitCustomEvent(WORKFLOW_EVENT_NAME, map);
    }

    private void onAskUserQuestion(Map<?, ?> map) {
        if (map.get("resumeUrl") instanceof String resumeUrl && !resumeUrl.isBlank()) {
            resumeRegistry.register(threadId, resumeUrl);
        } else {
            log.warn("ask_user_question event without a resumeUrl for thread {}; the answer cannot resume the run",
                threadId);
        }

        emitCustomEvent(ASK_WORKFLOW_QUESTION_EVENT_NAME, map);
    }

    private void emitResult(Map<?, ?> map) {
        if (assistantTextBuilder.length() > 0 || !(map.get("result") instanceof Map<?, ?> resultMap)
            || !(resultMap.get("message") instanceof String message) || message.isBlank()) {

            return;
        }

        ensureStarted();

        TextMessageContentEvent resultEvent = new TextMessageContentEvent();

        resultEvent.setMessageId(messageId);
        resultEvent.setDelta(message);

        dispatch(resultEvent);

        assistantTextBuilder.append(message);
    }

    /**
     * Reports the {@code jobId} of a {@code {event=start, payload.jobId=...}} event to {@code onJobId}, at most once
     * per bridge. The id is captured under {@link #dispatchLock} and handed to {@code onJobId} after the lock is
     * released. Returns {@code true} when the event was a start event (handled, not surfaced to the client),
     * {@code false} otherwise.
     */
    private boolean registerJobIdFromStartEvent(Map<?, ?> map) {
        if (!("start".equals(map.get("event")) && map.get("payload") instanceof Map<?, ?> payloadMap)) {
            return false;
        }

        Object jobIdObject = payloadMap.get("jobId");

        if (jobIdObject == null) {
            return true;
        }

        long jobId;

        try {
            jobId = jobIdObject instanceof Number jobIdNumber
                ? jobIdNumber.longValue()
                : Long.parseLong(jobIdObject.toString());
        } catch (NumberFormatException exception) {
            if (log.isDebugEnabled()) {
                log.debug("Could not parse jobId from start event for thread {}: {}", threadId, jobIdObject);
            }

            return true;
        }

        if (!jobIdReported.compareAndSet(false, true)) {
            return true;
        }

        pendingJobId = jobId;

        return true;
    }

    /**
     * Passes an AI Agent SSE event (keyed by {@link AiAgentSseEventType#EVENT_TYPE}) through as a CustomEvent named
     * after the event type, without the type key itself. Returns {@code true} when the map carried an AI Agent SSE
     * event type, {@code false} otherwise.
     */
    private boolean emitAiAgentSseEvent(Map<?, ?> map) {
        if (!(map.get(AiAgentSseEventType.EVENT_TYPE) instanceof String eventType)) {
            return false;
        }

        Map<String, Object> eventData = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!AiAgentSseEventType.EVENT_TYPE.equals(entry.getKey())) {
                eventData.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }

        emitCustomEvent(eventType, eventData);

        return true;
    }

    /**
     * Emits an AG-UI tool-call start/end pair for an enriched {@code task_started} payload
     * ({@code {taskExecutionId, name, type}}), labelled with the task name (falling back to its type). Payloads without
     * a usable label are dropped. The start event carries {@code parentMessageId}, because the client's schema rejects
     * an explicit null there.
     */
    private void emitTaskStartedToolCall(Map<?, ?> payloadMap) {
        Object taskExecutionId = payloadMap.get("taskExecutionId");

        if (taskExecutionId == null) {
            return;
        }

        String label = payloadMap.get("name") instanceof String name && !name.isBlank()
            ? name
            : Objects.toString(payloadMap.get("type"), null);

        if (label == null) {
            return;
        }

        String toolCallId = "task-" + taskExecutionId;

        ToolCallStartEvent toolCallStartEvent = new ToolCallStartEvent();

        toolCallStartEvent.setToolCallId(toolCallId);
        toolCallStartEvent.setToolCallName(label);
        toolCallStartEvent.setParentMessageId(messageId);

        dispatch(toolCallStartEvent);

        ToolCallEndEvent toolCallEndEvent = new ToolCallEndEvent();

        toolCallEndEvent.setToolCallId(toolCallId);

        dispatch(toolCallEndEvent);
    }

    /**
     * Best-effort invocation of the finalize callback. A throwing callback is logged and swallowed, since the terminal
     * event has already been emitted.
     */
    private void runFinalize() {
        try {
            onFinalize.run();
        } catch (RuntimeException exception) {
            log.warn(
                "AgUiStreamBridge.onFinalize callback threw for thread {}; SSE may stay open until client "
                    + "disconnects or the emitter times out",
                threadId, exception);
        }
    }

    private void emitCustomEvent(String name, Object value) {
        CustomEvent event = new CustomEvent(name, value);

        dispatch(event);
    }

    private void ensureStarted() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        TextMessageStartEvent start = new TextMessageStartEvent();

        start.setMessageId(messageId);
        start.setRole("assistant");

        dispatch(start);
    }

    /**
     * Dispatches a {@link BaseEvent} through the catch-all {@code subscriber.onEvent(...)} hook first, which is the one
     * that reaches the SSE stream, and then through the type-specific hook for subscribers that listen to a specific
     * event type.
     */
    private void dispatch(BaseEvent event) {
        subscriber.onEvent(event);

        if (event instanceof RunFinishedEvent finished) {
            subscriber.onRunFinishedEvent(finished);
        } else if (event instanceof RunErrorEvent runErrorEvent) {
            subscriber.onRunErrorEvent(runErrorEvent);
        } else if (event instanceof TextMessageStartEvent start) {
            subscriber.onTextMessageStartEvent(start);
        } else if (event instanceof TextMessageContentEvent content) {
            subscriber.onTextMessageContentEvent(content);
        } else if (event instanceof TextMessageEndEvent end) {
            subscriber.onTextMessageEndEvent(end);
        } else if (event instanceof CustomEvent custom) {
            subscriber.onCustomEvent(custom);
        }
    }
}
