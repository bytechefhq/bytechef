/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.ai.llm.tool.DelegatingToolCallback;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Decorator that intercepts a gated tool call before it executes: instead of running {@code delegate}, it records a
 * {@code PENDING} {@link AiHubToolApproval} row and returns a structured envelope so the chat surface can render an
 * approval card. Whether a given tool call is gated at all is decided per call by {@link AiHubToolApprovalPolicy}, so
 * the same wrapper instance can pass some calls straight through and gate others. Every gated row is recorded as a
 * {@code CATALOG} tool.
 *
 * <p>
 * A call with no resolvable {@link AgentToolInvocationContext} (missing thread id, workspace id, or user id) passes
 * straight through to {@code delegate}. A gated call whose context is complete but whose {@link AiHubChat} no longer
 * exists, is in {@code DELETED} status or is being deleted (its thread is retired) is refused without recording an
 * approval: the hub persists the chat before any run starts, so a missing chat means it was deleted.
 * </p>
 *
 * <p>
 * Fails closed: when the {@code PENDING} row cannot be persisted, the tool is refused rather than executed. Persisting
 * the row and building the response envelope are two separate failure domains: if the row is persisted but the envelope
 * then fails to build, the row is immediately marked {@code SUPERSEDED} rather than left {@code PENDING}, so a row
 * nobody ever saw an approval card for does not block every later gated call in the chat with {@code DEFERRED_RESULT}.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubApprovalGateToolCallback implements DelegatingToolCallback {

    public static final String ENVELOPE_KIND = "tool-approval-request";
    public static final String TOOL_CONTEXT_MODE_KEY = "bytechef.aiHub.mode";

    private static final Logger log = LoggerFactory.getLogger(AiHubApprovalGateToolCallback.class);

    private static final String DEFERRED_RESULT =
        "{\"deferred\":true,\"reason\":\"Another tool call is awaiting approval. Retry after it is resolved.\"}";
    private static final String REFUSED_RESULT =
        "{\"error\":\"approval could not be recorded; the tool was not executed\"}";
    private static final String CHAT_MISSING_RESULT =
        "{\"error\":\"the chat no longer exists; the tool was not executed\"}";

    private final ToolCallback delegate;
    private final AiHubToolApprovalPolicy policy;
    private final AiHubToolApprovalService approvalService;
    private final AiHubChatService chatService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubApprovalGateToolCallback(
        ToolCallback delegate, AiHubToolApprovalPolicy policy, AiHubToolApprovalService approvalService,
        AiHubChatService chatService) {

        this.delegate = delegate;
        this.policy = policy;
        this.approvalService = approvalService;
        this.chatService = chatService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolCallback getDelegate() {
        return delegate;
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null) {
            return delegate.call(toolInput, toolContext);
        }

        String threadId = invocationContext.conversationId();
        Long workspaceId = invocationContext.workspaceId();
        Long userId = invocationContext.userId();

        if (threadId == null || workspaceId == null || userId == null) {
            return delegate.call(toolInput, toolContext);
        }

        ToolDefinition toolDefinition = getToolDefinition();

        String toolName = toolDefinition.name();

        AiHubToolApprovalPolicy.Decision decision = policy.decide(workspaceId);

        if (!decision.isGated(toolName)) {
            return delegate.call(toolInput, toolContext);
        }

        Optional<AiHubChat> chatOptional = chatService.findActiveByThreadId(threadId);

        if (chatOptional.isEmpty()) {
            log.warn("Refusing gated tool={} for thread={}: the chat no longer exists", toolName, threadId);

            return CHAT_MISSING_RESULT;
        }

        AiHubChat chat = chatOptional.get();

        Optional<AiHubToolApproval> pendingApproval = approvalService.findPending(chat.getId());

        if (pendingApproval.isPresent()) {
            return DEFERRED_RESULT;
        }

        AiHubToolApproval approval;

        try {
            approval = approvalService.createPending(
                toApproval(chat, userId, toolName, toolInput, invocationContext, toolContext));
        } catch (RuntimeException exception) {
            log.warn("Could not record a tool approval for tool={} chat={}", toolName, chat.getId(), exception);

            return REFUSED_RESULT;
        }

        try {
            return JsonUtils.write(toEnvelope(approval));
        } catch (RuntimeException exception) {
            log.warn(
                "Tool approval id={} for tool={} chat={} was persisted but its response envelope could not be "
                    + "built; marking it SUPERSEDED so an unseen row does not block the chat for its full expiry",
                approval.getId(), toolName, chat.getId(), exception);

            approval.setStatus(AiHubToolApproval.Status.SUPERSEDED);

            try {
                approvalService.save(approval);
            } catch (RuntimeException saveException) {
                log.warn(
                    "Tool approval id={} for tool={} chat={} could not be marked SUPERSEDED; it may remain PENDING",
                    approval.getId(), toolName, chat.getId(), saveException);
            }

            return REFUSED_RESULT;
        }
    }

    private static AiHubToolApproval toApproval(
        AiHubChat chat, long userId, String toolName, @Nullable String toolInput,
        AgentToolInvocationContext invocationContext, @Nullable ToolContext toolContext) {

        AiHubToolApproval approval = new AiHubToolApproval();

        approval.setChatId(chat.getId());
        approval.setThreadId(chat.getThreadId());
        approval.setRequestedByUserId(userId);
        approval.setToolKind(AiHubToolApproval.ToolKind.CATALOG);
        approval.setToolName(toolName);
        approval.setArguments(toolInput == null ? "{}" : toolInput);
        approval.setMode(readMode(toolContext));
        approval.setEnvironment((int) chat.getEnvironmentId());
        approval.setLlmProvider(invocationContext.llmProvider());
        approval.setLlmModel(invocationContext.llmModel());

        return approval;
    }

    private static String readMode(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return "ASK";
        }

        Map<String, Object> context = toolContext.getContext();

        Object mode = context.get(TOOL_CONTEXT_MODE_KEY);

        return mode instanceof String stringMode && !stringMode.isBlank() ? stringMode : "ASK";
    }

    private static Map<String, Object> toEnvelope(AiHubToolApproval approval) {
        Map<String, Object> envelope = new LinkedHashMap<>();

        envelope.put("kind", ENVELOPE_KIND);
        envelope.put("approvalId", approval.getId());
        envelope.put("toolName", approval.getToolName());
        envelope.put("componentName", approval.getComponentName());
        envelope.put("arguments", JsonUtils.read(approval.getArguments(), Map.class));
        envelope.put("expiresAt", approval.getExpiresAt() == null ? null : String.valueOf(approval.getExpiresAt()));
        envelope.put("awaitingApproval", true);

        return envelope;
    }
}
