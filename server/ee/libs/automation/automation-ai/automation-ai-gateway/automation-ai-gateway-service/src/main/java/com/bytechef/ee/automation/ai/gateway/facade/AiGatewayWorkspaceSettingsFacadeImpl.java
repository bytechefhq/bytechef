/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.service.AiGatewayWorkspaceSettingsService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayWorkspaceSettings;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiGatewayWorkspaceSettingsFacade}. Delegates to the shared
 * {@code AiGatewayWorkspaceSettingsService} and carries the authorization guards so they are enforced for every caller
 * of the facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiGatewayWorkspaceSettingsFacadeImpl implements AiGatewayWorkspaceSettingsFacade {

    private final AiGatewayWorkspaceSettingsService aiGatewayWorkspaceSettingsService;

    @SuppressFBWarnings("EI")
    AiGatewayWorkspaceSettingsFacadeImpl(AiGatewayWorkspaceSettingsService aiGatewayWorkspaceSettingsService) {
        this.aiGatewayWorkspaceSettingsService = aiGatewayWorkspaceSettingsService;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public Optional<AiGatewayWorkspaceSettings> findByWorkspaceId(Long workspaceId) {
        return aiGatewayWorkspaceSettingsService.findByWorkspaceId(workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayWorkspaceSettings upsert(AiGatewayWorkspaceSettings settings) {
        return aiGatewayWorkspaceSettingsService.upsert(settings);
    }
}
