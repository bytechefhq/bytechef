/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatArtifactRepository;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Default implementation of {@link AiHubChatArtifactService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnAiHub
@Transactional
public class AiHubChatArtifactServiceImpl implements AiHubChatArtifactService {

    /**
     * Hard upper bound on the number of artifacts returned by {@link #listByChat}. Without this cap a long-running chat
     * could accumulate hundreds of artifacts and force the controller to materialise them all in memory; the audit/log
     * UI never needs more than a single bounded page at a time.
     */
    private static final int LIST_LIMIT = 100;

    private static final List<Integer> WORKFLOW_KIND_ORDINALS = List.of(
        AiHubChatArtifactKind.WORKFLOW_CREATED.ordinal(),
        AiHubChatArtifactKind.WORKFLOW_UPDATED.ordinal(),
        AiHubChatArtifactKind.WORKFLOW_REFERENCED.ordinal());

    private static final Logger log = LoggerFactory.getLogger(AiHubChatArtifactServiceImpl.class);

    private final AiHubChatArtifactRepository chatArtifactRepository;
    private final AiHubChatService chatService;
    private final Clock clock;
    private final JsonMapper jsonMapper;
    private final ObjectProvider<ProjectWorkflowService> projectWorkflowServiceProvider;

    @SuppressFBWarnings({
        "EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"
    })
    public AiHubChatArtifactServiceImpl(
        AiHubChatArtifactRepository chatArtifactRepository,
        AiHubChatService chatService,
        JsonMapper jsonMapper,
        ObjectProvider<ProjectWorkflowService> projectWorkflowServiceProvider) {

        this.chatArtifactRepository = chatArtifactRepository;
        this.chatService = chatService;
        this.clock = Clock.systemUTC();
        this.jsonMapper = jsonMapper;
        this.projectWorkflowServiceProvider = projectWorkflowServiceProvider;
    }

    @Override
    public void record(
        String threadId, long userId, AiHubChatArtifactKind kind,
        String artifactId, String artifactName, @Nullable Map<String, Object> metadata) {

        Optional<AiHubChat> chatOptional =
            chatService.findByThreadIdViewable(threadId, userId);

        if (chatOptional.isEmpty()) {
            log.warn(
                "No chat found for threadId={} userId={} kind={} artifactId={} artifactName={} — " +
                    "DROPPING artifact record. The underlying mutation has already committed; the user will not " +
                    "see an undo entry for it.",
                threadId, userId, kind, artifactId, artifactName);

            return;
        }

        AiHubChat chat = chatOptional.get();

        AiHubChatArtifact artifact = new AiHubChatArtifact();

        artifact.setChatId(chat.getId());
        artifact.setKind(kind);
        artifact.setArtifactId(artifactId);
        artifact.setArtifactName(artifactName);
        artifact.setEnvironment(chat.getEnvironment());
        artifact.setCreatedAt(LocalDateTime.now(clock));

        if (metadata != null && !metadata.isEmpty()) {
            artifact.setMetadataJson(serializeMetadata(metadata, kind, artifactId));
        }

        artifact.setStatus(AiHubChatArtifactStatus.APPLIED);

        chatArtifactRepository.save(artifact);
    }

    @Override
    public void recordWorkflowArtifact(
        String threadId, @Nullable Long userId, AiHubChatArtifactKind kind,
        String workflowId, long projectId, @Nullable Long projectWorkflowId, String workflowName) {

        if (userId == null) {
            log.warn(
                "recordWorkflowArtifact called without a bound userId — skipping (threadId={}, workflowId={}). "
                    + "The persist has already committed; the user will not see an artifact for it.",
                threadId, workflowId);

            return;
        }

        Optional<AiHubChat> chatOptional = chatService.findByThreadIdViewable(threadId, userId);

        if (chatOptional.isEmpty()) {
            log.warn(
                "No chat found for threadId={} userId={} — DROPPING workflow artifact (workflowId={}, name={}).",
                threadId, userId, workflowId, workflowName);

            return;
        }

        AiHubChat chat = chatOptional.get();

        Map<String, Object> metadata = new HashMap<>();

        metadata.put("projectId", String.valueOf(projectId));

        if (projectWorkflowId != null) {
            metadata.put("projectWorkflowId", projectWorkflowId);
        }

        Optional<AiHubChatArtifact> existingOptional =
            chatArtifactRepository.findFirstByChatIdAndArtifactIdAndKindIn(
                chat.getId(), workflowId, WORKFLOW_KIND_ORDINALS);

        AiHubChatArtifact artifact;

        if (existingOptional.isPresent()) {
            artifact = existingOptional.get();

            artifact.setArtifactName(workflowName);
            artifact.setMetadataJson(serializeMetadata(metadata, artifact.getKind(), workflowId));
        } else {
            artifact = new AiHubChatArtifact();

            artifact.setChatId(chat.getId());
            artifact.setKind(kind);
            artifact.setArtifactId(workflowId);
            artifact.setArtifactName(workflowName);
            artifact.setEnvironment(chat.getEnvironment());
            artifact.setCreatedAt(LocalDateTime.now(clock));
            artifact.setMetadataJson(serializeMetadata(metadata, kind, workflowId));
            artifact.setStatus(AiHubChatArtifactStatus.APPLIED);
        }

        chatArtifactRepository.save(artifact);
    }

    @Override
    public AiHubChatArtifact recordReference(
        long chatId, long requesterWorkspaceId, long requesterUserId,
        AiHubChatArtifactKind kind,
        String artifactId, String artifactName,
        @Nullable Map<String, Object> metadata) {

        AiHubChat chat = chatService.getViewable(chatId, requesterWorkspaceId, requesterUserId);

        Optional<AiHubChatArtifact> existing =
            WORKFLOW_KIND_ORDINALS.contains(kind.ordinal())
                ? chatArtifactRepository.findFirstByChatIdAndArtifactIdAndKindIn(
                    chatId, artifactId, WORKFLOW_KIND_ORDINALS)
                : chatArtifactRepository.findFirstByChatIdAndKindAndArtifactId(
                    chatId, kind.ordinal(), artifactId);

        if (existing.isPresent()) {
            return existing.get();
        }

        AiHubChatArtifact artifact = new AiHubChatArtifact();

        artifact.setChatId(chatId);
        artifact.setKind(kind);
        artifact.setArtifactId(artifactId);
        artifact.setArtifactName(artifactName);
        artifact.setEnvironment(chat.getEnvironment());
        artifact.setCreatedAt(LocalDateTime.now(clock));

        if (metadata != null && !metadata.isEmpty()) {
            artifact.setMetadataJson(serializeMetadata(metadata, kind, artifactId));
        }

        artifact.setStatus(AiHubChatArtifactStatus.APPLIED);

        return chatArtifactRepository.save(artifact);
    }

    @Override
    public @Nullable AiHubChatArtifact recordReferenceByThread(
        String threadId, @Nullable Long userId, AiHubChatArtifactKind kind, String artifactId,
        String artifactName) {

        if (userId == null) {
            return null;
        }

        Optional<AiHubChat> chatOptional = chatService.findByThreadIdViewable(threadId, userId);

        if (chatOptional.isEmpty()) {
            return null;
        }

        AiHubChat chat = chatOptional.get();

        Optional<AiHubChatArtifact> existing = chatArtifactRepository.findFirstByChatIdAndKindAndArtifactId(
            chat.getId(), kind.ordinal(), artifactId);

        if (existing.isPresent()) {
            return existing.get();
        }

        AiHubChatArtifact artifact = new AiHubChatArtifact();

        artifact.setChatId(chat.getId());
        artifact.setKind(kind);
        artifact.setArtifactId(artifactId);
        artifact.setArtifactName(artifactName);
        artifact.setEnvironment(chat.getEnvironment());
        artifact.setCreatedAt(LocalDateTime.now(clock));
        artifact.setStatus(AiHubChatArtifactStatus.APPLIED);

        return chatArtifactRepository.save(artifact);
    }

    @Override
    public void deleteReference(long artifactId, long requesterWorkspaceId, long requesterUserId) {
        Optional<AiHubChatArtifact> artifactOptional = chatArtifactRepository.findById(artifactId);

        if (artifactOptional.isEmpty()) {
            return;
        }

        AiHubChatArtifact artifact = artifactOptional.get();

        chatService.getViewable(artifact.getChatId(), requesterWorkspaceId, requesterUserId);

        AiHubChatArtifactKind kind = artifact.getKind();

        if (kind != AiHubChatArtifactKind.WORKFLOW_REFERENCED && kind != AiHubChatArtifactKind.AI_AGENT_REFERENCED) {
            throw new IllegalArgumentException(
                "Only WORKFLOW_REFERENCED and AI_AGENT_REFERENCED artifacts can be deleted via this surface; got kind="
                    +
                    kind);
        }

        chatArtifactRepository.deleteById(artifactId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubChatArtifact> listByChat(
        long chatId, long requesterWorkspaceId, long requesterUserId) {

        chatService.getViewable(chatId, requesterWorkspaceId, requesterUserId);

        List<AiHubChatArtifact> artifacts = chatArtifactRepository.findByChatIdOrderByCreatedAtDesc(
            chatId, Limit.of(LIST_LIMIT));

        return withoutDeletedWorkflows(artifacts);
    }

    private List<AiHubChatArtifact> withoutDeletedWorkflows(List<AiHubChatArtifact> artifacts) {
        List<String> workflowIds = artifacts.stream()
            .filter(AiHubChatArtifactServiceImpl::isWorkflowArtifact)
            .map(AiHubChatArtifact::getArtifactId)
            .distinct()
            .toList();

        ProjectWorkflowService projectWorkflowService = projectWorkflowServiceProvider.getIfAvailable();

        if (workflowIds.isEmpty() || projectWorkflowService == null) {
            return artifacts;
        }

        Set<String> existingWorkflowIds;

        try {
            existingWorkflowIds = projectWorkflowService.getWorkflowProjectWorkflows(workflowIds)
                .stream()
                .map(ProjectWorkflow::getWorkflowId)
                .collect(Collectors.toSet());
        } catch (UnsupportedOperationException exception) {
            log.debug("Workflow lookup is not available in this deployment; listing every workflow artifact");

            return artifacts;
        }

        return artifacts.stream()
            .filter(artifact -> !isWorkflowArtifact(artifact) || existingWorkflowIds.contains(artifact.getArtifactId()))
            .toList();
    }

    private static boolean isWorkflowArtifact(AiHubChatArtifact artifact) {
        AiHubChatArtifactKind kind = artifact.getKind();

        return kind != null && WORKFLOW_KIND_ORDINALS.contains(kind.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByChat(long chatId) {
        return chatArtifactRepository.countByChatId(chatId);
    }

    /**
     * Returns the JSON payload, or {@code null} when serialization fails. The artifact is still recorded with a null
     * {@code metadataJson} when this returns null — losing the metadata does not invalidate the audit row. The kind and
     * artifactId are logged so ops can correlate the failure to the originating mutation.
     */
    private String
        serializeMetadata(Map<String, Object> metadata, AiHubChatArtifactKind kind, String artifactId) {
        try {
            return jsonMapper.writeValueAsString(metadata);
        } catch (JacksonException exception) {
            log.warn(
                "Failed to serialize artifact metadata for kind={}, artifactId={} — recording artifact with "
                    + "null metadataJson.",
                kind, artifactId, exception);

            return null;
        }
    }
}
