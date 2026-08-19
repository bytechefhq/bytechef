/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifact;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatMessage;
import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatPatch;
import com.bytechef.ee.ai.hub.chat.AiHubChatStatus;
import com.bytechef.ee.ai.hub.chat.TitleGenerationService;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.security.WorkspaceAccessGuard;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.ZoneOffset;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for AI Hub aiHubChats. Replaces the prior REST {@code TaskApiController} — the
 * {@link AiHubChatService} / {@link AiHubChatArtifactService} / {@link TitleGenerationService} contracts are unchanged;
 * only the transport differs.
 *
 * <p>
 * Authorization mirrors the REST controller exactly: every operation requires {@code isAuthenticated()} plus the
 * {@link WorkspaceAccessGuard} membership gate. Service-layer ownership checks remain the authoritative defence — a
 * caller who is a workspace member but not the chat owner still gets a 403-equivalent error from the service.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@PreAuthorize("isAuthenticated()")
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class AiHubChatGraphQlController {

    private static final Logger log = LoggerFactory.getLogger(AiHubChatGraphQlController.class);

    private final AiHubChatArtifactService chatArtifactService;
    private final AiHubChatService chatService;
    private final TitleGenerationService titleGenerationService;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public AiHubChatGraphQlController(
        AiHubChatArtifactService chatArtifactService,
        AiHubChatService chatService,
        TitleGenerationService titleGenerationService, UserService userService, WorkspaceFacade workspaceFacade) {

        this.chatArtifactService = chatArtifactService;
        this.chatService = chatService;
        this.titleGenerationService = titleGenerationService;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    @QueryMapping
    public List<AiHubChat> aiHubChats(
        @Argument long workspaceId, @Argument int environment,
        @Argument @Nullable AiHubChatStatus status) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        AiHubChatStatus effectiveStatus =
            status == null ? AiHubChatStatus.ACTIVE : status;

        return chatService.list(workspaceId, userId, environment, effectiveStatus);
    }

    @QueryMapping
    public List<AiHubChatMessage>
        aiHubChatMessages(@Argument long workspaceId, @Argument long id) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        return chatService.loadMessages(id, workspaceId, userId);
    }

    @QueryMapping
    public List<AiHubChatArtifact> aiHubChatArtifactsByAiHubChat(
        @Argument long workspaceId, @Argument long id) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        return chatArtifactService.listByChat(id, workspaceId, userId);
    }

    @MutationMapping
    public AiHubChat createAiHubChat(
        @Argument long workspaceId, @Argument int environment, @Argument String threadId) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        return chatService.create(workspaceId, userId, environment, threadId);
    }

    @MutationMapping
    public AiHubChat createWorkflowChatAiHubChat(
        @Argument long workspaceId, @Argument int environment, @Argument String workflowExecutionId,
        @Argument long projectDeploymentId, @Argument @Nullable String title) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        // Always-new semantics: every call inserts a fresh chat row with a UUID-suffixed threadId.
        // Past aiHubChats bound to the same workflow remain reachable through the
        // aiHubChats list rather
        // than being restored on re-click. The title is stamped onto the new row.
        return chatService.createWorkflowChat(
            workspaceId, userId, environment, workflowExecutionId, projectDeploymentId, title);
    }

    @MutationMapping
    public AiHubChat createAgentChatAiHubChat(
        @Argument long workspaceId, @Argument int environment, @Argument String workflowExecutionId,
        @Argument long projectDeploymentId, @Argument @Nullable String title) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        // Same always-new semantics as the workflow-chat mutation above; only the persisted kind differs. Kept as its
        // own mutation rather than a flag on that one because the caller already knows which cascade the user picked,
        // and a boolean argument would let a client silently create a mislabelled row.
        return chatService.createAgentChat(
            workspaceId, userId, environment, workflowExecutionId, projectDeploymentId, title);
    }

    @MutationMapping
    public AiHubChat
        updateAiHubChat(@Argument AiHubChatPatchInput input) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, input.workspaceId());

        AiHubChatPatch patch = new AiHubChatPatch(
            input.title(), input.lastPreview(), input.messageCount(), input.status());

        return chatService.patch(input.id(), input.workspaceId(), userId, patch);
    }

    @MutationMapping
    public AiHubChat
        generateAiHubChatTitle(@Argument long workspaceId, @Argument long id) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        AiHubChat existing = chatService.getById(id, workspaceId, userId);

        if (!existing.isAutoTitled()) {
            // Locked — title was either set by the LLM on a previous regeneration or explicitly renamed by
            // the user. Short-circuit before loading messages or calling the LLM. The "auto-titled" flag
            // (versus the previous "title is empty" check) lets workflow chats — which start with a
            // label-based title that's still eligible for LLM regeneration — get a more meaningful title
            // after a few turns instead of being stuck on the project-name pattern forever.
            return existing;
        }

        List<AiHubChatMessage> messages = chatService.loadMessages(id, workspaceId, userId);

        String title = generateTitleForEnvironment(existing.getEnvironment(), messages);

        if (title.isEmpty()) {
            // The model returned a blank or over-length title. Reuse the row we already loaded above
            // for the idempotency check rather than issuing a second getById round-trip.
            return existing;
        }

        try {
            return chatService.patch(
                id, workspaceId, userId, new AiHubChatPatch(title, null, null, null));
        } catch (NotFoundException notFound) {
            // Race: the chat was deleted between the getById/loadMessages above and this patch.
            // Title generation is fire-and-forget on the client (one per turn while untitled), so a quick
            // delete after sending a message lands here. Surface as a benign no-op — the deleted row's
            // pre-delete snapshot is still a valid AiHubChat shape for the GraphQL response, and the
            // client has already removed the chat from its local state so this body is never
            // rendered. Logging at INFO so the race stays visible without spamming WARN/ERROR.
            log.info(
                "generateAiHubChatTitle no-op: chat {} was deleted during title generation",
                id);

            return existing;
        }
    }

    private String generateTitleForEnvironment(Environment environment, List<AiHubChatMessage> messages) {
        Environment previousEnvironment = EnvironmentContext.fetchCurrentEnvironment();

        EnvironmentContext.set(environment);

        try {
            return titleGenerationService.generateTitle(messages);
        } finally {
            if (previousEnvironment == null) {
                EnvironmentContext.clear();
            } else {
                EnvironmentContext.set(previousEnvironment);
            }
        }
    }

    @MutationMapping
    public boolean deleteAiHubChat(@Argument long workspaceId, @Argument long id) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        chatService.delete(id, workspaceId, userId);

        return true;
    }

    @MutationMapping
    public int
        bulkArchiveWorkflowChatAiHubChats(@Argument long workspaceId, @Argument int environment) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        // Service-layer per-row ownership re-check matches the rest of the chat API; we double-gate on
        // the workspace-membership guard here AND the per-chat ownership check inside the loop. Returning
        // the count rather than the affected row ids keeps the response small (no per-row latency the client has
        // to render); the sidebar refetches the aiHubChats query after this fires anyway.
        return chatService.bulkArchiveWorkflowChatAiHubChats(workspaceId, userId, environment);
    }

    @MutationMapping
    public boolean cancelWorkflowChatTurn(@Argument long workspaceId, @Argument long id) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        return chatService.cancelWorkflowChatTurn(id, workspaceId, userId);
    }

    @MutationMapping
    public boolean cancelAiHubRun(@Argument long workspaceId, @Argument long id, @Argument String runId) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        return chatService.cancelAiHubRun(id, workspaceId, userId, runId);
    }

    @MutationMapping
    public int truncateAiHubChatMessages(
        @Argument long workspaceId, @Argument long id, @Argument int fromMessageIndex) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        return chatService.truncateMessagesFrom(id, workspaceId, userId, fromMessageIndex);
    }

    @MutationMapping
    public boolean appendAiHubChatAssistantMessage(
        @Argument long workspaceId, @Argument long id, @Argument String content) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        chatService.appendAssistantMessage(id, workspaceId, userId, content);

        return true;
    }

    /**
     * Resolves the owning workspace id for a chat, read straight off the loaded row's {@code workspace_id} column, so
     * the sidebar listing costs no extra query per row. The schema declares the field non-null and a chat with no
     * workspace is unreachable through every workspace-scoped path, so a null fails loudly here — the same contract
     * {@code AiHubChatService.getWorkspaceId} enforces for callers that only have a chat id.
     */
    @SchemaMapping(typeName = "AiHubChat", field = "workspaceId")
    public long chatWorkspaceId(AiHubChat chat) {
        Long workspaceId = chat.getWorkspaceId();

        if (workspaceId == null) {
            throw new NotFoundException("No workspace for ai_hub_chat id=" + chat.getId());
        }

        return workspaceId;
    }

    @SchemaMapping(typeName = "AiHubChat", field = "status")
    public String chatStatus(AiHubChat chat) {
        return chat.getStatus()
            .name();
    }

    @SchemaMapping(typeName = "AiHubChat", field = "kind")
    public String chatKind(AiHubChat chat) {
        return chat.getKind()
            .name();
    }

    @SchemaMapping(typeName = "AiHubChat", field = "createdAt")
    @Nullable
    public Long chatCreatedAt(AiHubChat chat) {
        return chat.getCreatedAt() == null ? null
            : chat.getCreatedAt()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiHubChat", field = "updatedAt")
    @Nullable
    public Long chatUpdatedAt(AiHubChat chat) {
        return chat.getUpdatedAt() == null ? null
            : chat.getUpdatedAt()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiHubChatMessage", field = "timestamp")
    public long messageTimestamp(AiHubChatMessage message) {
        return message.timestamp()
            .toEpochMilli();
    }

    public record AiHubChatPatchInput(
        long id, long workspaceId, @Nullable String title, @Nullable String lastPreview,
        @Nullable Integer messageCount, @Nullable AiHubChatStatus status) {
    }
}
