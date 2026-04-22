/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiGatewayRequestLogFacade}. Delegates to the shared {@code AiLlmUsageService} and carries
 * the authorization guards so they are enforced for every caller of the facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiGatewayRequestLogFacadeImpl implements AiGatewayRequestLogFacade {

    private final AiLlmUsageService aiLlmUsageService;

    @SuppressFBWarnings("EI")
    AiGatewayRequestLogFacadeImpl(AiLlmUsageService aiLlmUsageService) {
        this.aiLlmUsageService = aiLlmUsageService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiLlmUsage> getRequestLogs(Instant startDate, Instant endDate) {
        return aiLlmUsageService.getRequestLogs(startDate, endDate);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public List<AiLlmUsage> getRequestLogsByWorkspace(Long workspaceId, Instant startDate, Instant endDate) {
        return aiLlmUsageService.getRequestLogsByWorkspace(workspaceId, startDate, endDate);
    }
}
