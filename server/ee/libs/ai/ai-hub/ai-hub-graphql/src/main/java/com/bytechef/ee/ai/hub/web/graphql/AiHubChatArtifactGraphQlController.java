/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifact;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactFacade.AiHubChatArtifactPage;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactService;
import com.bytechef.ee.ai.hub.security.WorkspaceAccessGuard;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.ZoneOffset;
import java.util.Map;
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
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * GraphQL surface for AI Hub chat artifacts (audit log). Read-only listing for admins plus a
 * {@code recordReferencedAiHubChatArtifact} mutation used by the composer to attach a referenced resource (file,
 * workflow, data table, knowledge base) to a chat.
 *
 * <p>
 * Authorization: every operation requires workspace membership (class-level {@code isAuthenticated()} plus the
 * programmatic {@link WorkspaceAccessGuard} check); the audit listing additionally requires {@code ADMIN} authority,
 * enforced on {@link AiHubChatArtifactFacade} rather than here. "Artifact does not exist" and "artifact exists in
 * another workspace" produce the same opaque {@link com.bytechef.ee.ai.hub.exception.NotFoundException} so an
 * authenticated workspace member cannot enumerate artifact ids across the rest of the system.
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
public class AiHubChatArtifactGraphQlController {

    private static final Logger log = LoggerFactory.getLogger(AiHubChatArtifactGraphQlController.class);

    private final AiHubChatArtifactFacade chatArtifactFacade;
    private final AiHubChatArtifactService chatArtifactService;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;
    private final JsonMapper jsonMapper;

    @SuppressFBWarnings("EI")
    public AiHubChatArtifactGraphQlController(
        AiHubChatArtifactFacade chatArtifactFacade, AiHubChatArtifactService chatArtifactService,
        UserService userService, WorkspaceFacade workspaceFacade, JsonMapper jsonMapper) {

        this.chatArtifactFacade = chatArtifactFacade;
        this.chatArtifactService = chatArtifactService;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
        this.jsonMapper = jsonMapper;
    }

    @QueryMapping
    public AiHubChatArtifactPage aiHubChatArtifacts(
        @Argument long workspaceId,
        @Argument @Nullable Integer environment,
        @Argument @Nullable Long userId,
        @Argument @Nullable AiHubChatArtifactKind kind,
        @Argument @Nullable Long from,
        @Argument @Nullable Long to,
        @Argument @Nullable Integer page,
        @Argument @Nullable Integer size) {

        return chatArtifactFacade.getAiHubChatArtifacts(
            workspaceId, environment, userId, kind, from, to, page, size);
    }

    @MutationMapping
    public AiHubChatArtifact recordReferencedAiHubChatArtifact(
        @Argument RecordReferencedAiHubChatArtifactInput input) {

        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, input.workspaceId());

        AiHubChatArtifactKind kind;

        try {
            kind = AiHubChatArtifactKind.valueOf(input.kind());
        } catch (IllegalArgumentException exception) {
            // The GraphQL layer should have validated the enum already; defensive fallback in case the
            // schema and the Java enum drift between deploys.
            throw new IllegalArgumentException("Unknown AiHubChatArtifactKind: " + input.kind(),
                exception);
        }

        // Optional metadata blob — currently used by WORKFLOW_REFERENCED to stash projectId /
        // projectWorkflowId so the sidebar quick-open can route the workflow tab (which keys on the
        // parent project). Other kinds may use it later for the same shape of side-channel context.
        // Deserialize defensively: a malformed payload from a misbehaving client must not abort the
        // recording — fall back to no metadata and log so the issue is visible.
        Map<String, Object> metadata = parseMetadataJson(input.metadataJson());

        // Service layer enforces the chat-belongs-to-(user, workspace) gate AND the idempotency
        // check by (chatId, kind, artifactId). Re-attaching the same file/workflow via the
        // composer is therefore safe to call repeatedly without proliferating sidebar rows.
        return chatArtifactService.recordReference(
            input.chatId(), input.workspaceId(), userId, kind, input.artifactId(), input.artifactName(),
            metadata);
    }

    @MutationMapping
    public boolean deleteAiHubChatArtifact(@Argument DeleteAiHubChatArtifactInput input) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, input.workspaceId());

        // Service enforces (artifact -> chat -> workspaceChat) ownership AND the
        // reference-kinds-only guard. The mutation returns true on success or on a benign no-op
        // (already deleted); throws on ownership / cross-workspace / non-reference-kind input.
        chatArtifactService.deleteReference(input.artifactId(), input.workspaceId(), userId);

        return true;
    }

    private Map<String, Object> parseMetadataJson(@Nullable String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }

        try {
            return jsonMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
        } catch (JacksonException exception) {
            log.warn("Failed to parse metadataJson on recordReferencedAiHubChatArtifact; ignoring", exception);

            return Map.of();
        }
    }

    @SchemaMapping(typeName = "AiHubChatArtifact", field = "kind")
    public String kind(AiHubChatArtifact artifact) {
        return artifact.getKind()
            .name();
    }

    @SchemaMapping(typeName = "AiHubChatArtifact", field = "status")
    public String status(AiHubChatArtifact artifact) {
        return artifact.getStatus()
            .name();
    }

    @SchemaMapping(typeName = "AiHubChatArtifact", field = "createdAt")
    @Nullable
    public Long createdAt(AiHubChatArtifact artifact) {
        return artifact.getCreatedAt() == null ? null
            : artifact.getCreatedAt()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiHubChatArtifact", field = "statusChangedAt")
    @Nullable
    public Long statusChangedAt(AiHubChatArtifact artifact) {
        return artifact.getStatusChangedAt() == null ? null
            : artifact.getStatusChangedAt()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    /**
     * Input for {@code recordReferencedAiHubChatArtifact}. Carries the chat + workspace ids alongside the resource
     * being attached. {@code kind} is the GraphQL enum value as a string — converted to {@link AiHubChatArtifactKind}
     * in the controller method (Spring's automatic enum coercion can't be relied on for nested input records the same
     * way it works for top-level @Argument enums).
     */
    public record RecordReferencedAiHubChatArtifactInput(
        long workspaceId, long chatId, String kind, String artifactId, String artifactName,
        @Nullable String metadataJson) {
    }

    /**
     * Input for {@code deleteAiHubChatArtifact}. Carries the workspace id (for the access-guard check) and the
     * artifact-row primary-key id. Workspace and chat ownership are re-verified server-side via the artifact -> chat ->
     * workspaceChat join inside the service.
     */
    public record DeleteAiHubChatArtifactInput(long workspaceId, long artifactId) {
    }
}
