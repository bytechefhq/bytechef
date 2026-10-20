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
import com.bytechef.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.metric.WorkflowChatMetrics;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * AG-UI {@link LocalAgent} that serves the turns of {@code kind = WORKFLOW_CHAT} chats through the webhook executor
 * instead of an LLM. Built on {@link WebhookWorkflowExecutor} so the HTTP/SSE webhook controller and this bridge share
 * the same orchestration logic.
 *
 * <p>
 * Per-turn flow:
 * </p>
 * <ol>
 * <li>Look up the {@link AiHubChat} for {@code input.threadId()} and verify it is webhook-bridged; any other row
 * reaching this agent is a routing bug and ends the turn with a RUN_ERROR.</li>
 * <li>Extract the user's last message from {@code input.messages()} and build a {@link WebhookRequest} carrying it, the
 * chat id and the workflow's conversation id.</li>
 * <li>Resume a workflow paused at {@code ask_user_question} when the previous turn left a resume URL; otherwise run the
 * workflow synchronously through {@link WebhookWorkflowExecutor#executeSync} or, when it streams or its trigger is not
 * synchronous, through {@link WebhookWorkflowExecutor#stream} with an {@link AgUiStreamBridge}.</li>
 * <li>Every path ends with {@code RUN_FINISHED} or {@code RUN_ERROR} followed by {@code onRunFinalized}, as the LLM
 * agent's turns do, so the client and the run registry treat both kinds of chat alike.</li>
 * </ol>
 *
 * <p>
 * A Stop ({@link InFlightAiHubRunRegistry#stopRequested}) stops the turn's workflow job through a
 * {@link WorkflowChatTurnStopper}, whichever of the Stop and the job id arrives first.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WebhookBridgeAgent extends LocalAgent {

    private static final Logger log = LoggerFactory.getLogger(WebhookBridgeAgent.class);

    /**
     * Bound on the resume POST, so a workflow that holds the connection open does not pin a chat turn forever.
     */
    private static final Duration RESUME_REQUEST_TIMEOUT = Duration.ofSeconds(60);

    /**
     * The request body shape is calibrated for the canonical chat trigger. Turns against other triggers are logged so a
     * misconfigured workflow chat can be spotted.
     */
    private static final String CHAT_TRIGGER_NAME = "newChatRequest";

    /**
     * Prefix of the conversation id the workflow's own chat memory sees. It must differ from the thread id, which is
     * the hub's session id, because both memories can live in the same session store.
     */
    private static final String CONVERSATION_ID_PREFIX = "workflow-chat:";

    private final WebhookWorkflowExecutor webhookWorkflowExecutor;
    private final AiHubChatService chatService;
    private final WebhookResumeRegistry resumeRegistry;
    private final HttpClient resumeHttpClient;
    private final JsonMapper jsonMapper;
    private final WorkflowChatMetrics metrics;
    private final AiHubSessionMemory sessionMemory;
    private final WorkflowChatGuard guard;
    @Nullable
    private final JobFacade jobFacade;
    private final InFlightAiHubRunRegistry inFlightRunRegistry;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WebhookBridgeAgent(
        WebhookWorkflowExecutor webhookWorkflowExecutor, AiHubChatService chatService,
        WebhookResumeRegistry resumeRegistry, JsonMapper jsonMapper, WorkflowChatMetrics metrics,
        AiHubSessionMemory sessionMemory, WorkflowChatGuard guard,
        @Nullable JobFacade jobFacade, InFlightAiHubRunRegistry inFlightRunRegistry) throws AGUIException {

        super("webhook-bridge-agent", null, null, "(unused, the webhook bridge bypasses the LLM)", List.of());

        this.webhookWorkflowExecutor = webhookWorkflowExecutor;
        this.chatService = chatService;
        this.resumeRegistry = resumeRegistry;
        this.jsonMapper = jsonMapper;
        this.metrics = metrics;
        this.sessionMemory = sessionMemory;
        this.guard = guard;
        this.jobFacade = jobFacade;
        this.inFlightRunRegistry = inFlightRunRegistry;
        this.resumeHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    /**
     * Binds the caller's tenant for the whole bridged run before any of it executes. {@code LocalAgent.runAgent}
     * dispatches the body through a bare {@code CompletableFuture.runAsync}, and its first statement is a database
     * read, so without the binding the turn would run against the {@code public} schema.
     */
    @Override
    protected void run(RunAgentInput input, AgentSubscriber subscriber) {
        AiHubAgentTenantBinder.runWithTenant(getAgentId(), input.state(), () -> runBridge(input, subscriber));
    }

    @SuppressWarnings("checkstyle:methodlength")
    private void runBridge(RunAgentInput input, AgentSubscriber subscriber) {
        emitRunStarted(input, subscriber);

        Optional<AiHubChat> chatOptional = chatService.findByThreadId(input.threadId());

        if (chatOptional.isEmpty()) {
            emitError(input, subscriber, "AiHubChat not found for thread " + input.threadId());

            return;
        }

        AiHubChat chat = chatOptional.get();

        if (!chat.getKind()
            .isWebhookBridged()) {

            log.warn(
                "WebhookBridgeAgent invoked for a chat that is not webhook-bridged: {} (kind={}). Misroute.",
                chat.getId(), chat.getKind());

            emitError(input, subscriber, "AiHubChat kind mismatch: bridge agent invoked for a non-workflow chat");

            return;
        }

        String workflowExecutionIdString = chat.getWorkflowExecutionId();

        if (workflowExecutionIdString == null || workflowExecutionIdString.isBlank()) {
            emitError(input, subscriber, "Workflow chat is missing workflowExecutionId");

            return;
        }

        WorkflowExecutionId workflowExecutionId;

        try {
            workflowExecutionId = WorkflowExecutionId.parse(workflowExecutionIdString);
        } catch (RuntimeException exception) {
            emitError(
                input, subscriber,
                "Failed to parse workflow execution id: " + workflowExecutionIdString + ": " + exception.getMessage());

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

        WorkflowChatTurnStopper stopper = new WorkflowChatTurnStopper(
            chat.getId(), TenantContext.getCurrentTenantId(), jobFacade);

        String pendingResumeUrl = resumeRegistry.consume(chat.getThreadId());

        if (pendingResumeUrl != null) {
            recordTurn("resume", chat);

            handleResume(input, pendingResumeUrl, userMessage, chat, subscriber, messageId, stopper);

            return;
        }

        WebhookTriggerFlags webhookTriggerFlags = lookupTriggerFlagsOrEmitError(
            input, workflowExecutionId, workflowExecutionIdString, chat, subscriber);

        if (webhookTriggerFlags == null) {
            return;
        }

        WebhookRequest webhookRequest = buildWebhookRequest(userMessage, chat);

        String triggerName = workflowExecutionId.getTriggerName();

        if (log.isDebugEnabled() && !CHAT_TRIGGER_NAME.equals(triggerName)) {
            log.debug(
                "Workflow chat {} runs against non-chat trigger '{}'; body shape may not match.", chat.getId(),
                triggerName);
        }

        boolean streamingPath = hasStreamingTask(workflowExecutionId) || !webhookTriggerFlags.workflowSyncExecution();

        recordTurn(streamingPath ? "streaming" : "sync", chat);

        String threadId = chat.getThreadId();
        AgUiStreamBridge[] bridgeReference = new AgUiStreamBridge[1];

        Disposable stopSubscription = subscribeToStop(input.threadId(), stopper);

        Runnable onFinalize = () -> {
            try {
                stopSubscription.dispose();

                AgUiStreamBridge currentBridge = bridgeReference[0];

                if (currentBridge != null && streamingPath) {
                    persistTurnToChatMemory(threadId, userMessage, currentBridge.getAccumulatedAssistantText());
                }
            } finally {
                finalizeRun(input, subscriber);
            }
        };

        AgUiStreamBridge bridge = new AgUiStreamBridge(
            subscriber, messageId, input.threadId(), input.runId(), resumeRegistry, stopper::onJobId,
            stopper::onStopRequested, onFinalize);

        bridgeReference[0] = bridge;

        if (!streamingPath) {
            executeSync(workflowExecutionId, webhookRequest, bridge, subscriber, messageId, chat, userMessage);

            return;
        }

        try {
            webhookWorkflowExecutor.stream(workflowExecutionId, webhookRequest, bridge);
        } catch (RuntimeException exception) {
            bridge.onError(exception);
        }
    }

    private boolean hasStreamingTask(WorkflowExecutionId workflowExecutionId) {
        try {
            return webhookWorkflowExecutor.hasStreamingTask(workflowExecutionId);
        } catch (RuntimeException exception) {
            if (log.isDebugEnabled()) {
                log.debug(
                    "hasStreamingTask lookup failed for {}; falling back to trigger flag", workflowExecutionId,
                    exception);
            }

            return false;
        }
    }

    /**
     * Records a turn in the global counter and, when the chat carries a workspace, in the per-workspace counter.
     */
    private void recordTurn(String outcome, AiHubChat chat) {
        metrics.recordTurn(outcome);

        Long workspaceId = chat.getWorkspaceId();

        if (workspaceId != null) {
            metrics.recordTurnByWorkspace(outcome, workspaceId);
        }
    }

    /**
     * Subscribes {@code stopper} to the run's Stop signal. Callers subscribe only after everything that can throw
     * before the bridge exists, because the subscription is disposed by the bridge's finalize callback.
     */
    private Disposable subscribeToStop(String threadId, WorkflowChatTurnStopper stopper) {
        Mono<Void> stopRequested = inFlightRunRegistry.stopRequested(threadId);

        return stopRequested.subscribe(
            null, throwable -> log.warn("Stop signal of workflow chat thread {} failed", threadId, throwable),
            stopper::onStopRequested);
    }

    private void executeSync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest, AgUiStreamBridge bridge,
        AgentSubscriber subscriber, String messageId, AiHubChat chat, String userMessageText) {

        try {
            if (webhookWorkflowExecutor.isWorkflowDisabled(workflowExecutionId)) {
                if (log.isDebugEnabled()) {
                    log.debug("Workflow chat disabled mid-turn for chat {}", chat.getId());
                }

                bridge.onError(new IllegalStateException(disabledChatErrorMessage(chat)));

                return;
            }

            Object outputs = webhookWorkflowExecutor.executeSync(workflowExecutionId, webhookRequest);

            handleSyncOutputs(outputs, bridge, subscriber, messageId, chat, userMessageText);
        } catch (RuntimeException exception) {
            bridge.onError(exception);
        }
    }

    /**
     * Renders the user-facing message for a disabled workflow. Names the chat so a user with several workflow chats
     * knows which one is affected.
     */
    private static String disabledChatErrorMessage(AiHubChat chat) {
        String title = chat.getTitle();

        if (title != null && !title.isBlank()) {
            return "This workflow chat (\"" + title + "\") is currently disabled. Re-enable the workflow's "
                + "deployment to resume the chat.";
        }

        return "This workflow chat is currently disabled. Re-enable the workflow's deployment to resume the chat.";
    }

    /**
     * Renders the user-facing message for a deleted workflow. Differs from the disabled message because the recovery
     * differs: a disabled chat can be re-enabled in place, a chat whose workflow is gone needs the workflow restored or
     * the chat archived.
     */
    private static String deletedWorkflowErrorMessage(AiHubChat chat) {
        String title = chat.getTitle();

        if (title != null && !title.isBlank()) {
            return "This workflow chat (\"" + title + "\") can no longer be reached: the underlying workflow "
                + "has been deleted or its deployment removed. Archive this chat, or restore the workflow to resume.";
        }

        return "This workflow chat can no longer be reached: the underlying workflow has been deleted or its "
            + "deployment removed. Archive this chat, or restore the workflow to resume.";
    }

    /**
     * Resume path: the previous turn paused the workflow at {@code ask_user_question} and {@link AgUiStreamBridge}
     * captured its resume URL. POSTs the user's answer to that URL and streams the response through a fresh
     * {@link AgUiStreamBridge}, so a nested {@code ask_user_question} registers a new resume URL in the same slot.
     *
     * <p>
     * An SSE response is fed through the bridge event by event; any other response is read whole and handled like a
     * synchronous result.
     * </p>
     *
     * <p>
     * <b>Tenant on the loopback:</b> the receiving webhook controller parses the workflow execution id out of the URL
     * path and runs under that id's tenant, so the POST needs no auth headers or tenant cookies.
     * </p>
     */
    private void handleResume(
        RunAgentInput input, String resumeUrl, String userMessage, AiHubChat chat, AgentSubscriber subscriber,
        String messageId, WorkflowChatTurnStopper stopper) {

        Disposable stopSubscription = subscribeToStop(input.threadId(), stopper);

        AgUiStreamBridge bridge = new AgUiStreamBridge(
            subscriber, messageId, input.threadId(), input.runId(), resumeRegistry, stopper::onJobId,
            stopper::onStopRequested, () -> {
                try {
                    stopSubscription.dispose();
                } finally {
                    finalizeRun(input, subscriber);
                }
            });

        try {
            String requestBody = jsonMapper.writeValueAsString(
                Map.of(
                    "message", userMessage,
                    "chatId", chat.getThreadId(),
                    "conversationId", conversationId(chat),
                    "attachments", List.of()));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(resumeUrl))
                .timeout(RESUME_REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream, application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<Stream<String>> response = resumeHttpClient.send(
                httpRequest, HttpResponse.BodyHandlers.ofLines());

            int statusCode = response.statusCode();

            if (statusCode < 200 || statusCode >= 300) {
                metrics.recordResume("http_error");

                String errorBody = collectBodyLines(response.body());

                bridge.onError(
                    new IllegalStateException("Workflow resume failed with HTTP " + statusCode + ": " + errorBody));

                return;
            }

            metrics.recordResume("success");

            Optional<String> contentTypeOptional = response.headers()
                .firstValue("Content-Type");

            String contentType = contentTypeOptional.orElse("");

            if (contentType.startsWith("text/event-stream")) {
                streamResumeBody(response.body(), bridge);

                persistTurnToChatMemory(chat.getThreadId(), userMessage, null);

                bridge.onComplete();

                return;
            }

            String body = collectBodyLines(response.body());

            handleSyncOutputs(parseResponseBody(body), bridge, subscriber, messageId, chat, userMessage);
        } catch (InterruptedException interruptedException) {
            Thread currentThread = Thread.currentThread();

            currentThread.interrupt();

            metrics.recordResume("transport_error");

            bridge.onError(interruptedException);
        } catch (RuntimeException | IOException exception) {
            log.warn("Workflow resume to {} failed for chat {}", resumeUrl, chat.getId(), exception);

            metrics.recordResume("transport_error");

            bridge.onError(exception);
        }
    }

    /**
     * Drains a line stream into a single newline-joined string.
     */
    private static String collectBodyLines(Stream<String> lines) {
        StringBuilder builder = new StringBuilder();

        try (Stream<String> stream = lines) {
            stream.forEach(line -> {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }

                builder.append(line);
            });
        }

        return builder.toString();
    }

    /**
     * Streams an SSE-shaped resume response through the bridge. Each {@code data:} payload that parses as JSON becomes
     * a {@code Map} event, so it is routed like the same event on a fresh streaming run; any other payload becomes a
     * text chunk. Blank and non-data lines are skipped.
     */
    private void streamResumeBody(Stream<String> lines, AgUiStreamBridge bridge) {
        try (Stream<String> stream = lines) {
            stream.forEach(line -> {
                if (!line.startsWith("data:")) {
                    return;
                }

                String data = line.substring("data:".length());

                String payload = data.stripLeading();

                if (payload.isEmpty()) {
                    return;
                }

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

    private @Nullable Object parseResponseBody(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        try {
            return jsonMapper.readValue(body, Map.class);
        } catch (RuntimeException exception) {
            if (log.isDebugEnabled()) {
                log.debug("Workflow resume response was not parseable JSON; surfacing raw body", exception);
            }

            return body;
        }
    }

    private void handleSyncOutputs(
        @Nullable Object outputs, AgUiStreamBridge bridge, AgentSubscriber subscriber, String messageId,
        AiHubChat chat, String userMessageText) {

        if (outputs instanceof Map<?, ?> outputMap
            && (outputMap.containsKey("ask_user_question") || outputMap.containsKey("questions"))) {

            bridge.onEvent(outputMap);

            persistTurnToChatMemory(chat.getThreadId(), userMessageText, null);

            bridge.onComplete();

            return;
        }

        String message = extractMessage(outputs);

        if (bridge.hasStarted()) {
            persistTurnToChatMemory(chat.getThreadId(), userMessageText, message);

            bridge.onComplete();

            return;
        }

        if (message == null || message.isBlank()) {
            logMissingReply(outputs, chat);

            message = "This workflow finished but produced no chat reply. If you wired "
                + "`Chat → Response to Chat Request` with `message: ${<aiTask>}`, change it to "
                + "`${<aiTask>.text}` (or the matching scalar field on the AI task's output) so the chat "
                + "trigger receives a string instead of the full task object.";
        }

        bridge.onEvent(message);

        AssistantMessage assistantMessage = new AssistantMessage();

        assistantMessage.setId(messageId);
        assistantMessage.setContent(message);

        subscriber.onNewMessage(assistantMessage);

        persistTurnToChatMemory(chat.getThreadId(), userMessageText, message);

        bridge.onComplete();
    }

    private void logMissingReply(@Nullable Object outputs, AiHubChat chat) {
        if (outputs == null) {
            log.warn(
                "Workflow chat {} produced null outputs and no streamed text. Workflow likely has no terminal task; "
                    + "add a streaming AI agent task or a `chat/responseToRequest` step.",
                chat.getId());

            return;
        }

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
            envelopePreview = envelopePreview.substring(0, 2000) + "...(truncated)";
        }

        Class<?> outputsClass = outputs.getClass();

        log.warn(
            "Workflow chat {} produced no extractable chat reply and no streamed text. outputsType={} "
                + "topLevelKeys={} envelopeType={} envelopePreview={}. If the workflow uses chat/responseToRequest "
                + "with `message: ${{<aiTask>}}`, change it to `${{<aiTask>.text}}` or the matching scalar field on "
                + "the AI task's output schema.",
            chat.getId(), outputsClass.getSimpleName(),
            outputs instanceof Map<?, ?> outputMap ? outputMap.keySet() : "(not a Map)",
            rawEnvelope == null ? "(null)" : rawEnvelope.getClass()
                .getName(),
            envelopePreview);
    }

    /**
     * Persists a user/assistant exchange to the AI Hub session store, keyed by the chat's thread id, so the chat thread
     * survives a page reload. The bridge bypasses the LLM agent's memory advisors, so without this a workflow chat
     * would have no persisted history. Best-effort: a store failure is logged and the turn continues.
     */
    private void persistTurnToChatMemory(
        String threadId, String userMessageText, @Nullable String assistantMessageText) {

        try {
            SessionRepository sessionRepository = sessionMemory.sessionRepository();

            if (Optional.ofNullable(sessionRepository.findById(threadId))
                .isEmpty()) {

                sessionRepository.save(
                    Session.builder()
                        .id(threadId)
                        .userId(AiHubSessionMemory.SESSION_USER_ID)
                        .createdAt(Instant.now())
                        .build());
            }

            sessionRepository.appendEvent(
                SessionEvent.builder()
                    .sessionId(threadId)
                    .message(new UserMessage(userMessageText))
                    .build());

            if (assistantMessageText != null && !assistantMessageText.isBlank()) {
                sessionRepository.appendEvent(
                    SessionEvent.builder()
                        .sessionId(threadId)
                        .message(new org.springframework.ai.chat.messages.AssistantMessage(assistantMessageText))
                        .build());
            }
        } catch (RuntimeException exception) {
            log.warn("Failed to persist workflow-chat turn to session memory for threadId {}", threadId, exception);
        }
    }

    /**
     * Pulls the assistant reply out of the workflow's synchronous result. Recognised shapes:
     * <ol>
     * <li>The {@link MetadataConstants#WEBHOOK_RESPONSE} envelope that {@code chat/responseToRequest} produces:
     * {@code body.message} for JSON bodies, the body itself for raw String bodies.</li>
     * <li>A map with a top-level {@code message} field.</li>
     * <li>A bare String.</li>
     * </ol>
     * Anything else returns {@code null}. The unwrap mirrors the webhook controller's {@code processWebhookResponse},
     * so a workflow that renders through the HTTP transport renders the same here.
     */
    private @Nullable String extractMessage(@Nullable Object outputs) {
        if (outputs instanceof Map<?, ?> outputMap) {
            String fromEnvelope = extractFromEnvelope(outputMap.get(MetadataConstants.WEBHOOK_RESPONSE));

            if (fromEnvelope != null) {
                return fromEnvelope;
            }

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

    private @Nullable String extractFromEnvelope(@Nullable Object webhookResponseValue) {
        if (webhookResponseValue instanceof String envelopeString) {
            return envelopeString.isBlank() ? null : envelopeString;
        }

        if (webhookResponseValue instanceof Map<?, ?> webhookResponseMap) {
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

            String envelopeCoerced = coerceToText(webhookResponseMap.get("message"));

            if (envelopeCoerced != null) {
                return envelopeCoerced;
            }

            if (webhookResponseMap.size() == 1) {
                Object onlyValue = webhookResponseMap.values()
                    .iterator()
                    .next();

                if (onlyValue instanceof String onlyString && !onlyString.isBlank()) {
                    return onlyString;
                }
            }

            return null;
        }

        if (webhookResponseValue != null) {
            return extractFromTypedWebhookResponse(webhookResponseValue);
        }

        return null;
    }

    /**
     * Extracts a reply from a non-Map, non-String value under {@link MetadataConstants#WEBHOOK_RESPONSE}, typically the
     * original {@code WebhookResponse} object when the storage round-trip preserved its type. Serialises it back to
     * JSON, re-parses it and runs the Map extraction on the result.
     */
    private @Nullable String extractFromTypedWebhookResponse(Object webhookResponseValue) {
        try {
            String json = jsonMapper.writeValueAsString(webhookResponseValue);
            Object reparsed = jsonMapper.readValue(json, Object.class);

            if (reparsed instanceof Map<?, ?> reparsedMap) {
                return extractFromEnvelope(reparsedMap);
            }

            if (reparsed instanceof String reparsedString && !reparsedString.isBlank()) {
                return reparsedString;
            }
        } catch (RuntimeException jsonException) {
            if (log.isDebugEnabled()) {
                Class<?> valueClass = webhookResponseValue.getClass();

                log.debug(
                    "Could not re-parse __webhookResponse value of type {} for chat reply extraction",
                    valueClass.getName(), jsonException);
            }
        }

        return null;
    }

    /**
     * Coerces a {@code message} field value to chat text: a String as is; for a Map, the first non-blank String among
     * {@code content}, {@code text}, {@code message}, {@code response} and {@code answer}; anything else {@code null}.
     */
    private static @Nullable String coerceToText(@Nullable Object value) {
        if (value instanceof String stringValue) {
            return stringValue;
        }

        if (value instanceof Map<?, ?> mapValue) {
            for (String key : List.of("content", "text", "message", "response", "answer")) {
                if (mapValue.get(key) instanceof String candidateString && !candidateString.isBlank()) {
                    return candidateString;
                }
            }
        }

        return null;
    }

    /**
     * Builds the {@link WebhookRequest} handed to the workflow executor. {@code body.content} carries {@code message},
     * {@code chatId} (the thread id), {@code conversationId} (stable per chat, for the workflow's own chat memory) and
     * an empty {@code attachments} list, which is the shape the chat trigger reads. The same values, except
     * attachments, are also put into {@code parameters} for triggers that read from there.
     */
    private static WebhookRequest buildWebhookRequest(String userMessage, AiHubChat chat) {
        String threadId = chat.getThreadId();
        String conversationId = conversationId(chat);

        Map<String, Object> bodyContent = new HashMap<>();

        bodyContent.put("message", userMessage);
        bodyContent.put("chatId", threadId);
        bodyContent.put("conversationId", conversationId);
        bodyContent.put("attachments", List.of());

        Map<String, List<String>> parameters = new HashMap<>();

        parameters.put("message", List.of(userMessage));
        parameters.put("chatId", List.of(threadId));
        parameters.put("conversationId", List.of(conversationId));

        return new WebhookRequest(
            Map.of(), parameters,
            new WebhookRequest.WebhookBodyImpl(bodyContent, ContentType.JSON, "application/json", null),
            WebhookMethod.POST);
    }

    private static String conversationId(AiHubChat chat) {
        return CONVERSATION_ID_PREFIX + chat.getThreadId();
    }

    /**
     * Returns the content of the most recent message with {@code Role.user}. Matches on the role rather than the
     * concrete message type, so any message subclass carrying the user role still works.
     */
    private static @Nullable String lastUserMessage(@Nullable List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        for (int index = messages.size() - 1; index >= 0; index--) {
            BaseMessage message = messages.get(index);

            if (message != null && Role.user.equals(message.getRole())) {
                return message.getContent();
            }
        }

        return null;
    }

    /**
     * Verifies the workflow is reachable (enabled and not deleted) before the executor and bridge are set up. Returns
     * the {@link WebhookTriggerFlags} on success; otherwise emits a friendly RUN_ERROR and returns {@code null}. Any
     * {@link RuntimeException} from the lookup counts as deleted, because the lookup spans several services and a
     * deleted project deployment, workflow or trigger each fail differently.
     */
    private @Nullable WebhookTriggerFlags lookupTriggerFlagsOrEmitError(
        RunAgentInput input, WorkflowExecutionId workflowExecutionId, String workflowExecutionIdString,
        AiHubChat chat, AgentSubscriber subscriber) {

        try {
            if (webhookWorkflowExecutor.isWorkflowDisabled(workflowExecutionId)) {
                metrics.recordUnreachable("disabled");

                emitError(input, subscriber, disabledChatErrorMessage(chat));

                return null;
            }

            return webhookWorkflowExecutor.getWebhookTriggerFlags(workflowExecutionId);
        } catch (RuntimeException exception) {
            log.warn(
                "Workflow chat trigger lookup failed for chat {} (workflowExecutionId={})", chat.getId(),
                workflowExecutionIdString, exception);

            metrics.recordUnreachable("deleted");

            emitError(input, subscriber, deletedWorkflowErrorMessage(chat));

            return null;
        }
    }

    /**
     * Applies the per-chat rate limit. Returns {@code true} when the turn is admitted; otherwise records the rejection,
     * emits a RUN_ERROR with the guard's user-facing message and returns {@code false}.
     */
    private boolean tryAdmitOrEmitError(RunAgentInput input, AiHubChat chat, AgentSubscriber subscriber) {
        WorkflowChatGuard.AdmissionResult admission = guard.tryAdmit(chat.getId());

        if (admission instanceof WorkflowChatGuard.AdmissionResult.Admit) {
            return true;
        }

        recordTurn("rate_limited", chat);

        String message = admission.userFacingMessage();

        emitError(input, subscriber, message != null ? message : "Workflow chat turn rejected");

        return false;
    }

    /**
     * Emits {@code RUN_STARTED}, which must be the first event the client sees, through both {@code onEvent} (which
     * writes it to the stream) and the typed dispatch.
     */
    private static void emitRunStarted(RunAgentInput input, AgentSubscriber subscriber) {
        RunStartedEvent event = new RunStartedEvent();

        event.setThreadId(input.threadId());
        event.setRunId(input.runId());

        subscriber.onEvent(event);
        subscriber.onRunStartedEvent(event);
    }

    /**
     * Emits {@code RUN_ERROR} and finalizes the run, so every early exit closes the stream and frees the chat.
     */
    private void emitError(RunAgentInput input, AgentSubscriber subscriber, String message) {
        RunErrorEvent event = new RunErrorEvent();

        event.setError(message);

        subscriber.onEvent(event);
        subscriber.onRunErrorEvent(event);

        finalizeRun(input, subscriber);
    }

    /**
     * Fires {@code subscriber.onRunFinalized}, which completes the turn's run in {@link InFlightAiHubRunRegistry}: it
     * closes the client stream and releases the chat for its next turn. Every completion path goes through here.
     */
    private void finalizeRun(RunAgentInput input, AgentSubscriber subscriber) {
        try {
            subscriber.onRunFinalized(new AgentSubscriberParams(input.messages(), state, this, input));
        } catch (RuntimeException exception) {
            log.warn(
                "subscriber.onRunFinalized threw for thread {} (run {}); the stream may stay open", input.threadId(),
                input.runId(), exception);
        }
    }
}
