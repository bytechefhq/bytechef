/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing {@link AiGuardrailsWorkspaceSettings} for the admin settings UI.
 *
 * <p>
 * The read query is scoped to workspace members (or a tenant admin): a non-null {@code workspaceId} is checked via the
 * {@code AI_GATEWAY_VIEW} workspace permission scope (the same scope every other AI-settings read facade reuses -- see
 * {@code AiGatewayWorkspaceSettingsFacadeImpl}, {@code AiPromptFacadeImpl}); the null-{@code workspaceId}
 * tenant-default read is admin-only, since it has no workspace to scope against.
 *
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
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or (#workspaceId != null && hasPermission(#workspaceId, 'Workspace', "
        + "'AI_GATEWAY_VIEW'))")
    public @Nullable AiGuardrailsWorkspaceSettings aiGuardrailsWorkspaceSettings(
        @Argument @Nullable Long workspaceId) {

        return aiGuardrailsWorkspaceSettingsService.fetchSettings(workspaceId)
            .orElse(null);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AiGuardrailsWorkspaceSettings updateAiGuardrailsWorkspaceSettings(
        @Argument AiGuardrailsWorkspaceSettingsInput input) {

        return aiGuardrailsWorkspaceSettingsService.saveSettings(new AiGuardrailsWorkspaceSettings(
            input.workspaceId(), input.redactPii(), input.redactSecrets(), input.blockedTerms(),
            input.moderationEnabled(), input.injectionDetectionEnabled(), input.scanResponses(),
            input.blockingMode()));
    }

    public record AiGuardrailsWorkspaceSettingsInput(
        @Nullable Long workspaceId, @Nullable Boolean redactPii, @Nullable Boolean redactSecrets,
        @Nullable String blockedTerms, @Nullable Boolean moderationEnabled,
        @Nullable Boolean injectionDetectionEnabled, @Nullable Boolean scanResponses,
        @Nullable BlockingMode blockingMode) {
    }
}
