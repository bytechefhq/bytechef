/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.event.RunStartedEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.AssistantMessage;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.Role;
import com.agui.server.LocalAgent;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.metric.WorkflowChatMetrics;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import tools.jackson.databind.json.JsonMapper;

/**
 * AG-UI {@link LocalAgent} that routes per-turn invocations of {@code kind = WORKFLOW_CHAT} chats through the webhook
 * executor instead of the LLM. Built on top of {@link WebhookWorkflowExecutor} so the existing HTTP/SSE controller and
 * this bridge share the same orchestration logic.
 *
 * <p>
 * Per-turn flow:
 * </p>
 * <ol>
 * <li>Look up the {@link AiHubChat} for {@code input.threadId()}. Verify it's a {@code kind = WORKFLOW_CHAT} row; any
 * non-workflow-chat row reaching this agent is a routing bug — surface as a RUN_ERROR rather than silently delegating,
 * since the user would otherwise see no visible failure.</li>
 * <li>Extract the user's last message from {@code input.messages()} and build a {@link WebhookRequest} with it. The
 * webhook trigger reads the {@code message} parameter to drive the workflow's first step.</li>
 * <li>Read {@link WebhookTriggerFlags#workflowSyncExecution()} from the executor. Sync triggers go through
 * {@link WebhookWorkflowExecutor#executeSync}; streaming triggers through
 * {@link WebhookWorkflowExecutor#executeStreaming} with an {@link AgUiStreamBridge}.</li>
 * <li>Either path culminates in a {@code RUN_FINISHED} or {@code RUN_ERROR} event on the AG-UI subscriber, mirroring
 * what the LLM agent path emits — so the client's runtime provider treats both flavours of chats identically.</li>
 * </ol>
 *
 * <p>
 * <b>Attachments:</b> the bridge reads attachment metadata from {@code RunAgentInput.forwardedProps} under the
 * {@code attachments} key — see {@link #buildWebhookRequest} for the contract. Each {@code {name, contentType, base64}}
 * entry is decoded, persisted as a first-class workspace asset file via {@link AssetFileFacade#createFromUpload}, and
 * wrapped in {@link BridgedFileEntry} (which implements the SDK {@link com.bytechef.component.definition.FileEntry}
 * interface) so {@code ChatNewRequestTrigger} sees real file entries — its {@code list.getFirst() instanceof FileEntry}
 * discriminator picks the file-list branch the same way as the legacy multipart upload path. Promoting through
 * {@link AssetFileFacade} (rather than a transient temp scope) means a file dropped in either flavour of chat becomes
 * browsable in the Files panel and survives across the chat lifecycle, matching how the rest of the CC surface treats
 * user-supplied content.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WebhookBridgeAgent extends LocalAgent {

    private static final Logger log = LoggerFactory.getLogger(WebhookBridgeAgent.class);

    /**
     * Bound the resume POST so a misbehaving workflow that holds the connection open doesn't pin a chat turn forever.
     * Long enough that a normal "wait for the workflow's reply step to render an answer" finishes; short enough that
     * the user gets a recognisable failure rather than the spinning-forever experience.
     */
    private static final Duration RESUME_REQUEST_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Maximum decoded size for a single workflow-chat attachment. 25 MB is comfortably above any normal user upload
     * (images, PDFs, transcripts) but small enough that a hostile or buggy client base64-attaching a gigabyte file
     * can't OOM the ai-hub service before {@link AssetFileFacade#createFromUpload}'s workspace- quota check fires. Hard
     * cap rather than a configurable limit because the bridge is hot-path code: any legitimate workflow that needs >25
     * MB inputs should use direct asset-file upload, not the inline base64 channel.
     */
    private static final int MAX_ATTACHMENT_BYTES = 25 * 1024 * 1024;

    /**
     * The bridge body shape ({@code body.content = {message, chatId, attachments}}) is calibrated for the canonical
     * chat trigger ({@code newChatRequest}). Other webhook triggers wired to a workflow chat will see this shape
     * regardless — generic triggers reading from {@code parameters} still work since the same data lands there too, but
     * a trigger expecting a different body shape needs adapter logic that doesn't exist yet. We tag every turn with the
     * trigger name so ops can spot non-chat triggers being used.
     */
    private static final String CHAT_TRIGGER_NAME = "newChatRequest";

    private final WebhookWorkflowExecutor webhookWorkflowExecutor;
    private final AiHubChatService chatService;
    private final WebhookResumeRegistry resumeRegistry;
    private final HttpClient resumeHttpClient;
    private final JsonMapper jsonMapper;
    private final AssetFileFacade assetFileFacade;
    private final WorkflowChatMetrics metrics;
    private final WorkflowChatJobRegistry jobRegistry;
    private final AiHubSessionMemory sessionMemory;
    private final WorkflowChatGuard guard;
    @Nullable
    private final JobFacade jobFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WebhookBridgeAgent(
        WebhookWorkflowExecutor webhookWorkflowExecutor, AiHubChatService chatService,
        WebhookResumeRegistry resumeRegistry, JsonMapper jsonMapper, AssetFileFacade assetFileFacade,
        WorkflowChatMetrics metrics, WorkflowChatJobRegistry jobRegistry, AiHubSessionMemory sessionMemory,
        WorkflowChatGuard guard, @Nullable JobFacade jobFacade) throws AGUIException {

        // LocalAgent's constructor demands a non-null systemMessage OR systemMessageProvider; the bridge has no
        // LLM and uses neither, but a placeholder satisfies the constructor invariant. The placeholder is never
        // sent anywhere — `run()` bypasses LocalAgent's chat-completion path entirely.
        super(
            "webhook-bridge-agent", null, null, "(unused — webhook bridge bypasses LLM)", List.of());

        this.webhookWorkflowExecutor = webhookWorkflowExecutor;
        this.chatService = chatService;
        this.resumeRegistry = resumeRegistry;
        this.jsonMapper = jsonMapper;
        this.assetFileFacade = assetFileFacade;
        this.metrics = metrics;
        this.jobRegistry = jobRegistry;
        this.sessionMemory = sessionMemory;
        this.guard = guard;
        this.jobFacade = jobFacade;
        this.resumeHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    /**
     * Binds the caller's tenant for the whole bridged run before any of it executes.
     *
     * <p>
     * This agent has the same defect as {@link AiHubSpringAIAgent} for the same reason and needs the same fix: it
     * extends {@code LocalAgent} directly, {@code AiHubRoutingAgent} routes every webhook-bridged chat to it, and
     * {@code LocalAgent.runAgent} dispatches {@link #runBridge} through a bare {@code CompletableFuture.runAsync}. The
     * very first statement of the bridged body is {@code chatService.findByThreadId(...)}, followed by chat-memory
     * writes and {@code jobFacade} calls — all of which would otherwise run against the {@code public} schema. The
     * router forwards the same {@code RunAgentParameters} the controller built, so the verified tenant id is present in
     * {@code input.state()} here exactly as it is for the LLM agent.
     */
    @Override
    protected void run(RunAgentInput input, AgentSubscriber subscriber) {
        AiHubAgentTenantBinder.runWithTenant(getAgentId(), input.state(), () -> runBridge(input, subscriber));
    }

    @SuppressWarnings("checkstyle:methodlength")
    private void runBridge(RunAgentInput input, AgentSubscriber subscriber) {
        // RUN_STARTED MUST be the first event the client sees — the AG-UI runtime's Zod schema validates the
        // SSE event stream and rejects the whole run with "First event must be 'RUN_STARTED'" if anything
        // else arrives first. Mirroring SpringAIAgent / LangchainAgent which fire this immediately after
        // entering run(). Emitting before any validation so even early-exit error paths (emitError below)
        // produce a valid event sequence: RUN_STARTED, then RUN_ERROR, then RUN_FINALIZED.
        emitRunStarted(input, subscriber);

        Optional<AiHubChat> chatOptional = chatService.findByThreadId(input.threadId());

        if (chatOptional.isEmpty()) {
            emitError(input, subscriber, "AiHubChat not found for thread " + input.threadId());

            return;
        }

        AiHubChat chat = chatOptional.get();

        if (!chat.getKind()
            .isWebhookBridged()) {
            // The router upstream should have dispatched this to the LLM agent — landing here means a routing
            // bug. Surface as RUN_ERROR so the client doesn't sit waiting for events that never arrive. The log line
            // is the breadcrumb for ops to track down the misroute.
            log.warn(
                "WebhookBridgeAgent invoked for a chat that is not webhook-bridged: {} (kind={}). Misroute.",
                chat.getId(), chat.getKind());

            emitError(input, subscriber,
                "AiHubChat kind mismatch: bridge agent invoked for a non-workflow chat");

            return;
        }

        String workflowExecutionIdStr = chat.getWorkflowExecutionId();

        if (workflowExecutionIdStr == null || workflowExecutionIdStr.isBlank()) {
            // Defensive: WORKFLOW_CHAT rows MUST have workflowExecutionId — the create path always stamps it. A
            // null/blank value indicates a kind/payload mismatch (e.g. a row mis-tagged as WORKFLOW_CHAT). Treat
            // as a recoverable error rather than NPE.
            emitError(input, subscriber, "Workflow chat is missing workflowExecutionId");

            return;
        }

        WorkflowExecutionId workflowExecutionId;

        try {
            workflowExecutionId = WorkflowExecutionId.parse(workflowExecutionIdStr);
        } catch (RuntimeException exception) {
            emitError(
                input, subscriber,
                "Failed to parse workflow execution id: " + workflowExecutionIdStr + " — " + exception.getMessage());

            return;
        }

        String userMessage = lastUserMessage(input.messages());

        if (userMessage == null) {
            emitError(input, subscriber, "No user message found in agent input");

            return;
        }

        if (!tryAdmitOrEmitError(input, chat, subscriber)) {
            return;
        }

        String messageId = UUID.randomUUID()
            .toString();

        // Resume short-circuit: a previous turn paused the workflow at `ask_user_question` and AgUiStreamBridge
        // captured the resume URL into the registry. The atomic `consume()` removes-and-returns so the next-next
        // turn (if any) starts a fresh execution rather than re-resuming with a stale URL.
        String pendingResumeUrl = resumeRegistry.consume(chat.getId());

        if (pendingResumeUrl != null) {
            recordTurn("resume", chat);

            handleResume(
                input, pendingResumeUrl, userMessage, chat, subscriber, messageId, input.threadId(),
                input.runId(), input.forwardedProps());

            return;
        }

        WebhookTriggerFlags flags = lookupTriggerFlagsOrEmitError(
            input, workflowExecutionId, workflowExecutionIdStr, chat, subscriber);

        if (flags == null) {
            return;
        }

        WebhookRequest webhookRequest = buildWebhookRequest(userMessage, chat, input.forwardedProps());

        // Capture into final references so the lambda can read them — needed because the bridge's onFinalize
        // runs asynchronously after the streaming run completes. The streamingPathRef flag is set by the routing
        // decision below; the closure reads it to know whether to persist chat-memory itself (streaming path: yes)
        // or skip because handleSyncOutputs already did (sync path: no).
        String capturedUserMessage = userMessage;
        String capturedThreadId = chat.getThreadId();
        AgUiStreamBridge[] bridgeRef = new AgUiStreamBridge[1];
        boolean[] streamingPathRef = new boolean[1];

        // The onFinalize closure fires subscriber.onRunFinalized — required to close the SSE channel via
        // AgentStreamer's subscription model. Without it, RUN_FINISHED / RUN_ERROR events reach the client but
        // the SSE emitter stays open, leaving the UI stuck in a "running" state after the workflow completes.
        // SpringAIAgent fires the equivalent call directly at the end of its run; for the bridge we do it inside
        // onComplete/onError so all completion paths (sync, streaming whenComplete, error) get it uniformly.
        //
        // Chat-memory persistence rides on the same closure — for streaming runs the assistant reply isn't a
        // single "final message" but the concatenation of streamed chunks accumulated by the bridge. Persisting
        // here means workflow-chat history reloads correctly after a page refresh, same as for non-streaming
        // sync runs. The sync path already persists in handleSyncOutputs; this branch covers the streaming +
        // resume paths where handleSyncOutputs is never called. Idempotency-wise, persistTurnToChatMemory is
        // safe to call twice on the same thread id (Spring AI's chat-memory schema allows duplicate inserts of
        // the same content with distinct timestamps), but we guard against it by only persisting from this hook
        // when the sync path didn't already write — handleSyncOutputs sets a sentinel via
        // bridge.hasStarted() to signal it has streamed text, otherwise the sync writes its own assistant text.
        Runnable onFinalize = () -> {
            try {
                AgUiStreamBridge currentBridge = bridgeRef[0];

                // Streaming/resume paths: handleSyncOutputs never ran, so chat-memory hasn't been written. Persist
                // the user turn regardless of whether the assistant produced text — the chat history needs
                // the user's message even when the workflow errors before producing output. If chunks were
                // streamed, fold the accumulated assistant text into the same persist call so reloading the
                // chat renders the assistant turn the same way it would for a sync chat reply.
                //
                // Skip when handleSyncOutputs already persisted (sync path with extractable message): that path
                // calls persistTurnToChatMemory itself and we don't want a duplicate user turn. The signal is
                // bridge.hasStarted() — the sync path emits via bridge.onEvent for any extracted message, which
                // flips started to true; the streaming path also flips started when the first chunk arrives.
                // Both paths produce a "did write" signal via the same flag, so we can't disambiguate "sync wrote"
                // from "streaming wrote" here. Resolution: handleSyncOutputs is the only caller that writes
                // BEFORE finalize, and it persists user + assistant atomically. By the time onFinalize runs,
                // chat-memory already has the sync turn. Re-persisting from onFinalize would duplicate the user
                // entry. So we use a separate sentinel: only persist from onFinalize if we're on the streaming
                // path (no handleSyncOutputs invocation to begin with) — tracked via the hasStreamingTask
                // decision captured in `streamingPath` below.
                if (currentBridge != null && streamingPathRef[0]) {
                    String assistantText = currentBridge.getAccumulatedAssistantText();

                    persistTurnToChatMemory(capturedThreadId, capturedUserMessage, assistantText);
                }
            } finally {
                finalizeRun(input, subscriber);
            }
        };

        AgUiStreamBridge bridge = new AgUiStreamBridge(
            subscriber, messageId, input.threadId(), input.runId(), chat.getId(), resumeRegistry,
            jobRegistry, guard, jobFacade, onFinalize);

        bridgeRef[0] = bridge;

        // Surface non-chat triggers in telemetry so ops can spot misconfigured workflow chats. The bridge's
        // body shape is calibrated for `newChatRequest`; other triggers may not understand it, but rather than
        // refuse the turn we run it and let the trigger fail downstream — the alternative (refuse upfront)
        // breaks any custom trigger that happens to accept the chat shape.
        String triggerName = workflowExecutionId.getTriggerName();

        if (log.isDebugEnabled() && !CHAT_TRIGGER_NAME.equals(triggerName)) {
            log.debug(
                "Workflow chat {} runs against non-chat trigger '{}'; body shape may not match.",
                chat.getId(), triggerName);
        }

        // Note: we do NOT record a WORKFLOW_EXECUTION_STARTED artifact here. The chat IS the workflow
        // execution for workflow-chat — every turn would otherwise produce one of these rows under its own
        // chat, which is circular ("here's a sidebar link to the execution that produced this very
        // sidebar entry"). The execution is still reachable from the dedicated /automation/executions page
        // for ops/debugging. The companion path — RunChatWorkflowToolCallback — keeps the artifact because
        // there the chat is a standard chat that *invoked* a workflow as a tool, so the
        // artifact links to a separate execution that's not implicit in the chat thread.

        // Routing decision: workflow-content awareness wins over the trigger flag for chat workflows.
        //
        // The chat trigger (chat/v1/newChatRequest) hard-codes workflowSyncExecution=true so the legacy
        // /webhooks/{id} HTTP transport keeps working for embedded chat clients (which expect a sync HTTP body
        // back). But that decision is wrong for streaming AI workflows wired through the AG-UI bridge: sync
        // execution registers no bridge with SseStreamBridgeRegistry, so per-token chunks from streaming tasks
        // disappear and the user sees a blank assistant reply. We override the flag here when the workflow's
        // tasks include a streaming action — single-app dev still works because executeStreaming uses the same
        // coordinator + post-processor wiring as before, and EE deployments work because SseStreamBridgeRegistry
        // is the cross-process event sink workers publish to via the message broker.
        boolean hasStreamingTask;

        try {
            // Approval tasks also force the event-bridge path: the chat approval channel publishes the card
            // event onto the run's SSE stream, which only has a listener when this turn goes through
            // webhookWorkflowExecutor.stream(...). On the sync path the card would be raised into the void.
            hasStreamingTask = webhookWorkflowExecutor.hasStreamingTask(workflowExecutionId)
                || webhookWorkflowExecutor.hasApprovalTask(workflowExecutionId);
        } catch (RuntimeException exception) {
            // Defensive: workflow lookup can fail if the workflow was deleted between the trigger-flags check
            // above and this call. Fall back to the trigger flag rather than failing the whole turn — at worst
            // the user sees a blank reply for the streaming case (which is the pre-fix behaviour anyway).
            if (log.isDebugEnabled()) {
                log.debug(
                    "hasStreamingTask lookup failed for {}; falling back to trigger flag",
                    workflowExecutionId, exception);
            }

            hasStreamingTask = false;
        }

        if (!hasStreamingTask && flags.workflowSyncExecution()) {
            // Sync path: the webhook executor returns the full result in one call. Translate the result map's
            // `message` field into a single text-message-content + complete sequence on the AG-UI subscriber.
            // Non-streaming chat workflows that emit a `chat/responseToRequest` reply land here, with the chat
            // text living under `outputs.__webhookResponse.body.message` (see extractMessage's unwrap path).
            // streamingPathRef stays false — handleSyncOutputs persists chat-memory itself, so the onFinalize
            // closure must skip persistence to avoid duplicating the user turn.
            recordTurn("sync", chat);
            executeSync(workflowExecutionId, webhookRequest, bridge, subscriber, messageId, chat, userMessage);
        } else {
            // Streaming path: events arrive on the bridge as the workflow runs, via SseStreamBridgeRegistry which
            // works across coordinator/worker process boundaries. AI agent token deltas reach the AG-UI client as
            // TextMessageContent events. The whenComplete handler in the facade closes out the stream.
            // streamingPathRef = true so onFinalize knows to persist user + accumulated assistant text to
            // chat-memory after the stream finalizes — without this, workflow-chat history would only contain
            // the user turn for streaming runs and would render as empty (no assistant message) on reload.
            streamingPathRef[0] = true;

            recordTurn("streaming", chat);
            webhookWorkflowExecutor.stream(workflowExecutionId, webhookRequest, bridge);
        }
    }

    /**
     * Records a turn in both the global counter (no workspace dimension) and the per-workspace counter (workspace tag
     * for ops drill-down). Centralised here so call sites in {@link #run} stay focused on dispatch logic. The
     * dual-record approach lets ops choose whether to slice by workspace or aggregate globally without paying the
     * cardinality cost on every counter.
     */
    private void recordTurn(String outcome, AiHubChat chat) {
        metrics.recordTurn(outcome);
        metrics.recordTurnByWorkspace(outcome, chatService.getWorkspaceId(chat.getId()));
    }

    private void executeSync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest, AgUiStreamBridge bridge,
        AgentSubscriber subscriber, String messageId, AiHubChat chat, String userMessageText) {

        try {
            // Belt-and-suspenders: the upfront isWorkflowDisabled check in run() (via lookupTriggerFlagsOrEmitError)
            // catches the disabled state in the common case, but a workflow disabled BETWEEN that check and this call
            // lands here. Re-check so a mid-turn disable surfaces the same friendly message rather than running
            // against a half-torn-down deployment, and so the sync path always terminates with onError/onComplete
            // (a silent no-op here would leave the client UI hung waiting for events that never arrive).
            if (webhookWorkflowExecutor.isWorkflowDisabled(workflowExecutionId)) {
                if (log.isDebugEnabled()) {
                    log.debug("Workflow chat disabled mid-turn for chat {}", chat.getId());
                }

                bridge.onError(new IllegalStateException(disabledChatErrorMessage(chat)));

                return;
            }

            Object outputs;

            try {
                outputs = webhookWorkflowExecutor.executeSync(workflowExecutionId, webhookRequest)
                    .join();
            } catch (CompletionException completionException) {
                Throwable cause = completionException.getCause();

                throw cause instanceof RuntimeException runtimeException
                    ? runtimeException
                    : new IllegalStateException(cause);
            }

            handleSyncOutputs(outputs, bridge, subscriber, messageId, chat, userMessageText);
        } catch (RuntimeException exception) {
            bridge.onError(exception);
        }
    }

    /**
     * Renders a user-facing error message for the disabled-workflow case. Keeps the chat title in the message so a user
     * with multiple workflow chats open knows which one is broken — "This chat is disabled" is ambiguous when several
     * chats sit in the same sidebar.
     */
    private static String disabledChatErrorMessage(AiHubChat chat) {
        String title = chat.getTitle();

        if (title != null && !title.isBlank()) {
            return "This workflow chat (\"" + title + "\") is currently disabled. Re-enable the workflow's "
                + "deployment to resume the chat.";
        }

        return "This workflow chat is currently disabled. Re-enable the workflow's deployment to resume the "
            + "chat.";
    }

    /**
     * Renders a user-facing error message for the workflow-deleted case. Distinguishes from the disabled message
     * because the recovery action is different: disabled chats can be re-enabled in place; deleted-workflow chats need
     * the user to either restore the workflow or archive the chat.
     */
    private static String deletedWorkflowErrorMessage(AiHubChat chat) {
        String title = chat.getTitle();

        if (title != null && !title.isBlank()) {
            return "This workflow chat (\"" + title + "\") can no longer be reached — the underlying workflow "
                + "has been deleted or its deployment removed. Archive this chat, or restore the "
                + "workflow to resume.";
        }

        return "This workflow chat can no longer be reached — the underlying workflow has been deleted or its "
            + "deployment removed. Archive this chat, or restore the workflow to resume.";
    }

    /**
     * Resume path: the previous turn paused the workflow at {@code ask_user_question} and the resume URL was captured
     * by {@link AgUiStreamBridge}. POST the user's answer to that URL and stream the response through a fresh
     * {@link AgUiStreamBridge} so any nested {@code ask_user_question} re-pause registers a new resume URL on the same
     * registry slot.
     *
     * <p>
     * The resume endpoint is the same workflow's webhook handler — it accepts a JSON body with a {@code message} field
     * and returns a JSON envelope shaped identically to a sync trigger response. Streaming-resume isn't a supported
     * transport on the webhook side today, so we always treat resume as sync and stream the result through the bridge
     * as one chunk.
     * </p>
     *
     * <p>
     * <b>TenantContext on the loopback:</b> the resume POST is a fresh {@link HttpClient#send} call from this thread,
     * so the request hits Spring's MVC layer with no propagated tenant context. That's intentional — the receiving
     * {@link com.bytechef.platform.webhook.web.rest.WebhookTriggerController} parses the workflow execution id out of
     * the URL path and runs the inner work under {@code TenantContext.callWithTenantId(executionId.getTenantId())}, so
     * the tenant is reconstructed from the URL itself. The bridge does NOT need to forward auth headers or tenant
     * cookies. If the URL ever stops carrying the tenant id (e.g. a non-tenanted resume token), this assumption breaks
     * and the resume body would land under {@code public} (the default tenant) — covered today by the webhook-trigger
     * integration tests' tenant-isolation assertions.
     * </p>
     */
    private void handleResume(
        RunAgentInput input, String resumeUrl, String userMessage, AiHubChat chat,
        AgentSubscriber subscriber, String messageId, String threadId, String runId,
        @Nullable Object forwardedProps) {

        AgUiStreamBridge bridge = new AgUiStreamBridge(
            subscriber, messageId, threadId, runId, chat.getId(), resumeRegistry, jobRegistry, guard,
            jobFacade, () -> finalizeRun(input, subscriber));

        try {
            String requestBody = jsonMapper.writeValueAsString(
                Map.of(
                    "message", userMessage,
                    "chatId", chat.getThreadId(),
                    "attachments", extractAttachments(forwardedProps)));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(resumeUrl))
                .timeout(RESUME_REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                // Advertise SSE so a streaming-capable resume endpoint can switch from the legacy single-blob
                // body to a line-oriented event stream. The webhook controller falls back to sync JSON when its
                // workflow doesn't emit streaming output, so this header is purely opportunistic — non-streaming
                // workflows still work unchanged.
                .header("Accept", "text/event-stream, application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

            // WC #8 streaming-resume: read the body as a line stream rather than a buffered string. For
            // text/event-stream responses we feed each SSE data event through the bridge as it arrives, giving
            // the user a streaming reply during resume rather than a single blob after the workflow finishes.
            // For any other content type we collect the lines into a single string and reuse the sync handler.
            HttpResponse<java.util.stream.Stream<String>> response = resumeHttpClient.send(
                httpRequest, HttpResponse.BodyHandlers.ofLines());

            int statusCode = response.statusCode();

            if (statusCode < 200 || statusCode >= 300) {
                metrics.recordResume("http_error");

                String errorBody = collectBodyLines(response.body());

                bridge.onError(
                    new IllegalStateException(
                        "Workflow resume failed with HTTP " + statusCode + ": " + errorBody));

                return;
            }

            String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("");

            if (contentType.startsWith("text/event-stream")) {
                metrics.recordResume("success");

                streamResumeBody(response.body(), bridge);
                handleResumeStreamComplete(bridge, chat, userMessage);

                return;
            }

            metrics.recordResume("success");

            // Non-streaming resume: collect the body into a single string and reuse the sync handler. Same path
            // every workflow chat used before the streaming-resume work landed; preserved verbatim so non-SSE
            // resumes regress to no-change.
            String body = collectBodyLines(response.body());
            Object outputs = parseResponseBody(body);

            handleSyncOutputs(outputs, bridge, subscriber, messageId, chat, userMessage);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            metrics.recordResume("transport_error");

            bridge.onError(interruptedException);
        } catch (RuntimeException | java.io.IOException exception) {
            log.warn("Workflow resume to {} failed for chat {}", resumeUrl, chat.getId(), exception);

            metrics.recordResume("transport_error");

            bridge.onError(exception);
        }
    }

    /**
     * Drains a line stream into a single newline-joined string. Used by the resume non-streaming fallback (the full
     * body was originally fetched via {@code BodyHandlers.ofString} but switching to {@code ofLines} for streaming
     * support means non-streaming responses arrive as a stream too).
     */
    private static String collectBodyLines(java.util.stream.Stream<String> lines) {
        StringBuilder builder = new StringBuilder();

        try (java.util.stream.Stream<String> stream = lines) {
            stream.forEach(line -> {
                if (builder.length() > 0) {
                    builder.append('\n');
                }

                builder.append(line);
            });
        }

        return builder.toString();
    }

    /**
     * Streams an SSE-shaped resume response through the bridge. Each {@code data: ...} line is parsed: JSON payloads
     * become {@code Map} events (so {@link AgUiStreamBridge#onEvent} routes them through the same ask-user-question /
     * AI-agent-event handlers a fresh streaming run uses); non-JSON payloads become text chunks. Blank lines + non-data
     * lines are skipped — the resume endpoint only emits {@code data:} frames today, but tolerating future
     * {@code event:}/{@code id:} fields keeps the parser non-lossy.
     */
    private void streamResumeBody(java.util.stream.Stream<String> lines, AgUiStreamBridge bridge) {
        try (java.util.stream.Stream<String> stream = lines) {
            stream.forEach(line -> {
                if (!line.startsWith("data:")) {
                    return;
                }

                String payload = line.substring("data:".length())
                    .stripLeading();

                if (payload.isEmpty()) {
                    return;
                }

                // Try JSON first so a structured event (ask_user_question, AI agent SSE event) routes through
                // the bridge's typed handlers. Plain-text chunks fall through and emit as text deltas.
                Object parsed;

                try {
                    parsed = jsonMapper.readValue(payload, Map.class);
                } catch (RuntimeException jsonException) {
                    parsed = payload;
                }

                bridge.onEvent(parsed);
            });
        }
    }

    /**
     * Completes a streaming resume turn. Mirrors the ask-user-question/no-text branches of {@link #handleSyncOutputs}
     * for chat-memory persistence — the user's edited message and any final assistant text need to land in chat-memory
     * so the chat survives a reload, regardless of which transport carried the response.
     */
    private void handleResumeStreamComplete(
        AgUiStreamBridge bridge, AiHubChat chat, String userMessage) {
        // Streaming resume currently doesn't surface a single "final message" the way the sync handler does —
        // the bridge has been emitting text deltas as they arrive, so the assistant's reply is already
        // visible. Persist the user message to chat-memory; the assistant text is captured separately by
        // ChatMemory's advisor when the agent path runs (which it doesn't for the bridge), so for parity with
        // the streaming path we accept that streaming-resume-only assistant text won't survive a reload until
        // the broader chat-memory persistence work that's tracked separately.
        persistTurnToChatMemory(chat.getThreadId(), userMessage, null);

        bridge.onComplete();
    }

    private @Nullable Object parseResponseBody(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        try {
            return jsonMapper.readValue(body, Map.class);
        } catch (RuntimeException exception) {
            // Non-JSON or unexpected shape — fall through with the raw string so handleSyncOutputs can still
            // surface it as a plain text reply rather than failing the whole resume turn.
            if (log.isDebugEnabled()) {
                log.debug("Workflow resume response was not parseable JSON; surfacing raw body", exception);
            }

            return body;
        }
    }

    private void handleSyncOutputs(
        @Nullable Object outputs, AgUiStreamBridge bridge, AgentSubscriber subscriber, String messageId,
        AiHubChat chat, String userMessageText) {

        // Detect ask_user_question / questions in the sync result so the bridge translates to a custom event
        // before completing. Mirrors the streaming path's onEvent handling.
        if (outputs instanceof Map<?, ?> outputMap
            && (outputMap.containsKey("ask_user_question") || outputMap.containsKey("questions"))) {

            bridge.onEvent(outputMap);

            // Persist the user input even on the ask-user-question path — the workflow paused waiting for an
            // answer, but the user's previous message is still part of the chat history.
            persistTurnToChatMemory(chat.getThreadId(), userMessageText, null);

            bridge.onComplete();

            return;
        }

        // Three cases to handle once the workflow finishes:
        // 1. Streaming AI agent already emitted tokens through the bridge — the assistant reply is fully delivered
        // via TextMessageContent deltas, no further emit needed. Just complete the run.
        // 2. Workflow used chat/responseToRequest — extractMessage pulls the body.message; emit it as a single
        // text event + AssistantMessage so the client sees the canonical assistant reply.
        // 3. Workflow produced neither — empty outputs, no streamed text. Surface a clear, actionable hint so the
        // user knows their workflow needs either a streaming AI agent task or a chat/responseToRequest step.
        String message = extractMessage(outputs);

        if (bridge.hasStarted()) {
            // Case 1: streaming task already delivered text deltas. Re-emitting now would either duplicate text
            // (if the workflow ALSO has a chat/responseToRequest with the same content) or graft an unrelated
            // message onto the streamed reply. The streaming path is authoritative — its events drive the AG-UI
            // client's message rendering — so let onComplete close out the in-flight TextMessageStart/Content
            // sequence and skip the synthetic onNewMessage emit. Persist the streamed assistant text to
            // chat-memory if extractMessage found one (covers stream + chat/responseToRequest hybrid workflows
            // where both produce content); otherwise persist just the user turn.
            persistTurnToChatMemory(chat.getThreadId(), userMessageText, message);

            bridge.onComplete();

            return;
        }

        if (message == null || message.isBlank()) {
            // Case 3: nothing streamed AND nothing extractable. Common shapes that land here:
            // - Empty outputs map: the workflow has no chat/responseToRequest AND no streaming task.
            // - {__webhookResponse: {body: {message: <non-string>}}}: the user wired
            // `message: ${<aiTask>}` (full task output) into chat/responseToRequest. The full output
            // usually isn't a String — it's a Map / record that the chat trigger can't render — so
            // coerceToText returns null. The fix is `${<aiTask>.text}` (or whichever scalar field
            // carries the reply) so the chat trigger receives a String.
            //
            // Log the wrapped body shape too when present so ops can see what the workflow actually emitted
            // — the topLevelKeys=[__webhookResponse] alone doesn't tell the user whether their problem is
            // a missing reply step or a misshaped reference.
            if (outputs != null) {
                // Pull the raw __webhookResponse value (whatever type it is) for diagnostic logging. The earlier
                // version of this log assumed the envelope is always a Map and printed "(no envelope)" when it
                // wasn't — which hides exactly the case we now know exists in the wild (envelope is something
                // other than a Map). Capture the raw value so we can log its class name and JSON preview
                // regardless of type.
                Object rawEnvelope = null;

                if (outputs instanceof Map<?, ?> outputMap) {
                    rawEnvelope = outputMap.get(MetadataConstants.WEBHOOK_RESPONSE);
                }

                String envelopePreview;

                try {
                    envelopePreview = rawEnvelope == null ? "(null)" : jsonMapper.writeValueAsString(rawEnvelope);
                } catch (RuntimeException jsonException) {
                    envelopePreview = String.valueOf(rawEnvelope);
                }

                if (envelopePreview.length() > 2000) {
                    envelopePreview = envelopePreview.substring(0, 2000) + "…(truncated)";
                }

                log.warn(
                    "Workflow chat {} produced no extractable chat reply and no streamed text. " +
                        "outputsType={} topLevelKeys={} envelopeType={} envelopePreview={}. If the workflow " +
                        "uses chat/responseToRequest with `message: ${{<aiTask>}}`, change it to " +
                        "`${{<aiTask>.text}}` or the matching scalar field on the AI task's output schema.",
                    chat.getId(),
                    outputs.getClass()
                        .getSimpleName(),
                    outputs instanceof Map<?, ?> outputMap ? outputMap.keySet() : "(not a Map)",
                    rawEnvelope == null ? "(null)"
                        : rawEnvelope.getClass()
                            .getName(),
                    envelopePreview);
            } else {
                log.warn(
                    "Workflow chat {} produced null outputs and no streamed text. Workflow likely " +
                        "has no terminal task; add a streaming AI agent task or a `chat/responseToRequest` step.",
                    chat.getId());
            }

            message = "This workflow finished but produced no chat reply. If you wired "
                + "`Chat → Response to Chat Request` with `message: ${<aiTask>}`, change it to "
                + "`${<aiTask>.text}` (or the matching scalar field on the AI task's output) so the chat "
                + "trigger receives a string instead of the full task object.";
        }

        // Case 2 (and the case 3 fallback): emit a single text event + AssistantMessage so the AG-UI client
        // renders the assistant reply. ChatMemory persistence keeps the chat history reachable after a
        // page reload — see the persistTurnToChatMemory javadoc for why this is explicit on the bridge path.
        bridge.onEvent(message);

        AssistantMessage assistantMessage = new AssistantMessage();

        assistantMessage.setId(messageId);
        assistantMessage.setContent(message);

        subscriber.onNewMessage(assistantMessage);

        persistTurnToChatMemory(chat.getThreadId(), userMessageText, message);

        bridge.onComplete();
    }

    /**
     * Persists a user/assistant exchange to the AI Hub session store so the chat thread survives a page reload. The
     * bridge bypasses the LLM agent's memory advisor pipeline, so without this call workflow chats would have no
     * persisted history — clicking back into a workflow chat after navigating away would render an empty thread even
     * though the chat row exists.
     *
     * <p>
     * Best-effort: a session-store write failure logs at WARN and the user-visible turn continues. The
     * {@link AiHubSessionMemory} bean is workspace-shared (one bean for the whole app); sessions are keyed by
     * {@code threadId}, which uniquely identifies a chat across the workspace+user partition, and owned by the AI Hub's
     * shared session user id.
     * </p>
     *
     * <p>
     * <b>Preserve-context note:</b> we always write, even for stateless workflows that don't read the history. The cost
     * is small (two events per turn) and the failure mode for "don't write" is much worse (lost thread on reload).
     * </p>
     */
    private void
        persistTurnToChatMemory(String threadId, String userMessageText, @Nullable String assistantMessageText) {
        try {
            SessionRepository sessionRepository = sessionMemory.sessionRepository();

            if (Optional.ofNullable(sessionRepository.findById(threadId))
                .isEmpty()) {

                sessionRepository.save(Session.builder()
                    .id(threadId)
                    .userId(AiHubSessionMemory.SESSION_USER_ID)
                    .createdAt(java.time.Instant.now())
                    .build());
            }

            sessionRepository.appendEvent(SessionEvent.builder()
                .sessionId(threadId)
                .message(new org.springframework.ai.chat.messages.UserMessage(userMessageText))
                .build());

            if (assistantMessageText != null && !assistantMessageText.isBlank()) {
                sessionRepository.appendEvent(SessionEvent.builder()
                    .sessionId(threadId)
                    .message(new org.springframework.ai.chat.messages.AssistantMessage(assistantMessageText))
                    .build());
            }
        } catch (RuntimeException exception) {
            log.warn("Failed to persist workflow-chat turn to session memory for threadId {}", threadId, exception);
        }
    }

    /**
     * Pulls the assistant-reply text out of the workflow's sync result. Three shapes are recognised:
     * <ol>
     * <li><b>Wrapped {@code __webhookResponse} envelope</b> — the canonical chat workflow shape produced by the
     * {@code chat/responseToRequest} action via
     * {@link com.bytechef.component.definition.ActionDefinition.WebhookResponse#json}. The executor stores the action's
     * return value under {@link MetadataConstants#WEBHOOK_RESPONSE} so the HTTP controller's
     * {@code processWebhookResponse} unwrap path can render it; the bridge mirrors that unwrap and pulls
     * {@code body.message} (for JSON-shaped bodies) or the body itself (for RAW-shaped String bodies).</li>
     * <li><b>Map with a string {@code message} field at the top level</b> — older / minimal workflows that emit a plain
     * map directly without going through {@code WebhookResponse}. Returns the message verbatim.</li>
     * <li><b>Bare String</b> — workflows whose final task output is just text. Returns it verbatim.</li>
     * </ol>
     *
     * <p>
     * Anything else returns {@code null}, signalling "no chat reply" so {@link #handleSyncOutputs} skips the text emit.
     * The unwrap mirrors {@code AbstractWebhookTriggerController.processWebhookResponse}'s logic — keeping the bridge
     * aligned with the HTTP transport so a workflow that renders correctly via the legacy {@code /chats} multipart path
     * also renders correctly via the AG-UI bridge.
     * </p>
     */
    private @Nullable String extractMessage(@Nullable Object outputs) {
        if (outputs instanceof Map<?, ?> outputMap) {
            Object webhookResponseValue = outputMap.get(MetadataConstants.WEBHOOK_RESPONSE);

            // Canonical chat workflow: chat/responseToRequest emits a WebhookResponse. After storage round-trip
            // it usually deserialises into a LinkedHashMap with {body, headers, statusCode, type}. Two other
            // observed shapes get the same handling here:
            //
            // - Direct String: some serialization paths collapse the envelope to just the response string
            // (rare, but seen in the wild on chats created against older code).
            // - Nested with no `body` field: some serializers strip null fields, leaving a Map with only
            // headers/statusCode/type. We probe the envelope itself for message-like keys as a last
            // resort so the user's reply still surfaces.
            if (webhookResponseValue instanceof String envelopeString) {
                if (!envelopeString.isBlank()) {
                    return envelopeString;
                }
            } else if (webhookResponseValue instanceof Map<?, ?> webhookResponseMap) {
                Object body = webhookResponseMap.get("body");

                if (body instanceof Map<?, ?> bodyMap) {
                    String coerced = coerceToText(bodyMap.get("message"));

                    if (coerced != null) {
                        return coerced;
                    }
                }

                if (body instanceof String bodyString) {
                    return bodyString;
                }

                // Defensive: if the envelope's `body` field is missing (some serializations strip it when it
                // would round-trip as null, others use different field naming), probe the envelope itself for
                // a message-shaped field. Covers the case where the ChatResponseToRequestAction path emitted
                // `{message, attachments}` directly into the __webhookResponse slot rather than nested under
                // `body`. Same coerceToText probe as above so the matching keys stay consistent.
                String envelopeCoerced = coerceToText(webhookResponseMap.get("message"));

                if (envelopeCoerced != null) {
                    return envelopeCoerced;
                }

                // Last-resort fallback: if the envelope is a one-key map with a String value, treat that as
                // the chat reply. Catches the rare case where the workflow stored a raw String under
                // __webhookResponse instead of a structured response.
                if (webhookResponseMap.size() == 1) {
                    Object onlyValue = webhookResponseMap.values()
                        .iterator()
                        .next();

                    if (onlyValue instanceof String onlyString && !onlyString.isBlank()) {
                        return onlyString;
                    }
                }
            } else if (webhookResponseValue != null) {
                // The __webhookResponse slot holds a non-Map non-String value — most likely the
                // {@link com.bytechef.component.definition.ActionDefinition.WebhookResponse} class itself
                // (Jackson typing config preserved the concrete type instead of deserialising to a Map).
                // Pull the body out via reflection-style key probing. We can't import WebhookResponse here
                // without adding a component-api dependency to the ai-hub service, so we go through a
                // best-effort cast: if the object happens to expose `getBody()` it will round-trip cleanly
                // when re-serialised, and we re-enter the Map branch via a JSON dance. Falls through silently
                // when the cast doesn't apply — the WARN log surfaces the unknown type for follow-up.
                String fromTypedObject = extractFromTypedWebhookResponse(webhookResponseValue);

                if (fromTypedObject != null) {
                    return fromTypedObject;
                }
            }

            // Fallback: a workflow that doesn't go through WebhookResponse can still return a plain map with a
            // top-level message field. Preserve this path so simpler workflows keep working, and so the existing
            // unit tests (which mock this shape directly) still pin the dispatch logic.
            String coerced = coerceToText(outputMap.get("message"));

            if (coerced != null) {
                return coerced;
            }
        }

        if (outputs instanceof String stringOutput) {
            return stringOutput;
        }

        return null;
    }

    /**
     * Attempts to extract a chat-renderable text from a non-Map, non-String value parked under
     * {@link MetadataConstants#WEBHOOK_RESPONSE}. The most common case: storage returned the original
     * {@code WebhookResponse} class (because Jackson type info was preserved during the round-trip), so
     * {@link Object#toString} would yield a class-stringified blob rather than the chat reply.
     *
     * <p>
     * Strategy: serialise the value back to JSON, parse as {@code Map}, and re-run the Map-shape extraction. Cheap (the
     * value is already small), reflection-free, and resilient to whichever concrete class the storage layer returns. If
     * serialisation fails or the round-tripped Map still has no extractable message, returns null and lets the fallback
     * path fire — the WARN log will surface the type so we can extend support directly here.
     * </p>
     */
    private @Nullable String extractFromTypedWebhookResponse(Object webhookResponseValue) {
        try {
            String json = jsonMapper.writeValueAsString(webhookResponseValue);
            Object reparsed = jsonMapper.readValue(json, Object.class);

            if (reparsed instanceof Map<?, ?> reparsedMap) {
                // Wrap and recurse so the Map branch's full extraction logic applies (body unwrap +
                // coerceToText + envelope-level probe).
                return extractMessage(Map.of(MetadataConstants.WEBHOOK_RESPONSE, reparsedMap));
            }

            if (reparsed instanceof String reparsedString && !reparsedString.isBlank()) {
                return reparsedString;
            }
        } catch (RuntimeException jsonException) {
            if (log.isDebugEnabled()) {
                log.debug(
                    "Could not re-parse __webhookResponse value of type {} for chat reply extraction",
                    webhookResponseValue.getClass()
                        .getName(),
                    jsonException);
            }
        }

        return null;
    }

    /**
     * Permissive coercion of a {@code message} field value to a chat-renderable text string. Handles three shapes:
     * <ul>
     * <li><b>String</b> — return verbatim. The canonical case where {@code chat/responseToRequest} was wired with a
     * plain text expression (e.g. {@code ${openAi_1.text}}).</li>
     * <li><b>Map with text-shaped fields</b> — common when the user wires {@code message: ${openAi_1}} and the AI
     * chat's output is an object like {@code {content: "...", role: "assistant"}}. Probes a small set of likely keys
     * ({@code content}, {@code text}, {@code message}, {@code response}, {@code answer}) so the chat renders the actual
     * reply rather than a stringified JSON dump. Skips numeric / structural fields that aren't intended as user-facing
     * text.</li>
     * <li><b>Other</b> — return null, signalling "no extractable message" so the caller falls through to the actionable
     * fallback hint instead of rendering the toString of an object the user didn't mean to show.</li>
     * </ul>
     */
    private static @Nullable String coerceToText(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String stringValue) {
            return stringValue;
        }

        if (value instanceof Map<?, ?> mapValue) {
            // Probe in priority order — `content` matches Spring AI's AssistantMessage shape (the most common
            // case when wiring an AI task's output directly into chat/responseToRequest); `text` matches the
            // streamAsk action's flat output; `message` covers nested-message shapes; `response`/`answer` are
            // common community-component conventions. First non-blank string wins.
            for (String key : new String[] {
                "content", "text", "message", "response", "answer"
            }) {
                Object candidate = mapValue.get(key);

                if (candidate instanceof String candidateString && !candidateString.isBlank()) {
                    return candidateString;
                }
            }
        }

        return null;
    }

    /**
     * Builds the {@link WebhookRequest} the bridge hands to the workflow executor. The shape mirrors what the legacy
     * {@code Chats.tsx} multipart-form-data POST produced after server-side parsing:
     * {@code body.content = Map<message, chatId, attachments>} with {@code contentType = JSON}. The downstream chat
     * trigger ({@code ChatNewRequestTrigger}) reads from {@code body.content} — leaving the body empty (the v1 shape)
     * made the trigger throw {@code IllegalArgumentException("Invalid webhook request.")} for every workflow chat using
     * {@code newChatRequest}, so this fix is also required for text-only chats.
     *
     * <p>
     * <b>Attachment contract (forwardedProps):</b> the AG-UI client signals attachments through {@code forwardedProps}
     * as a {@code Map} with key {@code "attachments"} holding a {@code List} of {@code Map<String, Object>} entries
     * shaped like {@code {name: String, contentType: String, base64: String}}. Each entry is decoded, stored through
     * {@link AssetFileFacade#createFromUpload} so the file becomes a first-class workspace artifact (browsable in the
     * Files panel, queryable by other tools, retained per workspace quota), and the resulting {@code FileEntry} is
     * wrapped in {@link BridgedFileEntry} for the trigger's {@code instanceof FileEntry} discriminator. Same storage
     * shape the legacy multipart upload path produced — using AssetFileFacade rather than the older temp file scope
     * means a file dropped in either flavour of chat (workflow-chat or standard) becomes a first-class workspace
     * artifact instead of vanishing after the turn.
     * </p>
     */
    private WebhookRequest buildWebhookRequest(
        String userMessage, AiHubChat chat, @Nullable Object forwardedProps) {

        String threadId = chat.getThreadId();

        Map<String, Object> bodyContent = new HashMap<>();

        bodyContent.put("message", userMessage);
        bodyContent.put("chatId", threadId);
        bodyContent.put("attachments", promoteAttachments(extractAttachments(forwardedProps), chat));

        // Parameters stays populated for triggers that read from there (e.g. plain "incoming webhook" patterns).
        // The new chat trigger reads exclusively from body.content, but other webhook triggers a workflow chat
        // could be wired against may still consult parameters — keep the existing entries for symmetry.
        Map<String, List<String>> parameters = new HashMap<>();

        parameters.put("message", List.of(userMessage));
        parameters.put("chatId", List.of(threadId));

        return new WebhookRequest(
            Map.of(),
            parameters,
            new WebhookRequest.WebhookBodyImpl(bodyContent, ContentType.JSON, "application/json", null),
            WebhookMethod.POST);
    }

    /**
     * Promotes the raw client-shape attachment list ({@code [{name, contentType, base64}, ...]}) into a list of SDK
     * {@link com.bytechef.component.definition.FileEntry} instances by uploading each base64 payload through
     * {@link AssetFileFacade#createFromUpload} (so the file becomes a first-class workspace artifact) and wrapping the
     * resulting {@link AssetFile}'s underlying platform {@code FileEntry} in {@link BridgedFileEntry}.
     *
     * <p>
     * Why we promote here rather than leave raw maps: webhook triggers — notably {@code ChatNewRequestTrigger.checkMap}
     * — discriminate by {@code list.getFirst() instanceof FileEntry}. Without promotion the trigger silently picks the
     * "single value" branch and the workflow never sees the attachment list as files. Failed promotions (decode error,
     * upload failure) drop just that entry at WARN so a single bad attachment doesn't fail the whole turn.
     * </p>
     *
     * <p>
     * Asset files inherit the chat's workspace + environment so they sit alongside whatever LLM- or workflow-emitted
     * artifacts the user already has in the Files panel. The {@code AssetFileSource} defaults to the upload variant
     * (set by {@code AssetFileFacade.createFromUpload}) — distinct from the AI-generated source — so listings can
     * filter on origin if needed.
     * </p>
     */
    private List<Object> promoteAttachments(List<?> rawAttachments, AiHubChat chat) {
        List<Object> result = new ArrayList<>(rawAttachments.size());

        for (Object raw : rawAttachments) {
            if (!(raw instanceof Map<?, ?> attachmentMap)) {
                if (log.isDebugEnabled()) {
                    log.debug(
                        "Dropping non-Map attachment entry: {}",
                        raw == null ? "null"
                            : raw.getClass()
                                .getSimpleName());
                }

                metrics.recordAttachmentFailure("malformed");

                continue;
            }

            try {
                BridgedFileEntry bridged = promoteAttachment(attachmentMap, chat);

                if (bridged != null) {
                    result.add(bridged);
                }
            } catch (RuntimeException exception) {
                log.warn(
                    "Failed to promote workflow-chat attachment to FileEntry; dropping. name={} contentType={}",
                    attachmentMap.get("name"), attachmentMap.get("contentType"), exception);

                // Reason categorisation here is best-effort — we want to know "storage backend is flaky" vs
                // "client sent garbage" without doing per-exception-type matching. IllegalArgumentException
                // from the Base64 decoder lands as decode_failure; everything else is upload_failure (covers
                // workspace-quota, size-limit, and storage-backend errors from AssetFileFacade).
                String reason = exception instanceof IllegalArgumentException ? "decode_failure" : "upload_failure";

                metrics.recordAttachmentFailure(reason);
            }
        }

        return result;
    }

    private @Nullable BridgedFileEntry
        promoteAttachment(Map<?, ?> attachmentMap, AiHubChat chat) {
        Object base64Object = attachmentMap.get("base64");

        if (!(base64Object instanceof String base64String) || base64String.isBlank()) {
            // No payload to promote — clients that send only metadata (e.g. a stub for a file already uploaded
            // out-of-band) shouldn't drive the workflow. Drop with a DEBUG breadcrumb.
            if (log.isDebugEnabled()) {
                log.debug("Attachment without base64 payload; dropping. name={}", attachmentMap.get("name"));
            }

            metrics.recordAttachmentFailure("missing_payload");

            return null;
        }

        // Estimate the decoded size from the base64 string length BEFORE decoding so a 100 MB base64 string
        // (≈ 75 MB decoded) fails fast without allocating the byte array. Each 4 base64 chars decode to 3 bytes;
        // the actual decoded length is at most `len * 3 / 4` minus 0-2 for padding. Conservative upper bound is
        // sufficient — we're rejecting, not measuring.
        long estimatedDecodedBytes = (long) base64String.length() * 3L / 4L;

        if (estimatedDecodedBytes > MAX_ATTACHMENT_BYTES) {
            log.warn(
                "Workflow-chat attachment exceeds the {} MB cap; dropping. name={} estimatedSize≈{} bytes",
                MAX_ATTACHMENT_BYTES / (1024 * 1024), attachmentMap.get("name"), estimatedDecodedBytes);

            metrics.recordAttachmentFailure("too_large");

            return null;
        }

        Object nameObject = attachmentMap.get("name");
        String filename = nameObject instanceof String nameString && !nameString.isBlank()
            ? nameString
            : "attachment-" + UUID.randomUUID();

        Object contentTypeObject = attachmentMap.get("contentType");
        String contentType = contentTypeObject instanceof String contentTypeString && !contentTypeString.isBlank()
            ? contentTypeString
            : "application/octet-stream";

        byte[] bytes = Base64.getDecoder()
            .decode(base64String);

        // createFromUpload runs workspace quota + size-limit checks. The chat's environment ordinal is used
        // so dev-uploaded attachments don't leak into prod's Files panel listings (env is part of every list query).
        AssetFile assetFile = assetFileFacade.createFromUpload(
            chatService.getWorkspaceId(chat.getId()), chat.getEnvironment()
                .ordinal(),
            filename, contentType, new ByteArrayInputStream(bytes));

        FileEntry stored = assetFile.getFile();

        return new BridgedFileEntry(
            assetFile.getName(), stored.getExtension(), assetFile.getMimeType(), stored.getUrl());
    }

    /**
     * Extracts the raw attachment list from {@code RunAgentInput.forwardedProps}. Returns an empty list when no
     * attachments are present, so {@code ChatNewRequestTrigger}'s {@code if (!content.containsKey("attachments"))}
     * fallback never fires (the bridge always supplies the key). The result is fed to {@link #promoteAttachments} for
     * the streaming path; the resume path serialises the raw list as JSON to ride the loopback POST through the same
     * {@code WebhookRequestUtils} parsing the controller already uses.
     *
     * <p>
     * Defensive against shape drift on the wire — assistant-ui's attachment payloads have changed shape twice in the
     * past year. Anything that isn't a recognisable {@code List} is logged at DEBUG and dropped, so a malformed client
     * doesn't take down the whole turn.
     * </p>
     */
    private static List<?> extractAttachments(@Nullable Object forwardedProps) {
        if (!(forwardedProps instanceof Map<?, ?> propsMap)) {
            return List.of();
        }

        Object attachmentsValue = propsMap.get("attachments");

        if (attachmentsValue instanceof List<?> attachmentsList) {
            return attachmentsList;
        }

        if (attachmentsValue != null && log.isDebugEnabled()) {
            log.debug(
                "Unexpected forwardedProps.attachments shape: {} — dropping",
                attachmentsValue.getClass()
                    .getSimpleName());
        }

        return List.of();
    }

    /**
     * Walk the message history backwards looking for the most recent user message. We match on the {@code role} field
     * rather than the concrete {@code UserMessage} subtype so any future AG-UI message subclass that still carries
     * {@code Role.user} continues to work — narrowing to {@code instanceof UserMessage} would silently drop the user
     * input and surface "No user message found" for any client that dispatched via a different concrete type. The
     * downside (a non-{@code UserMessage} that erroneously claims {@code Role.user}) is benign: we'd just forward its
     * content to the workflow.
     */
    private static @Nullable String lastUserMessage(@Nullable List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        for (int i = messages.size() - 1; i >= 0; i--) {
            BaseMessage message = messages.get(i);

            if (message != null && Role.user.equals(message.getRole())) {
                return message.getContent();
            }
        }

        return null;
    }

    /**
     * Verifies the workflow is reachable (enabled and not deleted) before spinning up the executor + an
     * {@link AgUiStreamBridge}. Returns the {@link WebhookTriggerFlags} on success; on rejection releases the admission
     * lock, emits a friendly RUN_ERROR via {@link #emitError}, and returns {@code null} so the caller can
     * short-circuit.
     *
     * <p>
     * Two failure modes share this helper because both happen in the same lookup path: the workflow is enabled but
     * disabled mid-turn ({@code isWorkflowDisabled} returns true), or the underlying workflow / project deployment /
     * trigger has been deleted out from under the chat (any RuntimeException from the lookup chain). Catching any
     * RuntimeException is broader than strict {@code NotFoundException}-only handling, but the facade composes calls
     * across several services and narrowing to one exception type misses real cases (project deployment deleted while
     * the workflow row remains).
     * </p>
     *
     * <p>
     * Both rejection paths release the admission lock explicitly because the bridge — which would normally release on
     * its own terminal events — never gets constructed in this branch. Without the explicit release the chat would stay
     * locked until cache TTL expired, blocking the user from retrying after they re-enable the deployment.
     * </p>
     */
    private @Nullable WebhookTriggerFlags lookupTriggerFlagsOrEmitError(
        RunAgentInput input, WorkflowExecutionId workflowExecutionId, String workflowExecutionIdStr,
        AiHubChat chat, AgentSubscriber subscriber) {

        try {
            if (webhookWorkflowExecutor.isWorkflowDisabled(workflowExecutionId)) {
                metrics.recordUnreachable("disabled");

                guard.release(chat.getId());

                emitError(input, subscriber, disabledChatErrorMessage(chat));

                return null;
            }

            return webhookWorkflowExecutor.getWebhookTriggerFlags(workflowExecutionId);
        } catch (RuntimeException exception) {
            log.warn(
                "Workflow chat trigger lookup failed for chat {} (workflowExecutionId={})",
                chat.getId(), workflowExecutionIdStr, exception);

            metrics.recordUnreachable("deleted");

            guard.release(chat.getId());

            emitError(input, subscriber, deletedWorkflowErrorMessage(chat));

            return null;
        }
    }

    /**
     * Per-chat admission gate: enforces a 2-second cooldown between turns (rate limit) and refuses a new turn while a
     * previous one is still in flight (concurrency). Returns {@code true} if the turn is admitted; on rejection emits a
     * RUN_ERROR with the guard's user-facing message and returns {@code false} so the caller can short-circuit the rest
     * of run().
     *
     * <p>
     * Concurrency is checked before the rate limit so a button-mash gets the more actionable "previous turn still
     * running" message; the rate limit applies only to genuinely back-to-back attempts where the previous turn finished
     * quickly. Admission is checked AFTER user-message extraction in run() so a pure no-op poll with no message doesn't
     * burn the cooldown budget — the failure modes are independent and shouldn't couple.
     * </p>
     */
    private boolean
        tryAdmitOrEmitError(RunAgentInput input, AiHubChat chat, AgentSubscriber subscriber) {
        WorkflowChatGuard.AdmissionResult admission = guard.tryAdmit(chat.getId());

        if (admission instanceof WorkflowChatGuard.AdmissionResult.Admit) {
            return true;
        }

        String outcome = admission instanceof WorkflowChatGuard.AdmissionResult.RateLimited
            ? "rate_limited"
            : "concurrency_blocked";

        // Record both the global and per-workspace counters for the rejection so ops can spot a single workspace
        // hammering the gate (typical sign of a buggy embedded client) versus a global spike (likely a deploy
        // affecting all clients).
        metrics.recordTurn(outcome);
        metrics.recordTurnByWorkspace(outcome, chatService.getWorkspaceId(chat.getId()));

        String message = admission.userFacingMessage();

        emitError(input, subscriber, message != null ? message : "Workflow chat turn rejected");

        return false;
    }

    /**
     * Emits the {@code RUN_STARTED} lifecycle event. Routed through both {@code subscriber.onEvent} (so
     * {@code AgentStreamer} writes it onto the SSE channel) and the typed {@code onRunStartedEvent} dispatch — same
     * pattern as {@link AgUiStreamBridge#dispatch} in the bridge's {@code RUN_FINISHED}/{@code RUN_ERROR} paths.
     * Without the {@code onEvent} call the event hits the AgentSubscriber's empty default impl and never reaches the
     * client.
     */
    private static void emitRunStarted(RunAgentInput input, AgentSubscriber subscriber) {
        RunStartedEvent event = new RunStartedEvent();

        event.setThreadId(input.threadId());
        event.setRunId(input.runId());

        subscriber.onEvent(event);
        subscriber.onRunStartedEvent(event);
    }

    /**
     * Fires {@code RUN_ERROR} and immediately finalizes the run so the SSE emitter closes. Every early-exit path in
     * {@link #run} routes through this helper so a workflow-chat error doesn't leave the client UI sitting in a
     * permanent "running" state. Pairing the {@code RunErrorEvent} emit with the {@code finalizeRun} call here
     * eliminates the "remember to also finalize" footgun that would otherwise need to live at every call site.
     */
    private void emitError(RunAgentInput input, AgentSubscriber subscriber, String message) {
        RunErrorEvent event = new RunErrorEvent();

        event.setError(message);

        // subscriber.onEvent FIRST so AgentStreamer pushes the event onto the SSE; the typed dispatch is
        // for any subscribers that hook the specific event type (no-op on the streaming path today). Without
        // the onEvent call the RUN_ERROR never reaches the client and the UI sits in "running" state until
        // the SSE timeout — same root cause as the bridge bypass we fixed in AgUiStreamBridge.dispatch.
        subscriber.onEvent(event);
        subscriber.onRunErrorEvent(event);

        finalizeRun(input, subscriber);
    }

    /**
     * Fires {@code subscriber.onRunFinalized(...)} — the AgentSubscriber lifecycle hook
     * {@code AgentStreamer.streamEvents} listens to in order to call {@code eventStream.complete()} and close the SSE
     * emitter. {@code SpringAIAgent} fires the equivalent call inline at the end of its run; for the webhook bridge we
     * funnel every completion path (sync handleSyncOutputs, streaming whenComplete, resume completion, early-exit
     * emitError) through this helper so the SSE channel reliably closes.
     *
     * <p>
     * Without this, a workflow chat completes server-side and emits {@code RUN_FINISHED}, but the client UI sits in a
     * permanent "running" state because the SSE stream never closes — the AG-UI runtime can't tell the run is over.
     * </p>
     *
     * @param input      the original {@link RunAgentInput}; passed verbatim to the params record so the subscriber can
     *                   inspect the messages / context it ran against.
     * @param subscriber the AG-UI subscriber to finalize.
     */
    private void finalizeRun(RunAgentInput input, AgentSubscriber subscriber) {
        try {
            subscriber.onRunFinalized(new AgentSubscriberParams(input.messages(), state, this, input));
        } catch (RuntimeException exception) {
            // Defensive — onRunFinalized failures are diagnostic only; the run has already happened. Logging here so
            // an SSE leak surfaces in ops dashboards rather than silently leaving the client hung.
            log.warn(
                "subscriber.onRunFinalized threw for thread {} (run {}); SSE may stay open",
                input.threadId(), input.runId(), exception);
        }
    }
}
