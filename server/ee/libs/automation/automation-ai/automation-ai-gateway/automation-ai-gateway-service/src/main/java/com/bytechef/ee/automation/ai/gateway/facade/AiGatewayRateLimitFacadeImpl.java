/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayRateLimitService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimit;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayRateLimitService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiGatewayRateLimitFacade}. Delegates to the shared services and carries the authorization
 * guards so they are enforced for every caller of the facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiGatewayRateLimitFacadeImpl implements AiGatewayRateLimitFacade {

    private final AiGatewayRateLimitService aiGatewayRateLimitService;
    private final WorkspaceAiGatewayRateLimitService workspaceAiGatewayRateLimitService;

    @SuppressFBWarnings("EI")
    AiGatewayRateLimitFacadeImpl(
        AiGatewayRateLimitService aiGatewayRateLimitService,
        WorkspaceAiGatewayRateLimitService workspaceAiGatewayRateLimitService) {

        this.aiGatewayRateLimitService = aiGatewayRateLimitService;
        this.workspaceAiGatewayRateLimitService = workspaceAiGatewayRateLimitService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public List<AiGatewayRateLimit> getRateLimitsByWorkspaceId(long workspaceId) {
        return workspaceAiGatewayRateLimitService.getRateLimitsByWorkspaceId(workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayRateLimit getRateLimit(long id) {
        return aiGatewayRateLimitService.getRateLimit(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayRateLimit createInWorkspace(AiGatewayRateLimit rateLimit, long workspaceId) {
        return workspaceAiGatewayRateLimitService.createInWorkspace(rateLimit, workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void delete(long id) {
        aiGatewayRateLimitService.delete(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayRateLimit update(AiGatewayRateLimit rateLimit) {
        return aiGatewayRateLimitService.update(rateLimit);
    }
}
