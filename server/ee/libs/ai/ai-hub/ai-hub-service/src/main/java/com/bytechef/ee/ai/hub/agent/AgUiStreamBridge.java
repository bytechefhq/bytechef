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
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.job.sync.SseStreamBridge;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link SseStreamBridge} implementation that translates webhook workflow events into AG-UI subscriber events. Used by
 * {@link WebhookBridgeAgent} to bridge the existing webhook execution stream into the AG-UI protocol the AI Hub client
 * expects.
 *
 * <p>
 * Translation table:
 * </p>
 * <ul>
 * <li><b>{@code onEvent(String chunk)}</b> — emit {@link TextMessageContentEvent} (delta). First chunk also fires
 * {@link TextMessageStartEvent} so the client knows a new message is starting.</li>
 * <li><b>{@code onEvent(Map ask_user_question)}</b> — translate to {@link CustomEvent} with name
 * {@code "ask-workflow-question"}, value carries the questions array and the resume URL. Client renders an inline
 * follow-up form (mirrors the existing {@code request-connection} custom-event pattern).</li>
 * <li><b>{@code onEvent(Map &lt;ai-agent-sse-event-type&gt;)}</b> — pass through as a {@link CustomEvent} preserving
 * the event-type key so AI Agent SSE events survive the bridge intact. {@code approval_request} events additionally
 * fold a markdown form-link marker into the accumulated assistant text, so a reloaded conversation still surfaces the
 * pending approval even though the inline card itself is client-only.</li>
 * <li><b>{@code onEvent(Map other)}</b> — forwarded as a generic {@code CustomEvent("workflow-event", map)}; the client
 * decides whether to render or ignore.</li>
 * <li><b>{@code onComplete()}</b> — emit {@link TextMessageEndEvent} (when a stream was started) followed by
 * {@link RunFinishedEvent}.</li>
 * <li><b>{@code onError(Throwable)}</b> — emit {@link RunErrorEvent} with the throwable's message.</li>
 * </ul>
 *
 * <p>
 * <b>Idempotency:</b> {@code onComplete()} and {@code onError(...)} both flip an internal {@code completed} flag and
 * skip duplicate emissions — the underlying webhook executor's whenComplete chain (in
 * {@link com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor#executeStreaming}) can deliver both a
 * stream-level error AND a future-completion error under some failure modes; without this guard the subscriber would
 * see {@code RUN_ERROR} twice.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AgUiStreamBridge implements SseStreamBridge {

    private static final Logger log = LoggerFactory.getLogger(AgUiStreamBridge.class);

    /**
     * Custom event name the client renders as an inline follow-up question form when a webhook trigger emits
     * {@code ask_user_question}. The client subscriber registry keys off this exact string.
     */
    public static final String ASK_WORKFLOW_QUESTION_EVENT_NAME = "ask-workflow-question";

    /**
     * Generic fallback custom event name for webhook stream payloads that don't match a known shape. Keeps the bridge
     * non-lossy: even unrecognised events surface to the client, which can choose to ignore them.
     */
    public static final String WORKFLOW_EVENT_NAME = "workflow-event";

    private final AgentSubscriber subscriber;
    private final String messageId;
    private final String threadId;
    private final String runId;
    private final long taskId;
    private final WebhookResumeRegistry resumeRegistry;
    private final WorkflowChatJobRegistry jobRegistry;
    private final WorkflowChatGuard guard;
    @Nullable
    private final JobFacade jobFacade;
    private final Runnable onFinalize;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean completed = new AtomicBoolean(false);

    /**
     * Accumulates the assistant text emitted across all {@code onEvent(String)} chunks. The streaming path doesn't have
     * a single "final assistant message" the way the sync path does — the assistant reply is the concatenation of every
     * per-token delta. Capturing it here lets {@link WebhookBridgeAgent} persist the full reply to the session store
     * when the run finalizes, so reloading the task later renders the assistant turn the same way it would for a
     * non-streaming chat. Synchronization is via the {@code completed} AtomicBoolean — chunks arrive on a single
     * message-broker thread before completion so a plain StringBuilder is safe; a concurrent appender would force a
     * more elaborate ConcurrentLinkedQueue + drain.
     */
    private final StringBuilder assistantTextBuilder = new StringBuilder();

    /**
     * Whether at least one text-message event was emitted through this bridge — useful to the sync chat path
     * ({@link WebhookBridgeAgent#executeSync}) for distinguishing "streaming task already delivered the assistant
     * reply" from "workflow finished without producing any chat output". Without this signal, the sync handler can't
     * tell the difference between a streaming AI agent that emitted tokens and a no-output workflow, so it would either
     * double-emit (appending a synthetic message after the streamed text) or stay silent (suppressing the fallback hint
     * when there genuinely is no reply).
     */
    public boolean hasStarted() {
        return started.get();
    }

    /**
     * Returns the assistant text accumulated from streamed chunks, or null when nothing was streamed. Called from
     * {@link WebhookBridgeAgent}'s onFinalize closure to persist the full assistant reply to chat-memory after a
     * streaming run finishes — without this hook, workflow-chat history would only contain the user turn and the chat
     * would appear empty after a page reload (the bridge skips the LLM agent's ChatMemory advisor pipeline, so the
     * assistant text needs an explicit write).
     */
    @Nullable
    public String getAccumulatedAssistantText() {
        if (assistantTextBuilder.length() == 0) {
            return null;
        }

        return assistantTextBuilder.toString();
    }

    /**
     * @param taskId         the persistent {@code ai_hub_task.id} for the workflow chat — used as the key when
     *                       registering a pending resume URL on {@code ask_user_question} events.
     *                       {@link WebhookBridgeAgent} reads this same key on the next turn to decide whether to start
     *                       a fresh workflow run or POST to the registered resume URL.
     * @param resumeRegistry the application-scoped registry where ask-user-question resume URLs land.
     * @param guard          the per-task rate-limit + concurrency gate. Released on terminal events so the next turn
     *                       can be admitted; same idempotent {@code completed} flag covers double-release under racing
     *                       onComplete/onError paths.
     * @param jobFacade      optional job-cancel hook fired from {@link #onError} so a workflow that errors out (or
     *                       drops because the client disconnected) gets its in-flight job stopped server-side rather
     *                       than continuing to consume worker capacity. Nullable for tests + deployments without the
     *                       atlas-execution stack; in that case the disconnect-cancel feature is a no-op.
     * @param onFinalize     callback fired as the LAST step of {@link #onComplete} / {@link #onError}, after the
     *                       terminal {@code RUN_FINISHED} / {@code RUN_ERROR} event has been emitted. Used by
     *                       {@link WebhookBridgeAgent} to invoke {@code subscriber.onRunFinalized(...)}, which is what
     *                       actually closes the SSE emitter via {@code AgentStreamer}'s subscription model — without
     *                       it, the SSE stays open after the workflow finishes and the client UI sits in a permanent
     *                       "running" state. Idempotent because it only fires inside the {@code completed} CAS.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AgUiStreamBridge(
        AgentSubscriber subscriber, String messageId, String threadId, String runId, long taskId,
        WebhookResumeRegistry resumeRegistry, WorkflowChatJobRegistry jobRegistry, WorkflowChatGuard guard,
        @Nullable JobFacade jobFacade, Runnable onFinalize) {

        this.subscriber = subscriber;
        this.messageId = messageId;
        this.threadId = threadId;
        this.runId = runId;
        this.taskId = taskId;
        this.resumeRegistry = resumeRegistry;
        this.jobRegistry = jobRegistry;
        this.guard = guard;
        this.jobFacade = jobFacade;
        this.onFinalize = onFinalize;
    }

    @Override
    public void onEvent(Object payload) {
        if (completed.get()) {
            // Stream-after-complete is benign but worth logging at DEBUG so a flood of late events surfaces
            // when investigating a stuck task.
            if (log.isDebugEnabled()) {
                log.debug("AgUiStreamBridge dropping post-complete event: {}", payload);
            }

            return;
        }

        if (payload instanceof Map<?, ?> map) {
            // The workflow executor emits {event=start, payload.jobId=...} when a streaming run begins; capture the
            // jobId into the cancel registry. The event itself is not surfaced to the client.
            if (registerJobIdFromStartEvent(map)) {
                return;
            }

            // Enriched coordinator task_started: render the workflow step as an AG-UI tool-call chip (start +
            // immediate end — the pipeline carries no per-task completion event), so the chat shows live step
            // progress with the actual task name instead of dropping the event as an unrenderable payload.
            if ("task_started".equals(map.get("event"))) {
                if (map.get("payload") instanceof Map<?, ?> payloadMap) {
                    emitTaskStartedToolCall(payloadMap);
                }

                return;
            }

            // The coordinator emits {event=result, result={message=...}} when the job completes, carrying the
            // final chat reply read back from the WEBHOOK_RESPONSE-tagged task. Streaming runs have already
            // delivered the same text as deltas — appending it again would double the message — so the result
            // only renders when nothing has been streamed yet: the approval-routed path for chat workflows
            // without a streaming task, which previously lost their final reply entirely.
            if ("result".equals(map.get("event"))) {
                if (assistantTextBuilder.length() == 0
                    && map.get("result") instanceof Map<?, ?> resultMap
                    && resultMap.get("message") instanceof String message && !message.isBlank()) {

                    ensureStarted();

                    TextMessageContentEvent resultEvent = new TextMessageContentEvent();

                    resultEvent.setMessageId(messageId);
                    resultEvent.setDelta(message);

                    dispatch(resultEvent);

                    assistantTextBuilder.append(message);
                }

                return;
            }

            // ask_user_question is the workflow's pause-for-input signal. Translate to a CustomEvent the client's
            // data-event handler renders inline. Server-side, capture the resumeUrl in the registry so the next
            // user turn POSTs the answer to it (instead of starting a fresh workflow run via the bridge).
            if (map.containsKey("ask_user_question") || map.containsKey("questions")) {
                Object rawResumeUrl = map.get("resumeUrl");

                if (rawResumeUrl instanceof String resumeUrl && !resumeUrl.isBlank()) {
                    resumeRegistry.register(taskId, resumeUrl);
                } else if (log.isDebugEnabled()) {
                    // Workflows that emit ask_user_question without a resumeUrl can't be resumed via this bridge —
                    // the question still renders client-side, but the user's answer will start a fresh execution
                    // rather than resuming. Log at DEBUG so it's discoverable when investigating.
                    log.debug(
                        "ask_user_question event without a resumeUrl for task {}; resume not possible",
                        taskId);
                }

                emitCustomEvent(ASK_WORKFLOW_QUESTION_EVENT_NAME, map);

                return;
            }

            // AI Agent SSE event types from the underlying workflow's AI nodes. Pass them through unchanged so the
            // client's existing AI-agent event handlers see them.
            if (emitAiAgentSseEvent(map)) {
                return;
            }

            // Unrecognised map shape — forward as a generic workflow-event so the bridge stays non-lossy.
            emitCustomEvent(WORKFLOW_EVENT_NAME, map);

            return;
        }

        // Anything non-String non-Map is dropped with a DEBUG log rather than rendered. The coordinator normally
        // emits task_started as an enriched map (handled above), but falls back to a bare Long taskExecutionId
        // when the task row can't be loaded; without this guard those Longs would land in the chat as raw numbers
        // via Objects.toString.
        if (!(payload instanceof String stringPayload)) {
            if (log.isDebugEnabled()) {
                log.debug(
                    "Dropping non-text bridge event: {}",
                    payload == null ? "null"
                        : payload.getClass()
                            .getSimpleName());
            }

            return;
        }

        // Plain text chunk — typical SSE streaming output. Emit as a delta on the (possibly newly-started)
        // assistant message.
        ensureStarted();

        TextMessageContentEvent event = new TextMessageContentEvent();

        event.setMessageId(messageId);
        event.setDelta(stringPayload);

        dispatch(event);

        // Accumulate for chat-memory persistence after the run finalizes. Done after dispatch so a render failure
        // doesn't pollute the buffer with a chunk the client never saw.
        assistantTextBuilder.append(stringPayload);
    }

    /**
     * Captures the {@code jobId} from a {@code {event=start, payload.jobId=...}} event into the cancel registry so a
     * user-initiated cancel maps back to the right {@code JobFacade.stopJob} target. Returns {@code true} when the
     * event was a start event (handled, not surfaced to the client), {@code false} otherwise.
     */
    private boolean registerJobIdFromStartEvent(Map<?, ?> map) {
        if (!("start".equals(map.get("event")) && map.get("payload") instanceof Map<?, ?> payloadMap)) {
            return false;
        }

        Object jobIdObject = payloadMap.get("jobId");

        if (jobIdObject != null) {
            try {
                long jobId = jobIdObject instanceof Number jobIdNumber
                    ? jobIdNumber.longValue()
                    : Long.parseLong(jobIdObject.toString());

                jobRegistry.register(taskId, jobId);
            } catch (NumberFormatException exception) {
                if (log.isDebugEnabled()) {
                    log.debug("Could not parse jobId from start event for task {}: {}", taskId, jobIdObject);
                }
            }
        }

        return true;
    }

    /**
     * Passes an AI Agent SSE event (keyed by {@link AiAgentSseEventType#EVENT_TYPE}) through as a CustomEvent so the
     * client's existing AI-agent handlers see it. For {@code approval_request} it also folds a persist-only markdown
     * form-link marker into the accumulated assistant text so a reloaded conversation still surfaces the pending
     * approval. Returns {@code true} when the map carried an AI Agent SSE event type, {@code false} otherwise.
     */
    private boolean emitAiAgentSseEvent(Map<?, ?> map) {
        if (!map.containsKey(AiAgentSseEventType.EVENT_TYPE)) {
            return false;
        }

        String eventType = (String) map.get(AiAgentSseEventType.EVENT_TYPE);

        Map<String, Object> eventData = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!AiAgentSseEventType.EVENT_TYPE.equals(entry.getKey())) {
                eventData.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }

        if (AiAgentSseEventType.APPROVAL_REQUEST.equals(eventType)
            && map.get("formUrl") instanceof String formUrl && !formUrl.isBlank()) {

            if (assistantTextBuilder.length() > 0) {
                assistantTextBuilder.append("\n\n");
            }

            assistantTextBuilder.append("Approval requested — [open the approval form](")
                .append(formUrl)
                .append(").");
        }

        emitCustomEvent(eventType, eventData);

        return true;
    }

    /**
     * Emits an AG-UI tool-call start/end pair for an enriched {@code task_started} payload
     * ({@code {taskExecutionId, name, type}}), labelled with the workflow task's name (falling back to its type). The
     * client's existing tool-call renderer shows it as a step chip — no dedicated client handling needed. Payloads
     * without a usable label are dropped silently.
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
        // Required even though @ag-ui/core types it as optional: z.optional(z.string()) accepts an ABSENT key but
        // rejects an explicit null, and the app ObjectMapper serializes null fields rather than omitting them.
        // Unset, this ships {"parentMessageId": null} and the browser-side Zod validator tears the whole SSE run
        // down before a single token renders. Mirrors ToolMapper on the LLM path, which sets it from the same
        // in-flight message id.
        toolCallStartEvent.setParentMessageId(messageId);

        dispatch(toolCallStartEvent);

        ToolCallEndEvent toolCallEndEvent = new ToolCallEndEvent();

        toolCallEndEvent.setToolCallId(toolCallId);

        dispatch(toolCallEndEvent);
    }

    @Override
    public void onComplete() {
        if (!completed.compareAndSet(false, true)) {
            return;
        }

        // Clear any in-flight jobId for this task — the workflow run is done so a cancel attempt now
        // would fail (or worse, cancel the next turn's job if one races into the same slot before the cache
        // entry naturally expires). Idempotent so onComplete-after-onError or duplicate completion are safe.
        jobRegistry.clear(taskId);

        // Release the per-task concurrency lock so the next turn can be admitted. Sequence matters:
        // job registry cleared first (so any racing cancel attempt sees no work to cancel), THEN the lock is
        // freed (so a follow-up turn can land).
        guard.release(taskId);

        if (started.get()) {
            TextMessageEndEvent end = new TextMessageEndEvent();

            end.setMessageId(messageId);

            dispatch(end);
        }

        RunFinishedEvent finished = new RunFinishedEvent();

        finished.setThreadId(threadId);
        finished.setRunId(runId);

        dispatch(finished);

        // onRunFinalized is the AgentSubscriber lifecycle hook AgentStreamer subscribes to in order to close the
        // SSE emitter — emitting RUN_FINISHED alone leaves the channel open. Best-effort: a thrown finalize closure
        // must NOT prevent the bridge's terminal cleanup, so we swallow + log instead of letting it bubble.
        runFinalize();
    }

    @Override
    public void onError(Throwable throwable) {
        if (!completed.compareAndSet(false, true)) {
            return;
        }

        // Read-then-clear-then-cancel: the read pulls the jobId so we can fire stopJob below; the clear
        // happens BEFORE the stopJob call so a racing turn that lands between them sees an empty slot rather
        // than the stale entry we're about to invalidate. stopJob itself is idempotent, so the rare race
        // where the cancel mutation fires concurrently is benign.
        Long inFlightJobId = jobRegistry.get(taskId);

        jobRegistry.clear(taskId);

        // Same release-after-clear ordering as onComplete — without this a workflow that crashes mid-run would
        // leave the task locked until the cache TTL expires (30 minutes), giving the user a confusing
        // "previous turn still running" error long after the actual previous turn died.
        guard.release(taskId);

        // WC #5b auto-cancel-on-disconnect: when the client drops mid-stream the bridge sees an emit failure
        // that surfaces here. Without an explicit stopJob the workflow keeps running and burns worker capacity
        // for output nobody will read. Fire stopJob unconditionally — for a workflow that errored out
        // server-side the job is already terminal and stopJob is a no-op; for a client-disconnect error the
        // cancel actually frees the worker.
        if (inFlightJobId != null && jobFacade != null) {
            try {
                jobFacade.stopJob(inFlightJobId);
            } catch (RuntimeException exception) {
                // Cancel is best-effort. A failure here (job already finished, coordinator transient outage)
                // shouldn't propagate further — the user's RUN_ERROR event still fires below regardless.
                if (log.isDebugEnabled()) {
                    log.debug(
                        "stopJob for jobId {} (task {}) failed during onError handling",
                        inFlightJobId, taskId, exception);
                }
            }
        }

        RunErrorEvent err = new RunErrorEvent();

        err.setError(Objects.toString(throwable.getMessage(), "Workflow execution failed"));

        dispatch(err);

        // Same SSE-closing concern as onComplete — RUN_ERROR alone doesn't release the AgentStreamer subscription;
        // only onRunFinalized does. Fire here so an errored workflow doesn't leave the client UI sitting in a
        // permanent "running" state after the error toast has surfaced.
        runFinalize();
    }

    /**
     * Best-effort invocation of the finalize closure supplied by {@link WebhookBridgeAgent}. Swallows any throwable —
     * the bridge has already emitted its terminal lifecycle event and released the task guard at this point, so a
     * thrown finalizer must not prevent that work from being seen as committed.
     */
    private void runFinalize() {
        try {
            onFinalize.run();
        } catch (RuntimeException exception) {
            log.warn(
                "AgUiStreamBridge.onFinalize callback threw for task {}; SSE may stay open until client "
                    + "disconnects or the emitter times out",
                taskId, exception);
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
        // Role is REQUIRED on the wire. The client's Zod schema validates it as a literal-union of
        // "developer" | "system" | "assistant" | "user" — leaving it null fails validation with
        // `invalid_literal: expected "assistant"` and the entire run is rejected client-side. SpringAIAgent
        // and LangchainAgent both stamp "assistant" on their text message start events; the bridge mirrors
        // that since workflow-chat replies are always from the workflow's assistant-perspective output.
        start.setRole("assistant");

        dispatch(start);
    }

    /**
     * Dispatches a {@link BaseEvent} through both the catch-all {@code subscriber.onEvent(...)} hook AND the
     * type-specific hook ({@code onRunFinishedEvent}, {@code onTextMessageContentEvent}, etc.). Mirrors what
     * {@code LocalAgent.emitEvent} does in the framework's typical agent path — and is the missing piece that caused
     * workflow-chat events to never reach the SSE channel.
     *
     * <p>
     * The reason: {@code AgentStreamer} (which wires the SseEmitter) only overrides three methods on the
     * {@link AgentSubscriber} interface — {@code onEvent}, {@code onRunFinalized}, and {@code onRunFailed}. Every other
     * method (including {@code onRunFinishedEvent}, {@code onTextMessageContentEvent}, etc.) uses the empty default
     * impl on the interface. So calling those typed methods directly bypasses the SSE stream entirely — events fire but
     * reach nowhere observable. {@code subscriber.onEvent(event)} is the one method that pushes events onto the
     * {@code EventStream} and out to the client.
     * </p>
     *
     * <p>
     * Order matters: {@code onEvent} fires FIRST so the SSE stream sees every event in order, then the typed dispatch
     * fires for any subscribers that hook into a specific event type (none today on the AgUiService path, but useful
     * for tests + future subscribers).
     * </p>
     */
    private void dispatch(BaseEvent event) {
        subscriber.onEvent(event);

        switch (event) {
            case RunFinishedEvent finished -> subscriber.onRunFinishedEvent(finished);
            case RunErrorEvent err -> subscriber.onRunErrorEvent(err);
            case TextMessageStartEvent start -> subscriber.onTextMessageStartEvent(start);
            case TextMessageContentEvent content -> subscriber.onTextMessageContentEvent(content);
            case TextMessageEndEvent end -> subscriber.onTextMessageEndEvent(end);
            case CustomEvent custom -> subscriber.onCustomEvent(custom);
            // Other event types — bridge doesn't emit them today, but the catch-all `onEvent` above still
            // fired for them, so they reach the SSE stream regardless. No work to do here.
            default -> {
            }
        }
    }
}
