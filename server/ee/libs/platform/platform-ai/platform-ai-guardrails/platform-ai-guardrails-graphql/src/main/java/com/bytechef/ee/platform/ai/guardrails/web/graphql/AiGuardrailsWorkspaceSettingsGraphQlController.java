/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
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
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class AiGuardrailsWorkspaceSettingsGraphQlController {

    private final AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService;

    @SuppressFBWarnings("EI")
    AiGuardrailsWorkspaceSettingsGraphQlController(
        AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService) {

        this.aiGuardrailsWorkspaceSettingsService = aiGuardrailsWorkspaceSettingsService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or (#scope != "
        + "T(com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope).EMBEDDED "
        + "&& #workspaceId != null && hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW'))")
    public @Nullable AiGuardrailsWorkspaceSettings aiGuardrailsWorkspaceSettings(
        @Argument @Nullable Long workspaceId, @Argument @Nullable AiGuardrailsSettingsScope scope) {

        if (scope == AiGuardrailsSettingsScope.EMBEDDED) {
            return aiGuardrailsWorkspaceSettingsService.fetchEmbeddedSettings()
                .orElse(null);
        }

        return aiGuardrailsWorkspaceSettingsService.fetchSettings(workspaceId)
            .orElse(null);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or ((#input.scope == null || #input.scope == "
        + "T(com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope).WORKSPACE) "
        + "&& #input.workspaceId != null "
        + "&& hasPermission(#input.workspaceId, 'Workspace', 'AI_GATEWAY_EDIT'))")
    public AiGuardrailsWorkspaceSettings updateAiGuardrailsWorkspaceSettings(
        @Argument AiGuardrailsWorkspaceSettingsInput input) {

        AiGuardrailsSettingsScope scope = scopeOf(input);

        validateScopeWorkspaceIdPairing(scope, input.workspaceId());

        return aiGuardrailsWorkspaceSettingsService.saveSettings(new AiGuardrailsWorkspaceSettings(
            scope, input.workspaceId(), input.redactPii(), input.redactSecrets(), input.blockedTerms(),
            input.moderationEnabled(), input.injectionDetectionEnabled(), input.scanResponses(),
            input.blockingMode(), input.minConfidence(), input.redactMcpResults(),
            input.restoreIntoWorkflowOutput()));
    }

    private AiGuardrailsSettingsScope scopeOf(AiGuardrailsWorkspaceSettingsInput input) {
        if (input.scope() != null) {
            return input.scope();
        }

        return input.workspaceId() == null ? AiGuardrailsSettingsScope.PLATFORM : AiGuardrailsSettingsScope.WORKSPACE;
    }

    private void validateScopeWorkspaceIdPairing(AiGuardrailsSettingsScope scope, @Nullable Long workspaceId) {
        if ((scope == AiGuardrailsSettingsScope.WORKSPACE) != (workspaceId != null)) {
            throw new GraphQlBadRequestException(
                "workspaceId must be non-null exactly when scope is WORKSPACE, got scope=%s, workspaceId=%s"
                    .formatted(scope, workspaceId));
        }
    }

    public record AiGuardrailsWorkspaceSettingsInput(
        @Nullable AiGuardrailsSettingsScope scope, @Nullable Long workspaceId, @Nullable Boolean redactPii,
        @Nullable Boolean redactSecrets, @Nullable String blockedTerms, @Nullable Boolean moderationEnabled,
        @Nullable Boolean injectionDetectionEnabled, @Nullable Boolean scanResponses,
        @Nullable BlockingMode blockingMode, @Nullable Double minConfidence,
        @Nullable Boolean redactMcpResults, @Nullable Boolean restoreIntoWorkflowOutput) {
    }
}
