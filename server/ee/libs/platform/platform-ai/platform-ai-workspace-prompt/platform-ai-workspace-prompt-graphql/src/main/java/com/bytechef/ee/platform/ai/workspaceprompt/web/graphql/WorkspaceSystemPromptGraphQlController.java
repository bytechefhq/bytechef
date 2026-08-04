/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.workspaceprompt.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.ai.workspaceprompt.domain.WorkspaceSystemPrompt;
import com.bytechef.ee.platform.ai.workspaceprompt.service.WorkspaceSystemPromptService;
import com.bytechef.graphql.error.GraphQlBadRequestException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for the workspace system prompt settings page. Read is scoped to workspace members (or a tenant
 * admin) via the same {@code AI_GATEWAY_VIEW} workspace permission every other AI-settings read reuses; the mutation is
 * admin-only on the controller itself (guardrails/A2A precedent — no facade layer owns the check here).
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class WorkspaceSystemPromptGraphQlController {

    private final WorkspaceSystemPromptService workspaceSystemPromptService;

    @SuppressFBWarnings("EI")
    WorkspaceSystemPromptGraphQlController(WorkspaceSystemPromptService workspaceSystemPromptService) {
        this.workspaceSystemPromptService = workspaceSystemPromptService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public @Nullable WorkspaceSystemPrompt workspaceSystemPrompt(@Argument long workspaceId) {
        return workspaceSystemPromptService.fetchWorkspaceSystemPrompt(workspaceId)
            .map(prompt -> new WorkspaceSystemPrompt(workspaceId, prompt))
            .orElse(null);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @Nullable WorkspaceSystemPrompt updateWorkspaceSystemPrompt(@Argument WorkspaceSystemPromptInput input) {
        String inputPrompt = input.prompt();

        if (inputPrompt != null && inputPrompt.strip()
            .length() > WorkspaceSystemPrompt.MAX_LENGTH) {

            throw new GraphQlBadRequestException(
                "prompt must be at most %d characters".formatted(WorkspaceSystemPrompt.MAX_LENGTH));
        }

        return workspaceSystemPromptService.saveWorkspaceSystemPrompt(input.workspaceId(), inputPrompt)
            .map(prompt -> new WorkspaceSystemPrompt(input.workspaceId(), prompt))
            .orElse(null);
    }

    public record WorkspaceSystemPromptInput(long workspaceId, @Nullable String prompt) {
    }
}
