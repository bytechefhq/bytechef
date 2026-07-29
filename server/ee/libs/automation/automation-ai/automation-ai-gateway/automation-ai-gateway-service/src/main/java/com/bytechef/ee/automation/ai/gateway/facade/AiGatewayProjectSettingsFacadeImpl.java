/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.service.AiGatewayProjectSettingsService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProjectSettings;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiGatewayProjectSettingsFacade}. Delegates to the shared
 * {@code AiGatewayProjectSettingsService} and carries the authorization guards so they are enforced for every caller of
 * the facade. Project-scoped guardrail configuration is an administrative concern, so both read and write require admin
 * authority.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiGatewayProjectSettingsFacadeImpl implements AiGatewayProjectSettingsFacade {

    private final AiGatewayProjectSettingsService aiGatewayProjectSettingsService;

    @SuppressFBWarnings("EI")
    AiGatewayProjectSettingsFacadeImpl(AiGatewayProjectSettingsService aiGatewayProjectSettingsService) {
        this.aiGatewayProjectSettingsService = aiGatewayProjectSettingsService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Optional<AiGatewayProjectSettings> findByProjectId(Long projectId) {
        return aiGatewayProjectSettingsService.findByProjectId(projectId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayProjectSettings upsert(AiGatewayProjectSettings settings) {
        return aiGatewayProjectSettingsService.upsert(settings);
    }
}
