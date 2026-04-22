/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.prompt.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.prompt.facade.AiPromptFacade;
import com.bytechef.ee.platform.ai.observability.dto.AiPromptVersionMetrics;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilitySpanRepository;
import com.bytechef.ee.platform.ai.prompt.AiPrompt;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersion;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersionService;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersionType;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for prompts and prompt versions. Combines:
 * <ul>
 * <li>workspace-agnostic CRUD and workspace-scoped queries/creation routed through {@link AiPromptFacade}, which hosts
 * the authorization guards so they protect every caller of the underlying services (not just this entry point), and
 * <li>the gateway-only {@code AiPromptVersion.metrics} resolver, which aggregates LLM observability spans for
 * per-version invocation/cost/latency stats. The metrics resolver was historically a separate controller in
 * automation-ai-gateway-graphql; merging it here keeps the prompt schema in one place.
 * </ul>
 *
 * <p>
 * The {@code @PreAuthorize} role guards that previously lived on this controller were moved onto
 * {@link AiPromptFacade}'s implementation so they apply to every caller of the facade. The {@code versions} and
 * {@code metrics} schema resolvers were never guarded and continue to read directly from the underlying
 * service/repository.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiPromptGraphQlController {

    private final AiObservabilitySpanRepository aiObservabilitySpanRepository;
    private final AiPromptFacade aiPromptFacade;
    private final AiPromptVersionService aiPromptVersionService;

    @SuppressFBWarnings("EI")
    AiPromptGraphQlController(
        AiObservabilitySpanRepository aiObservabilitySpanRepository, AiPromptFacade aiPromptFacade,
        AiPromptVersionService aiPromptVersionService) {

        this.aiObservabilitySpanRepository = aiObservabilitySpanRepository;
        this.aiPromptFacade = aiPromptFacade;
        this.aiPromptVersionService = aiPromptVersionService;
    }

    @QueryMapping
    public AiPrompt aiPrompt(@Argument long id) {
        return aiPromptFacade.getPrompt(id);
    }

    @QueryMapping
    public List<AiPrompt> aiPrompts(@Argument Long workspaceId) {
        return aiPromptFacade.getPromptsByWorkspace(workspaceId);
    }

    @QueryMapping
    public List<AiPromptVersion> aiPromptVersions(@Argument Long promptId) {
        return aiPromptFacade.getVersionsByPrompt(promptId);
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
    public AiPrompt createAiPrompt(@Argument CreateAiPromptInput input) {
        AiPrompt prompt = new AiPrompt(input.name());

        if (input.description() != null) {
            prompt.setDescription(input.description());
        }

        if (input.projectId() != null) {
            prompt.setProjectId(Long.parseLong(input.projectId()));
        }

        return aiPromptFacade.createInWorkspace(prompt, Long.parseLong(input.workspaceId()));
    }

    @MutationMapping
    public AiPromptVersion createAiPromptVersion(@Argument CreateAiPromptVersionInput input) {
        Long promptId = Long.parseLong(input.promptId());

        int nextVersionNumber = aiPromptFacade.getNextVersionNumber(promptId);

        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();
        String createdBy = authentication != null ? authentication.getName() : "system";

        AiPromptVersionType versionType = AiPromptVersionType.valueOf(input.type());

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

        return aiPromptFacade.createVersion(promptVersion);
    }

    @MutationMapping
    public boolean deleteAiPrompt(@Argument long id) {
        aiPromptFacade.deleteInWorkspace(id);

        return true;
    }

    @MutationMapping
    public boolean setActiveAiPromptVersion(@Argument long promptVersionId, @Argument String environment) {
        aiPromptFacade.setActiveVersion(promptVersionId, environment);

        return true;
    }

    @MutationMapping
    public AiPrompt updateAiPrompt(@Argument long id, @Argument UpdateAiPromptInput input) {
        AiPrompt prompt = aiPromptFacade.getPrompt(id);

        if (input.description() != null) {
            prompt.setDescription(input.description());
        }

        if (input.name() != null) {
            prompt.setName(input.name());
        }

        return aiPromptFacade.update(prompt);
    }

    @SuppressFBWarnings("EI")
    public record CreateAiPromptInput(String description, String name, String projectId, String workspaceId) {
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
