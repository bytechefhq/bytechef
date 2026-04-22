/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayBudget;
import com.bytechef.ee.automation.ai.gateway.repository.WorkspaceAiGatewayBudgetRepository;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudget;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayBudgetService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class WorkspaceAiGatewayBudgetServiceImpl implements WorkspaceAiGatewayBudgetService {

    private static final String HARD_BLOCKED_CACHE = "ai-gateway-hard-blocked";

    private final AiGatewayBudgetService aiGatewayBudgetService;
    private final CacheManager cacheManager;
    private final WorkspaceAiGatewayBudgetRepository workspaceAiGatewayBudgetRepository;

    public WorkspaceAiGatewayBudgetServiceImpl(
        AiGatewayBudgetService aiGatewayBudgetService,
        CacheManager cacheManager,
        WorkspaceAiGatewayBudgetRepository workspaceAiGatewayBudgetRepository) {

        this.aiGatewayBudgetService = aiGatewayBudgetService;
        this.cacheManager = cacheManager;
        this.workspaceAiGatewayBudgetRepository = workspaceAiGatewayBudgetRepository;
    }

    @Override
    public AiGatewayBudget createInWorkspace(AiGatewayBudget budget, long workspaceId) {
        AiGatewayBudget savedBudget = aiGatewayBudgetService.create(budget);

        workspaceAiGatewayBudgetRepository.save(new WorkspaceAiGatewayBudget(savedBudget.getId(), workspaceId));

        evictHardBlockedCache(workspaceId);

        return savedBudget;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiGatewayBudget> getBudgetByWorkspaceId(long workspaceId) {
        return workspaceAiGatewayBudgetRepository.findBudgetByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long budgetId) {
        return workspaceAiGatewayBudgetRepository.findByAiGatewayBudgetId(budgetId)
            .map(WorkspaceAiGatewayBudget::getWorkspaceId)
            .orElse(null);
    }

    private void evictHardBlockedCache(long workspaceId) {
        Cache cache = cacheManager.getCache(HARD_BLOCKED_CACHE);

        if (cache != null) {
            cache.evict(workspaceId);
        }
    }
}
