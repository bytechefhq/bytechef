/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimit;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitScope;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitType;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayRateLimitService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing AI Gateway rate limits.
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiGatewayRateLimitGraphQlController {

    private final AiGatewayRateLimitService aiGatewayRateLimitService;

    @SuppressFBWarnings("EI")
    AiGatewayRateLimitGraphQlController(AiGatewayRateLimitService aiGatewayRateLimitService) {
        this.aiGatewayRateLimitService = aiGatewayRateLimitService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public List<AiGatewayRateLimit> aiGatewayRateLimits(@Argument long workspaceId) {
        return aiGatewayRateLimitService.getRateLimitsByWorkspaceId(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayRateLimit createAiGatewayRateLimit(@Argument CreateAiGatewayRateLimitInput input) {
        AiGatewayRateLimit rateLimit = new AiGatewayRateLimit(
            Long.valueOf(input.workspaceId()), input.name(), input.scope(),
            input.limitType(), input.limitValue(), input.windowSeconds());

        if (input.enabled() != null) {
            rateLimit.setEnabled(input.enabled());
        }

        if (input.projectId() != null) {
            rateLimit.setProjectId(Long.valueOf(input.projectId()));
        }

        if (input.propertyKey() != null) {
            rateLimit.setPropertyKey(input.propertyKey());
        }

        return aiGatewayRateLimitService.create(rateLimit);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiGatewayRateLimit(@Argument long id) {
        aiGatewayRateLimitService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayRateLimit updateAiGatewayRateLimit(
        @Argument long id, @Argument UpdateAiGatewayRateLimitInput input) {

        AiGatewayRateLimit rateLimit = aiGatewayRateLimitService.getRateLimit(id);

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

        return aiGatewayRateLimitService.update(rateLimit);
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
