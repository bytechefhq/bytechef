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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EE implementation of the CE {@link AgentConversationRecorder} port: makes a conversation that reached an AI Agent
 * through one of its channels readable in the AI Hub chats list (ticket 732,
 * {@code 2026-08-17-agent-run-hub-visibility}).
 *
 * <p>
 * It writes <b>no transcript</b>. The agent's builtin chat-memory element already persists both sides of every turn
 * into the same Spring AI session store the Hub reads, under session id = {@code conversationId}. All that is missing
 * is the {@code ai_hub_chat} metadata row pointing at that thread, so this class find-or-creates exactly that row and
 * then refreshes its {@code message_count} and preview through the Hub's own read/patch paths.
 * </p>
 *
 * <p>
 * <b>Keying.</b> {@code thread_id = conversationId} verbatim, and the lookup is by {@code thread_id} ALONE, mirroring
 * {@code AiHubChatServiceImpl.create}: that column carries a global UNIQUE constraint, so no composite key is possible.
 * {@code ai_agent_id} is attribution, not a uniqueness scope — two agents reachable on the same Slack channel share one
 * {@code conversationId}, therefore one session, one transcript and one row, attributed to whichever agent spoke first.
 * </p>
 *
 * <p>
 * <b>The identity stamp is not trusted.</b> {@code workspaceId}/{@code aiAgentId}/{@code creatorUserId} are read off
 * the workflow node's extension map by a component that has no principal, and the keys carrying them are globally
 * allowlisted reserved words — so a hand-authored definition can claim any workspace and any user. Every turn is
 * therefore checked against the workflow the platform says is actually executing (see
 * {@link #isStampAuthentic(AgentConversation)}) before a row is written.
 * </p>
 *
 * <p>
 * <b>Fail-open, always.</b> Every rejection path returns quietly (with a log line) rather than throwing: this runs on
 * the agent's turn-completion path, and a Hub bookkeeping problem must never fail the agent's turn. In particular a
 * cross-owner {@code thread_id} collision is "found, not mine" and is skipped — deliberately NOT the
 * {@code ConflictException} that {@code AiHubChatServiceImpl.create} raises for the same situation on its user-facing
 * path.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
@Transactional
public class AiHubAgentConversationRecorder implements AgentConversationRecorder {

    private static final Logger log = LoggerFactory.getLogger(AiHubAgentConversationRecorder.class);

    /**
     * Upper bound on the sidebar preview text. {@code last_preview} is a TEXT column with no length limit of its own,
     * but a channel turn can be arbitrarily long and the sidebar renders one line.
     */
    private static final int PREVIEW_MAX_LENGTH = 255;

    private final AiHubChatRepository chatRepository;
    private final AiHubChatService chatService;
    private final Clock clock;
    private final ProjectService projectService;

    /**
     * Marked {@code @Autowired} because this class has two constructors: Spring auto-selects a constructor only when
     * there is exactly one candidate, and would otherwise fall back to a no-arg constructor that does not exist,
     * failing context startup.
     */
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
            // Chat memory is off for this agent, so there is no session and nothing to point a chat row at. A
            // resolved decision, not an error (design spec §8).
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

    /**
     * Verifies the caller's claimed workspace against the workspace that actually owns the executing workflow.
     *
     * <p>
     * The claim arrives in the payload, having been read off the node definition, and the three keys that carry it
     * ({@code aiHubWorkspaceId}, {@code aiHubAgentId}, {@code aiHubCreatorUserId}) are globally allowlisted reserved
     * words. Anyone who can author a workflow definition — the workflow editor, {@code updateWorkflow}, the MCP write
     * tools — can therefore set them freely, and the AI Agent component cannot tell a generated definition from a
     * hand-written one because it runs with no principal. {@code workflowId}, by contrast, comes from the platform's
     * own execution context, so resolving it to a project and comparing that project's workspace is a check the
     * definition cannot influence.
     * </p>
     *
     * <p>
     * Fails closed: no workflow id, an unresolvable workflow, a workflow outside any workspace, and a mismatch all mean
     * no row. A wrong-workspace row would be invisible to its claimed owner anyway (every list query filters on
     * workspace) while permanently claiming the globally-unique {@code thread_id}, so writing one is strictly worse
     * than writing none.
     * </p>
     *
     * <p>
     * The lookup goes through {@code ProjectService#fetchWorkflowProject}, NOT {@code getWorkflowProject}. The latter
     * throws for an unknown workflow, and because it is itself {@code @Transactional}, its proxy would mark this
     * method's transaction rollback-only the instant it threw — catching the exception here would leave the skip
     * looking quiet while the transaction failed at commit with {@code UnexpectedRollbackException}, and would poison
     * any caller transaction this recorder ever participates in.
     * </p>
     */
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

    /**
     * Returns the chat row this turn belongs to, creating it on the conversation's first turn, or {@code null} when the
     * thread is already claimed by a row this conversation must not touch.
     */
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

        // Re-read rather than trusting the insert's row count: when a concurrent turn of the same conversation won the
        // race, DO NOTHING left its row in place and that is the row this turn must use.
        return chatRepository.findByThreadId(threadId)
            .map(chat -> adoptChat(chat, agentConversation, threadId))
            .orElse(null);
    }

    /**
     * Decides whether an existing row is this conversation's chat. Returns {@code null} — skip, never throw — for every
     * row that is not.
     */
    private @Nullable AiHubChat adoptChat(AiHubChat chat, AgentConversation agentConversation, String threadId) {
        if (chat.getKind() != AiHubChatKind.AGENT_CHAT || chat.getAiAgentId() == null
            || chat.getWorkflowExecutionId() != null) {

            // A composer-created agent chat must never be adopted: it carries a random UUID thread id and the user
            // deliberately started it fresh. Two independent marks separate it from a channel-born row, because
            // either one alone rests on an invariant nothing enforces — createWebhookBridgedChat happens to leave
            // ai_agent_id null today (a later task labelling composer rows by agent could change that), and this
            // recorder happens to leave workflow_execution_id null (there is no execution to bind to). Requiring both
            // means a change to either path degrades to "skip", never to "hijack".
            log.warn(
                "Not recording an agent conversation: thread {} is already claimed by an AI Hub chat that did not "
                    + "originate from an agent channel run (kind={}, aiAgentId={}, workflowExecutionId={}).",
                threadId, chat.getKind(), chat.getAiAgentId(), chat.getWorkflowExecutionId());

            return null;
        }

        if (chat.getUserId() != agentConversation.creatorUserId()
            || !Objects.equals(chat.getWorkspaceId(), agentConversation.workspaceId())) {

            // "Found, not mine." AiHubChatServiceImpl.create raises a ConflictException here; this path must not,
            // because it runs on the agent's turn.
            log.warn(
                "Not recording an agent conversation: thread {} is already owned by user {} in workspace {}, while "
                    + "agent {} reports creator {} in workspace {}.",
                threadId, chat.getUserId(), chat.getWorkspaceId(), agentConversation.aiAgentId(),
                agentConversation.creatorUserId(), agentConversation.workspaceId());

            return null;
        }

        if (chat.getStatus() == AiHubChatStatus.DELETED) {
            // Deletion is terminal (AiHubChat.setStatus refuses to transition out of it); appending to a deleted row
            // would resurrect a conversation the user removed.
            return null;
        }

        // Two agents on one channel share one conversationId, one session and one row. The row keeps its original
        // ai_agent_id — the first agent to speak owns the attribution — so nothing is rewritten here.
        return chat;
    }

    /**
     * Brings {@code message_count} and the sidebar preview up to date, and bumps {@code updated_at} so the chat sorts
     * to the top of the list — through {@link AiHubChatService#summarizeTranscript} and {@link AiHubChatService#patch},
     * the Hub's own read and write. No transcript is written: the summary is reading back what the agent's chat-memory
     * element already stored under this thread id.
     *
     * <p>
     * Deliberately not {@code loadMessages}: that read stops after the most recent 500 visible rows because the UI
     * never renders more, so on a long-lived channel thread its last element is message #500 and the preview would
     * freeze there — showing a months-old line on a row whose {@code updated_at} keeps re-sorting it to the top of the
     * sidebar as freshly active. {@code summarizeTranscript} walks to the genuine end.
     * </p>
     */
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

    /**
     * Truncates the preview on a <b>code point</b> boundary, not a {@code char} one: channel text is emoji-heavy, and
     * {@code substring(0, 255)} can land between the two halves of a surrogate pair, leaving a lone surrogate that
     * renders as a replacement glyph in the sidebar. The result is therefore at most, and sometimes just under,
     * {@link #PREVIEW_MAX_LENGTH} chars.
     */
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

    /**
     * Resolves the executing job's environment ordinal. Returns {@code null} — meaning "write no row" — when the job
     * reported none or reported one this build does not know: a chat stamped with the wrong environment is invisible to
     * every list query while still consuming the conversation's globally-unique thread id.
     */
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
