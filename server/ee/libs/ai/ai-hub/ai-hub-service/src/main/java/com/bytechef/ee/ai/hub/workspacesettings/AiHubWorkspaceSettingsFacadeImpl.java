/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.workspacesettings;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiHubWorkspaceSettingsFacade}. Carries the workspace-role guards and delegates to
 * {@link AiHubWorkspaceSettingsService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class AiHubWorkspaceSettingsFacadeImpl implements AiHubWorkspaceSettingsFacade {

    private final AiHubWorkspaceSettingsService aiHubWorkspaceSettingsService;

    @SuppressFBWarnings("EI")
    public AiHubWorkspaceSettingsFacadeImpl(AiHubWorkspaceSettingsService aiHubWorkspaceSettingsService) {
        this.aiHubWorkspaceSettingsService = aiHubWorkspaceSettingsService;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_VIEW')")
    public Optional<AiHubWorkspaceSettings> findByWorkspaceId(Long workspaceId) {
        return aiHubWorkspaceSettingsService.findByWorkspaceId(workspaceId);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MANAGE')")
    public AiHubWorkspaceSettings updateVoiceWebhookUrl(Long workspaceId, @Nullable String voiceWebhookUrl) {
        return aiHubWorkspaceSettingsService.updateVoiceWebhookUrl(workspaceId, voiceWebhookUrl);
    }
}
