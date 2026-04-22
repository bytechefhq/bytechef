/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayRateLimit;
import com.bytechef.ee.automation.ai.gateway.repository.WorkspaceAiGatewayRateLimitRepository;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimit;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayRateLimitService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class WorkspaceAiGatewayRateLimitServiceImpl implements WorkspaceAiGatewayRateLimitService {

    private final AiGatewayRateLimitService aiGatewayRateLimitService;
    private final WorkspaceAiGatewayRateLimitRepository workspaceAiGatewayRateLimitRepository;

    public WorkspaceAiGatewayRateLimitServiceImpl(
        AiGatewayRateLimitService aiGatewayRateLimitService,
        WorkspaceAiGatewayRateLimitRepository workspaceAiGatewayRateLimitRepository) {

        this.aiGatewayRateLimitService = aiGatewayRateLimitService;
        this.workspaceAiGatewayRateLimitRepository = workspaceAiGatewayRateLimitRepository;
    }

    @Override
    public AiGatewayRateLimit createInWorkspace(AiGatewayRateLimit rateLimit, long workspaceId) {
        Validate.notNull(rateLimit, "'rateLimit' must not be null");

        AiGatewayRateLimit savedRateLimit = aiGatewayRateLimitService.create(rateLimit);

        workspaceAiGatewayRateLimitRepository.save(
            new WorkspaceAiGatewayRateLimit(savedRateLimit.getId(), workspaceId));

        return savedRateLimit;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayRateLimit> getRateLimitsByWorkspaceId(long workspaceId) {
        return workspaceAiGatewayRateLimitRepository.findRateLimitsByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayRateLimit> getEnabledRateLimitsByWorkspaceId(long workspaceId) {
        return workspaceAiGatewayRateLimitRepository.findEnabledRateLimitsByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long rateLimitId) {
        return workspaceAiGatewayRateLimitRepository.findByAiGatewayRateLimitId(rateLimitId)
            .map(WorkspaceAiGatewayRateLimit::getWorkspaceId)
            .orElse(null);
    }
}
