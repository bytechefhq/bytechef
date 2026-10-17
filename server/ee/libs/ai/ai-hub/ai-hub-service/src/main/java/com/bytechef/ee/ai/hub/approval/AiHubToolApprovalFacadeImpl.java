/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.ee.ai.hub.agent.AiHubToolCallbackWrappers;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.exception.ForbiddenException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.ee.ai.hub.util.Source;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Implementation of {@link AiHubToolApprovalFacade}. Not {@code @Transactional} at class level. {@link #resolve} runs
 * in three separate steps, each committed independently, rather than one wrapping transaction:
 * <ol>
 * <li>{@link #expireIfPastDeadline}, outside any transaction opened by this facade, so the {@code EXPIRED} write
 * survives the {@code ConflictException} the same step throws (a real transaction manager rolls back on any
 * {@code RuntimeException} escaping the callback).</li>
 * <li>{@link #persistDecision}: the resolver's decision (who, when, comment, APPROVED/REJECTED) is saved and committed
 * inside its own {@link TransactionTemplate} block BEFORE the tool ever runs. Two concurrent resolves both reading
 * {@code PENDING} race here: the loser's save fails the {@link AiHubToolApproval}'s optimistic {@code @Version} check,
 * surfaced as {@link OptimisticLockingFailureException} and translated to the same not-found-shaped failure every other
 * enumeration-safe failure uses.</li>
 * <li>{@link #executeAndPersistOutcome}: runs the approved tool call AFTER the decision has committed, outside any
 * transaction, so a slow external API never holds a pooled DB connection open. A thrown execution flips the row to
 * {@code FAILED} and persists that as a second, independent write.</li>
 * </ol>
 *
 * <p>
 * Only the chat owner resolves an approval (spec D4): a tenant ADMIN who may manage the chat but does not own it is
 * refused.
 * </p>
 *
 * <p>
 * Only the mode's PINNED tools are searched when re-finding an approved call's tool ({@link AiHubPinnedToolCallbacks}).
 * The approved call executes, and the continuation turn is dispatched, as the REQUESTER ({@code requestedByUserId}),
 * never the resolver: the approver is recorded ({@code decidedByUserId}) and never impersonated. When that identity
 * cannot be established (no rehydrator, or the user no longer exists) the row is marked {@code FAILED} and no
 * continuation starts, rather than falling back to the resolver's ambient context.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnAiHub
public class AiHubToolApprovalFacadeImpl implements AiHubToolApprovalFacade {

    private static final Logger log = LoggerFactory.getLogger(AiHubToolApprovalFacadeImpl.class);

    private static final String BUILD_MODE = "BUILD";

    private final AiHubToolApprovalService approvalService;
    private final AiHubChatService chatService;
    private final UserService userService;
    private final ObjectProvider<AiHubPinnedToolCallbacks> pinnedToolCallbacksProvider;
    private final ObjectProvider<AiHubContinuationStarter> continuationStarterProvider;
    private final @Nullable SecurityContextRehydrator securityContextRehydrator;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Autowired
    @SuppressFBWarnings("EI")
    public AiHubToolApprovalFacadeImpl(
        AiHubToolApprovalService approvalService, AiHubChatService chatService, UserService userService,
        ObjectProvider<AiHubPinnedToolCallbacks> pinnedToolCallbacksProvider,
        ObjectProvider<AiHubContinuationStarter> continuationStarterProvider,
        @Nullable SecurityContextRehydrator securityContextRehydrator, PlatformTransactionManager transactionManager) {

        this(
            approvalService, chatService, userService, pinnedToolCallbacksProvider, continuationStarterProvider,
            securityContextRehydrator, transactionManager, Clock.systemUTC());
    }

    @SuppressFBWarnings("EI")
    AiHubToolApprovalFacadeImpl(
        AiHubToolApprovalService approvalService, AiHubChatService chatService, UserService userService,
        ObjectProvider<AiHubPinnedToolCallbacks> pinnedToolCallbacksProvider,
        ObjectProvider<AiHubContinuationStarter> continuationStarterProvider,
        @Nullable SecurityContextRehydrator securityContextRehydrator, PlatformTransactionManager transactionManager,
        Clock clock) {

        this.approvalService = approvalService;
        this.chatService = chatService;
        this.userService = userService;
        this.pinnedToolCallbacksProvider = pinnedToolCallbacksProvider;
        this.continuationStarterProvider = continuationStarterProvider;
        this.securityContextRehydrator = securityContextRehydrator;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Override
    public List<AiHubToolApproval> list(long workspaceId, long chatId) {
        chatService.getViewable(chatId, workspaceId, currentUserId());

        return approvalService.list(chatId);
    }

    @Override
    public Resolution resolve(long workspaceId, long approvalId, boolean approved, @Nullable String comment) {
        long currentUserId = currentUserId();

        AiHubToolApproval approval = approvalService.get(approvalId);
        AiHubChat chat = getManageableChat(approval, workspaceId, currentUserId);

        if (chat.getUserId() != currentUserId) {
            throw new ForbiddenException("Only the chat owner can resolve a tool approval");
        }

        if (approval.getStatus() != AiHubToolApproval.Status.PENDING) {
            throw new ConflictException("Approval " + approvalId + " is already " + approval.getStatus());
        }

        expireIfPastDeadline(approval);

        AiHubToolApproval decided = persistDecision(approval, currentUserId, approved, comment);

        String toolResult = approved ? executeAndPersistOutcome(decided, chat, workspaceId) : null;

        String runId = startContinuation(decided, chat, workspaceId, toolResult);

        return new Resolution(decided, runId != null, runId);
    }

    private long currentUserId() {
        User currentUser = userService.getCurrentUser();

        return currentUser.getId();
    }

    private AiHubChat getManageableChat(AiHubToolApproval approval, long workspaceId, long currentUserId) {
        try {
            return chatService.getManageable(approval.getChatId(), workspaceId, currentUserId);
        } catch (NotFoundException exception) {
            throw new NotFoundException("AiHubToolApproval", approval.getId());
        }
    }

    private void expireIfPastDeadline(AiHubToolApproval approval) {
        if (!approval.isExpired(Instant.now(clock))) {
            return;
        }

        approval.setStatus(AiHubToolApproval.Status.EXPIRED);

        approvalService.save(approval);

        throw new ConflictException("Approval " + approval.getId() + " has expired");
    }

    private AiHubToolApproval persistDecision(
        AiHubToolApproval approval, long currentUserId, boolean approved, @Nullable String comment) {

        approval.setDecidedByUserId(currentUserId);
        approval.setDecidedAt(Instant.now(clock));
        approval.setComment(comment);
        approval.setStatus(approved ? AiHubToolApproval.Status.APPROVED : AiHubToolApproval.Status.REJECTED);

        try {
            return transactionTemplate.execute(status -> approvalService.save(approval));
        } catch (OptimisticLockingFailureException exception) {
            throw new NotFoundException("AiHubToolApproval", approval.getId());
        }
    }

    private @Nullable String executeAndPersistOutcome(AiHubToolApproval decided, AiHubChat chat, long workspaceId) {
        String toolResult = execute(decided, chat, workspaceId);

        if (decided.getStatus() == AiHubToolApproval.Status.FAILED) {
            approvalService.save(decided);
        }

        return toolResult;
    }

    private @Nullable String execute(AiHubToolApproval approval, AiHubChat chat, long workspaceId) {
        Optional<ToolCallback> callbackOptional = findCallback(approval);

        if (callbackOptional.isEmpty()) {
            approval.setStatus(AiHubToolApproval.Status.FAILED);
            approval.setExecutionError("Tool " + approval.getToolName() + " is no longer available in this chat");

            return null;
        }

        long requesterUserId = approval.getRequestedByUserId();
        SecurityContextRehydrator rehydrator = securityContextRehydrator;

        if (rehydrator == null || isUnknownUser(requesterUserId)) {
            approval.setStatus(AiHubToolApproval.Status.FAILED);
            approval.setExecutionError(
                "Tool " + approval.getToolName() + " cannot run: the security context of user " + requesterUserId
                    + " could not be established");

            return null;
        }

        ToolCallback resolvedCallback = callbackOptional.get();

        return rehydrator.withUserSecurityContext(
            requesterUserId, () -> call(approval, chat, workspaceId, resolvedCallback, rehydrator));
    }

    private boolean isUnknownUser(long userId) {
        Optional<User> userOptional = userService.fetchUser(userId);

        return userOptional.isEmpty();
    }

    private @Nullable String call(
        AiHubToolApproval approval, AiHubChat chat, long workspaceId, ToolCallback resolvedCallback,
        SecurityContextRehydrator rehydrator) {

        ToolCallback callback = AiHubToolCallbackWrappers.wrap(resolvedCallback, rehydrator);
        ToolContext toolContext = new ToolContext(toolContextFor(approval, chat, workspaceId));

        try {
            return callback.call(approval.getArguments(), toolContext);
        } catch (RuntimeException exception) {
            approval.setStatus(AiHubToolApproval.Status.FAILED);
            approval.setExecutionError(
                exception.getMessage() == null ? exception.toString() : exception.getMessage());

            return null;
        }
    }

    private Optional<ToolCallback> findCallback(AiHubToolApproval approval) {
        AiHubPinnedToolCallbacks pinnedToolCallbacks = pinnedToolCallbacksProvider.getIfAvailable();

        if (pinnedToolCallbacks == null) {
            return Optional.empty();
        }

        Mode mode = BUILD_MODE.equals(approval.getMode()) ? Mode.BUILD : Mode.ASK;

        return pinnedToolCallbacks.pinnedToolCallbacks(mode)
            .stream()
            .filter(callback -> hasName(callback, approval.getToolName()))
            .findFirst();
    }

    private static boolean hasName(ToolCallback callback, String toolName) {
        ToolDefinition toolDefinition = callback.getToolDefinition();

        return toolName.equals(toolDefinition.name());
    }

    private static Map<String, Object> toolContextFor(AiHubToolApproval approval, AiHubChat chat, long workspaceId) {
        long environmentId = approval.getEnvironment();
        long requesterUserId = approval.getRequestedByUserId();
        SecurityContext securityContext = SecurityContextHolder.getContext();

        Map<String, Object> toolContext = new HashMap<>();

        toolContext.put(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId);
        toolContext.put(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, environmentId);
        toolContext.put(AutomationToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, requesterUserId);
        toolContext.put(
            AutomationToolInvocationContext.TOOL_CONTEXT_SOURCE_ORDINAL_KEY, Source.AI_HUB.toAgentSourceOrdinal());
        toolContext.put(AutomationToolInvocationContext.TOOL_CONTEXT_THREAD_ID_KEY, chat.getThreadId());

        AgentToolInvocationContext agentToolInvocationContext = AgentToolInvocationContext.builder()
            .workspaceId(workspaceId)
            .userId(requesterUserId)
            .environmentId(environmentId)
            .conversationId(chat.getThreadId())
            .tenantId(TenantContext.getCurrentTenantId())
            .authentication(securityContext.getAuthentication())
            .llmProvider(approval.getLlmProvider())
            .llmModel(approval.getLlmModel())
            .build();

        toolContext.putAll(agentToolInvocationContext.toToolContext());
        toolContext.put(AiHubApprovalGateToolCallback.TOOL_CONTEXT_MODE_KEY, approval.getMode());

        return toolContext;
    }

    private @Nullable String startContinuation(
        AiHubToolApproval approval, AiHubChat chat, long workspaceId, @Nullable String toolResult) {

        AiHubContinuationStarter continuationStarter = continuationStarterProvider.getIfAvailable();

        if (continuationStarter == null) {
            return null;
        }

        long requesterUserId = approval.getRequestedByUserId();
        SecurityContextRehydrator rehydrator = securityContextRehydrator;

        if (rehydrator == null || isUnknownUser(requesterUserId)) {
            log.warn(
                "Not starting the continuation turn for approval id={}: the security context of requesting user {}"
                    + " could not be established, and the turn must not run as the approver",
                approval.getId(), requesterUserId);

            return null;
        }

        String message = continuationText(approval, toolResult);

        Optional<String> runIdOptional = rehydrator.withUserSecurityContext(
            requesterUserId, () -> continuationStarter.startContinuation(approval, chat, workspaceId, message));

        return runIdOptional.orElse(null);
    }

    static String continuationText(AiHubToolApproval approval, @Nullable String toolResult) {
        String decision = switch (approval.getStatus()) {
            case APPROVED -> "approved and executed";
            case REJECTED -> "rejected";
            case FAILED -> "approved but the tool failed";
            default -> {
                AiHubToolApproval.Status status = approval.getStatus();

                yield status.name()
                    .toLowerCase(Locale.ROOT);
            }
        };

        StringBuilder text = new StringBuilder()
            .append(AiHubContinuationStarter.CONTINUATION_PREFIX)
            .append(approval.getId())
            .append(' ')
            .append(decision)
            .append("]\nTool: ")
            .append(approval.getToolName());

        String comment = approval.getComment();

        if (comment != null && !comment.isBlank()) {
            text.append("\nComment: ")
                .append(comment);
        }

        if (toolResult != null) {
            text.append("\nResult:\n")
                .append(toolResult);
        }

        if (approval.getExecutionError() != null) {
            text.append("\nError: ")
                .append(approval.getExecutionError());
        }

        return text.toString();
    }
}
