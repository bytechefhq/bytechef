/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiPrompt;
import com.bytechef.ee.automation.ai.gateway.domain.AiPromptVersion;
import com.bytechef.ee.automation.ai.gateway.domain.AiPromptVersionType;
import com.bytechef.ee.automation.ai.gateway.dto.AiPromptVersionMetrics;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilitySpanRepository;
import com.bytechef.ee.automation.ai.gateway.service.AiPromptService;
import com.bytechef.ee.automation.ai.gateway.service.AiPromptVersionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiPromptGraphQlController {

    private final AiObservabilitySpanRepository aiObservabilitySpanRepository;
    private final AiPromptService aiPromptService;
    private final AiPromptVersionService aiPromptVersionService;

    @SuppressFBWarnings("EI")
    AiPromptGraphQlController(
        AiObservabilitySpanRepository aiObservabilitySpanRepository,
        AiPromptService aiPromptService,
        AiPromptVersionService aiPromptVersionService) {

        this.aiObservabilitySpanRepository = aiObservabilitySpanRepository;
        this.aiPromptService = aiPromptService;
        this.aiPromptVersionService = aiPromptVersionService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiPrompt aiPrompt(@Argument long id) {
        return aiPromptService.getPrompt(id);
    }

    @QueryMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")
    public List<AiPrompt> aiPrompts(@Argument Long workspaceId) {
        return aiPromptService.getPromptsByWorkspace(workspaceId);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiPromptVersion> aiPromptVersions(@Argument Long promptId) {
        return aiPromptVersionService.getVersionsByPrompt(promptId);
    }

    @SchemaMapping(typeName = "AiPrompt", field = "versions")
    public List<AiPromptVersion> versions(AiPrompt prompt) {
        return aiPromptVersionService.getVersionsByPrompt(prompt.getId());
    }

    @SchemaMapping(typeName = "AiPromptVersion", field = "metrics")
    public AiPromptVersionMetrics metrics(AiPromptVersion version) {
        return aiObservabilitySpanRepository.aggregateMetricsByPromptVersion(version.getId())
            .orElseGet(AiPromptVersionMetrics::empty);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiPrompt createAiPrompt(@Argument CreateAiPromptInput input) {
        AiPrompt prompt = new AiPrompt(
            Long.parseLong(input.workspaceId()), input.name());

        if (input.description() != null) {
            prompt.setDescription(input.description());
        }

        if (input.projectId() != null) {
            prompt.setProjectId(Long.parseLong(input.projectId()));
        }

        return aiPromptService.create(prompt);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiPromptVersion createAiPromptVersion(
        @Argument CreateAiPromptVersionInput input) {

        Long promptId = Long.parseLong(input.promptId());

        int nextVersionNumber = aiPromptVersionService.getNextVersionNumber(promptId);

        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();
        String createdBy = authentication != null ? authentication.getName() : "system";

        AiPromptVersionType versionType =
            AiPromptVersionType.valueOf(input.type());

        AiPromptVersion promptVersion = new AiPromptVersion(
            promptId, nextVersionNumber, versionType, input.content(), createdBy);

        if (input.commitMessage() != null) {
            promptVersion.setCommitMessage(input.commitMessage());
        }

        if (input.environment() != null) {
            promptVersion.setEnvironment(input.environment());
        }

        if (input.variables() != null) {
            promptVersion.setVariables(input.variables());
        }

        if (input.active() != null && input.active()) {
            promptVersion.setActive(true);
        }

        return aiPromptVersionService.create(promptVersion);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiPrompt(@Argument long id) {
        aiPromptService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean setActiveAiPromptVersion(@Argument long promptVersionId, @Argument String environment) {
        aiPromptVersionService.setActiveVersion(promptVersionId, environment);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiPrompt updateAiPrompt(
        @Argument long id, @Argument UpdateAiPromptInput input) {

        AiPrompt prompt = aiPromptService.getPrompt(id);

        if (input.description() != null) {
            prompt.setDescription(input.description());
        }

        if (input.name() != null) {
            prompt.setName(input.name());
        }

        return aiPromptService.update(prompt);
    }

    @SuppressFBWarnings("EI")
    public record CreateAiPromptInput(
        String description, String name, String projectId, String workspaceId) {
    }

    @SuppressFBWarnings("EI")
    public record CreateAiPromptVersionInput(
        Boolean active, String commitMessage, String content, String environment, String promptId,
        String type, String variables) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateAiPromptInput(String description, String name) {
    }
}
