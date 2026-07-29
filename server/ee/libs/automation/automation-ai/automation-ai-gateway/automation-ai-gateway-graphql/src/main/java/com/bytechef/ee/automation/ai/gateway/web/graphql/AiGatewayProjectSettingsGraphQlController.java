/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayProjectSettingsFacade;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProjectSettings;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * <p>
 * Authorization is enforced on {@link AiGatewayProjectSettingsFacade}, not here.
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiGatewayProjectSettingsGraphQlController {

    private final AiGatewayProjectSettingsFacade aiGatewayProjectSettingsFacade;

    @SuppressFBWarnings("EI")
    AiGatewayProjectSettingsGraphQlController(AiGatewayProjectSettingsFacade aiGatewayProjectSettingsFacade) {
        this.aiGatewayProjectSettingsFacade = aiGatewayProjectSettingsFacade;
    }

    @QueryMapping
    public AiGatewayProjectSettings aiGatewayProjectSettings(@Argument Long projectId) {
        return aiGatewayProjectSettingsFacade.findByProjectId(projectId)
            .orElse(null);
    }

    @MutationMapping
    public AiGatewayProjectSettings updateAiGatewayProjectSettings(@Argument AiGatewayProjectSettingsInput input) {
        return aiGatewayProjectSettingsFacade.upsert(new AiGatewayProjectSettings(
            input.projectId(),
            input.redactPii(),
            input.redactSecrets(),
            input.blockedTerms(),
            input.moderationEnabled(),
            input.injectionDetectionEnabled(),
            input.scanResponses()));
    }

    public record AiGatewayProjectSettingsInput(
        String blockedTerms, Boolean injectionDetectionEnabled, Boolean moderationEnabled, Long projectId,
        Boolean redactPii, Boolean redactSecrets, Boolean scanResponses) {
    }
}
