/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayRateLimitFacade;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimit;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitScope;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitType;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing AI Gateway rate limits.
 *
 * <p>
 * Authorization is enforced on {@link AiGatewayRateLimitFacade}, not here.
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiGatewayRateLimitGraphQlController {

    private final AiGatewayRateLimitFacade aiGatewayRateLimitFacade;

    @SuppressFBWarnings("EI")
    AiGatewayRateLimitGraphQlController(AiGatewayRateLimitFacade aiGatewayRateLimitFacade) {
        this.aiGatewayRateLimitFacade = aiGatewayRateLimitFacade;
    }

    @QueryMapping
    public List<AiGatewayRateLimit> aiGatewayRateLimits(@Argument long workspaceId) {
        return aiGatewayRateLimitFacade.getRateLimitsByWorkspaceId(workspaceId);
    }

    @MutationMapping
    public AiGatewayRateLimit createAiGatewayRateLimit(@Argument CreateAiGatewayRateLimitInput input) {
        AiGatewayRateLimit rateLimit = new AiGatewayRateLimit(
            input.name(), input.scope(), input.limitType(), input.limitValue(), input.windowSeconds());

        if (input.enabled() != null) {
            rateLimit.setEnabled(input.enabled());
        }

        if (input.projectId() != null) {
            rateLimit.setProjectId(Long.valueOf(input.projectId()));
        }

        if (input.propertyKey() != null) {
            rateLimit.setPropertyKey(input.propertyKey());
        }

        return aiGatewayRateLimitFacade.createInWorkspace(rateLimit, Long.parseLong(input.workspaceId()));
    }

    @MutationMapping
    public boolean deleteAiGatewayRateLimit(@Argument long id) {
        aiGatewayRateLimitFacade.delete(id);

        return true;
    }

    @MutationMapping
    public AiGatewayRateLimit updateAiGatewayRateLimit(
        @Argument long id, @Argument UpdateAiGatewayRateLimitInput input) {

        AiGatewayRateLimit rateLimit = aiGatewayRateLimitFacade.getRateLimit(id);

        if (input.enabled() != null) {
            rateLimit.setEnabled(input.enabled());
        }

        if (input.limitType() != null) {
            rateLimit.setLimitType(input.limitType());
        }

        if (input.limitValue() != null) {
            rateLimit.setLimitValue(input.limitValue());
        }

        if (input.name() != null) {
            rateLimit.setName(input.name());
        }

        if (input.projectId() != null) {
            rateLimit.setProjectId(Long.valueOf(input.projectId()));
        }

        if (input.propertyKey() != null) {
            rateLimit.setPropertyKey(input.propertyKey());
        }

        if (input.scope() != null) {
            rateLimit.setScope(input.scope());
        }

        if (input.windowSeconds() != null) {
            rateLimit.setWindowSeconds(input.windowSeconds());
        }

        return aiGatewayRateLimitFacade.update(rateLimit);
    }

    @SuppressFBWarnings("EI")
    public record CreateAiGatewayRateLimitInput(
        Boolean enabled, AiGatewayRateLimitType limitType, int limitValue, String name,
        String projectId, String propertyKey, AiGatewayRateLimitScope scope,
        int windowSeconds, String workspaceId) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateAiGatewayRateLimitInput(
        Boolean enabled, AiGatewayRateLimitType limitType, Integer limitValue, String name,
        String projectId, String propertyKey, AiGatewayRateLimitScope scope,
        Integer windowSeconds) {
    }
}
