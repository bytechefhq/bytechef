/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifact;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactService;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.ZoneOffset;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@PreAuthorize("isAuthenticated()")
@ConditionalOnEEVersion
@ConditionalOnAiHub
public class AiHubChatArtifactGraphQlController {

    private static final Logger log = LoggerFactory.getLogger(AiHubChatArtifactGraphQlController.class);

    private final AiHubChatArtifactService chatArtifactService;
    private final JsonMapper jsonMapper;
    private final PermissionService permissionService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public AiHubChatArtifactGraphQlController(
        AiHubChatArtifactService chatArtifactService, JsonMapper jsonMapper, PermissionService permissionService,
        UserService userService) {

        this.chatArtifactService = chatArtifactService;
        this.jsonMapper = jsonMapper;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @MutationMapping
    public AiHubChatArtifact recordReferencedAiHubChatArtifact(
        @Argument RecordReferencedAiHubChatArtifactInput input) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, input.workspaceId());

        AiHubChatArtifactKind kind;

        try {
            kind = AiHubChatArtifactKind.valueOf(input.kind());
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException(
                "Unknown AiHubChatArtifactKind: " + input.kind(), illegalArgumentException);
        }

        return chatArtifactService.recordReference(
            input.chatId(), input.workspaceId(), currentUserId(), kind, input.artifactId(), input.artifactName(),
            parseMetadataJson(input.metadataJson()));
    }

    @MutationMapping
    public boolean deleteAiHubChatArtifact(@Argument DeleteAiHubChatArtifactInput input) {
        AiHubWorkspaceAccess.verifyViewer(permissionService, input.workspaceId());

        chatArtifactService.deleteReference(input.artifactId(), input.workspaceId(), currentUserId());

        return true;
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
    public @Nullable Long createdAt(AiHubChatArtifact artifact) {
        return artifact.getCreatedAt() == null ? null
            : artifact.getCreatedAt()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiHubChatArtifact", field = "statusChangedAt")
    public @Nullable Long statusChangedAt(AiHubChatArtifact artifact) {
        return artifact.getStatusChangedAt() == null ? null
            : artifact.getStatusChangedAt()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    private long currentUserId() {
        return userService.getCurrentUser()
            .getId();
    }

    private Map<String, Object> parseMetadataJson(@Nullable String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }

        try {
            return jsonMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
        } catch (JacksonException jacksonException) {
            log.warn("Failed to parse metadataJson on recordReferencedAiHubChatArtifact; ignoring", jacksonException);

            return Map.of();
        }
    }

    public record RecordReferencedAiHubChatArtifactInput(
        long workspaceId, long chatId, String kind, String artifactId, String artifactName,
        @Nullable String metadataJson) {
    }

    public record DeleteAiHubChatArtifactInput(long workspaceId, long artifactId) {
    }
}
