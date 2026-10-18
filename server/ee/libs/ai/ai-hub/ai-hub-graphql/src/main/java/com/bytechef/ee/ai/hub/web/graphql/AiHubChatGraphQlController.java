/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifact;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatMessage;
import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatPatch;
import com.bytechef.ee.ai.hub.chat.AiHubChatStatus;
import com.bytechef.ee.ai.hub.chat.TitleGenerationService;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
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
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for AI Hub chats.
 *
 * <p>
 * Every operation requires {@code isAuthenticated()} plus the workspace VIEWER role. Service-layer chat access checks
 * remain the authoritative defence: a chat the caller may not view is reported as not found, never as forbidden, so the
 * response does not reveal that the chat exists.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@PreAuthorize("isAuthenticated()")
@ConditionalOnEEVersion
@ConditionalOnAiHub
public class AiHubChatGraphQlController {

    private static final Logger log = LoggerFactory.getLogger(AiHubChatGraphQlController.class);

    private final AiHubChatArtifactService chatArtifactService;
    private final AiHubChatService chatService;
    private final PermissionService permissionService;
    private final TitleGenerationService titleGenerationService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public AiHubChatGraphQlController(
        AiHubChatArtifactService chatArtifactService, AiHubChatService chatService,
        PermissionService permissionService, TitleGenerationService titleGenerationService,
        UserService userService) {

        this.chatArtifactService = chatArtifactService;
        this.chatService = chatService;
        this.permissionService = permissionService;
        this.titleGenerationService = titleGenerationService;
        this.userService = userService;
    }

    @QueryMapping
    public List<AiHubChat> aiHubChats(
        @Argument long workspaceId, @Argument int environment, @Argument @Nullable AiHubChatStatus status) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        AiHubChatStatus effectiveStatus = status == null ? AiHubChatStatus.ACTIVE : status;

        return chatService.list(workspaceId, currentUserId(), environment, effectiveStatus);
    }

    @QueryMapping
    public List<AiHubChatMessage> aiHubChatMessages(@Argument long workspaceId, @Argument long id) {
        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        return chatService.loadMessages(id, workspaceId, currentUserId());
    }

    @QueryMapping
    public List<AiHubChatArtifact> aiHubChatArtifactsByAiHubChat(@Argument long workspaceId, @Argument long id) {
        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        return chatArtifactService.listByChat(id, workspaceId, currentUserId());
    }

    @MutationMapping
    public AiHubChat createAiHubChat(
        @Argument long workspaceId, @Argument int environment, @Argument String threadId) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        return chatService.create(workspaceId, currentUserId(), environment, threadId);
    }

    @MutationMapping
    public AiHubChat updateAiHubChat(@Argument AiHubChatPatchInput input) {
        AiHubWorkspaceAccess.verifyViewer(permissionService, input.workspaceId());

        AiHubChatPatch patch = new AiHubChatPatch(
            input.title(), input.lastPreview(), input.messageCount(), input.status());

        return chatService.patch(input.id(), input.workspaceId(), currentUserId(), patch);
    }

    @MutationMapping
    public AiHubChat generateAiHubChatTitle(@Argument long workspaceId, @Argument long id) {
        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long userId = currentUserId();

        AiHubChat existing = chatService.getById(id, workspaceId, userId);

        if (!existing.isAutoTitled()) {
            return existing;
        }

        List<AiHubChatMessage> messages = chatService.loadMessages(id, workspaceId, userId);

        String title = generateTitleForEnvironment(existing.getEnvironment(), messages);

        if (title.isEmpty()) {
            return existing;
        }

        try {
            return chatService.patch(id, workspaceId, userId, new AiHubChatPatch(title, null, null, null));
        } catch (NotFoundException notFoundException) {
            log.info("generateAiHubChatTitle no-op: chat {} was deleted during title generation", id);

            return existing;
        }
    }

    @MutationMapping
    public boolean deleteAiHubChat(@Argument long workspaceId, @Argument long id) {
        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        chatService.delete(id, workspaceId, currentUserId());

        return true;
    }

    @MutationMapping
    public boolean cancelAiHubRun(
        @Argument long workspaceId, @Argument long id, @Argument @Nullable String runId) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        return chatService.cancelAiHubRun(id, workspaceId, currentUserId(), runId);
    }

    @MutationMapping
    public int truncateAiHubChatMessages(
        @Argument long workspaceId, @Argument long id, @Argument int fromMessageIndex) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        return chatService.truncateMessagesFrom(id, workspaceId, currentUserId(), fromMessageIndex);
    }

    @MutationMapping
    public boolean appendAiHubChatAssistantMessage(
        @Argument long workspaceId, @Argument long id, @Argument String content) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        chatService.appendAssistantMessage(id, workspaceId, currentUserId(), content);

        return true;
    }

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

    @SchemaMapping(typeName = "AiHubChat", field = "createdAt")
    public @Nullable Long chatCreatedAt(AiHubChat chat) {
        return chat.getCreatedAt() == null ? null
            : chat.getCreatedAt()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiHubChat", field = "updatedAt")
    public @Nullable Long chatUpdatedAt(AiHubChat chat) {
        return chat.getUpdatedAt() == null ? null
            : chat.getUpdatedAt()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiHubChat", field = "ownerUserId")
    public long chatOwnerUserId(AiHubChat chat) {
        return chat.getUserId();
    }

    @SchemaMapping(typeName = "AiHubChat", field = "isOwner")
    public boolean chatIsOwner(AiHubChat chat) {
        return chat.getUserId() == currentUserId();
    }

    @SchemaMapping(typeName = "AiHubChatMessage", field = "timestamp")
    public long messageTimestamp(AiHubChatMessage message) {
        return message.timestamp()
            .toEpochMilli();
    }

    private long currentUserId() {
        return userService.getCurrentUser()
            .getId();
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

    public record AiHubChatPatchInput(
        long id, long workspaceId, @Nullable String title, @Nullable String lastPreview,
        @Nullable Integer messageCount, @Nullable AiHubChatStatus status) {
    }
}
