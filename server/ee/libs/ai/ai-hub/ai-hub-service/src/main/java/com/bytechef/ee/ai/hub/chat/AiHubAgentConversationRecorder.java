/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatPatch;
import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatTranscriptSummary;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.platform.ai.conversation.AgentConversationRecorder;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnAiHub
@Transactional
public class AiHubAgentConversationRecorder implements AgentConversationRecorder {

    private static final Logger log = LoggerFactory.getLogger(AiHubAgentConversationRecorder.class);

    private static final int PREVIEW_MAX_LENGTH = 255;

    private final AiHubChatRepository chatRepository;
    private final AiHubChatService chatService;
    private final Clock clock;
    private final ProjectService projectService;

    @Autowired
    public AiHubAgentConversationRecorder(
        AiHubChatRepository chatRepository, AiHubChatService chatService, ProjectService projectService) {

        this(chatRepository, chatService, projectService, Clock.systemUTC());
    }

    AiHubAgentConversationRecorder(
        AiHubChatRepository chatRepository, AiHubChatService chatService, ProjectService projectService, Clock clock) {

        this.chatRepository = chatRepository;
        this.chatService = chatService;
        this.clock = clock;
        this.projectService = projectService;
    }

    @Override
    public void recordTurn(AgentConversation agentConversation) {
        String conversationId = agentConversation.conversationId();

        if (conversationId == null || conversationId.isBlank()) {
            return;
        }

        if (!isStampAuthentic(agentConversation)) {
            return;
        }

        AiHubChat chat = findOrCreateChat(agentConversation, conversationId);

        if (chat == null) {
            return;
        }

        refreshChatMetadata(chat, agentConversation);
    }

    private boolean isStampAuthentic(AgentConversation agentConversation) {
        String workflowId = agentConversation.workflowId();

        if (workflowId == null || workflowId.isBlank()) {
            log.warn(
                "Not recording an agent conversation for workspace {}: the executing job reported no workflow id, so "
                    + "the workspace claimed by the workflow definition cannot be verified.",
                agentConversation.workspaceId());

            return false;
        }

        Optional<Project> project = projectService.fetchWorkflowProject(workflowId);

        if (project.isEmpty()) {
            log.warn(
                "Not recording an agent conversation: workflow {} does not resolve to a project, so the workspace "
                    + "claimed by its definition cannot be verified.",
                workflowId);

            return false;
        }

        Long actualWorkspaceId = project.get()
            .getWorkspaceId();

        if (actualWorkspaceId == null || actualWorkspaceId != agentConversation.workspaceId()) {
            log.warn(
                "Rejecting an agent conversation stamp: workflow {} belongs to workspace {}, but its definition "
                    + "claims workspace {} (agent {}, creator {}). No AI Hub chat was created.",
                workflowId, actualWorkspaceId, agentConversation.workspaceId(), agentConversation.aiAgentId(),
                agentConversation.creatorUserId());

            return false;
        }

        return true;
    }

    private @Nullable AiHubChat findOrCreateChat(AgentConversation agentConversation, String threadId) {
        Optional<AiHubChat> existingChat = chatRepository.findByThreadId(threadId);

        if (existingChat.isPresent()) {
            return adoptChat(existingChat.get(), agentConversation, threadId);
        }

        Integer environment = resolveEnvironment(agentConversation);

        if (environment == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now(clock);

        chatRepository.insertAgentChatIfAbsent(
            agentConversation.creatorUserId(), threadId, agentConversation.title(), AiHubChatStatus.ACTIVE.ordinal(),
            environment, AiHubChatKind.AGENT_CHAT.ordinal(), agentConversation.workspaceId(),
            agentConversation.aiAgentId(), now);

        return chatRepository.findByThreadId(threadId)
            .map(chat -> adoptChat(chat, agentConversation, threadId))
            .orElse(null);
    }

    private @Nullable AiHubChat adoptChat(AiHubChat chat, AgentConversation agentConversation, String threadId) {
        if (chat.getKind() != AiHubChatKind.AGENT_CHAT || chat.getAiAgentId() == null
            || chat.getWorkflowExecutionId() != null) {

            log.warn(
                "Not recording an agent conversation: thread {} is already claimed by an AI Hub chat that did not "
                    + "originate from an agent channel run (kind={}, aiAgentId={}, workflowExecutionId={}).",
                threadId, chat.getKind(), chat.getAiAgentId(), chat.getWorkflowExecutionId());

            return null;
        }

        if (chat.getUserId() != agentConversation.creatorUserId()
            || !Objects.equals(chat.getWorkspaceId(), agentConversation.workspaceId())) {

            log.warn(
                "Not recording an agent conversation: thread {} is already owned by user {} in workspace {}, while "
                    + "agent {} reports creator {} in workspace {}.",
                threadId, chat.getUserId(), chat.getWorkspaceId(), agentConversation.aiAgentId(),
                agentConversation.creatorUserId(), agentConversation.workspaceId());

            return null;
        }

        if (chat.getStatus() == AiHubChatStatus.DELETED) {
            return null;
        }

        return chat;
    }

    private void refreshChatMetadata(AiHubChat chat, AgentConversation agentConversation) {
        Long chatId = chat.getId();
        long workspaceId = agentConversation.workspaceId();
        long creatorUserId = agentConversation.creatorUserId();

        AiHubChatTranscriptSummary transcriptSummary =
            chatService.summarizeTranscript(chatId, workspaceId, creatorUserId);

        chatService.patch(
            chatId, workspaceId, creatorUserId,
            new AiHubChatPatch(
                null, resolvePreview(transcriptSummary.lastMessageContent()), transcriptSummary.messageCount(), null));
    }

    private static @Nullable String resolvePreview(@Nullable String lastMessageContent) {
        if (lastMessageContent == null || lastMessageContent.isBlank()) {
            return null;
        }

        if (lastMessageContent.length() <= PREVIEW_MAX_LENGTH) {
            return lastMessageContent;
        }

        int endIndex = PREVIEW_MAX_LENGTH;

        if (Character.isHighSurrogate(lastMessageContent.charAt(endIndex - 1))) {
            endIndex--;
        }

        return lastMessageContent.substring(0, endIndex);
    }

    private static @Nullable Integer resolveEnvironment(AgentConversation agentConversation) {
        Long environmentId = agentConversation.environmentId();

        if (environmentId == null) {
            log.warn(
                "Not recording an agent conversation for agent {}: the executing job reported no environment.",
                agentConversation.aiAgentId());

            return null;
        }

        Environment[] environments = Environment.values();

        if (environmentId < 0 || environmentId >= environments.length) {
            log.warn(
                "Not recording an agent conversation for agent {}: unknown environment id {}.",
                agentConversation.aiAgentId(), environmentId);

            return null;
        }

        return environmentId.intValue();
    }
}
