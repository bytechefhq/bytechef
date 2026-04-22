/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayWorkspaceSettingsFacade;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayWorkspaceSettings;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * <p>
 * Authorization is enforced on {@link AiGatewayWorkspaceSettingsFacade}, not here.
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiGatewayWorkspaceSettingsGraphQlController {

    private final AiGatewayWorkspaceSettingsFacade aiGatewayWorkspaceSettingsFacade;

    @SuppressFBWarnings("EI")
    AiGatewayWorkspaceSettingsGraphQlController(AiGatewayWorkspaceSettingsFacade aiGatewayWorkspaceSettingsFacade) {
        this.aiGatewayWorkspaceSettingsFacade = aiGatewayWorkspaceSettingsFacade;
    }

    @QueryMapping
    public AiGatewayWorkspaceSettings aiGatewayWorkspaceSettings(@Argument Long workspaceId) {
        return aiGatewayWorkspaceSettingsFacade.findByWorkspaceId(workspaceId)
            .orElse(null);
    }

    @MutationMapping
    public AiGatewayWorkspaceSettings updateAiGatewayWorkspaceSettings(
        @Argument AiGatewayWorkspaceSettingsInput input) {

        return aiGatewayWorkspaceSettingsFacade.upsert(new AiGatewayWorkspaceSettings(
            input.workspaceId(),
            input.retryCount(),
            input.timeoutMs(),
            input.cacheEnabled(),
            input.cacheTtlSeconds(),
            input.logRetentionDays(),
            input.defaultRoutingPolicyId(),
            input.softBudgetWarningPct(),
            input.redactPii()));
    }

    public record AiGatewayWorkspaceSettingsInput(
        Boolean cacheEnabled, Integer cacheTtlSeconds, Long defaultRoutingPolicyId, Integer logRetentionDays,
        Boolean redactPii, Integer retryCount, Integer softBudgetWarningPct, Integer timeoutMs, Long workspaceId) {
    }
}
