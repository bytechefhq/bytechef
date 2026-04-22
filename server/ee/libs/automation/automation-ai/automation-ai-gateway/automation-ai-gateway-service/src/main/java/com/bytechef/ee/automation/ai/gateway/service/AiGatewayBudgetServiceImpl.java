/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayBudget;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayBudgetRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
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
class AiGatewayBudgetServiceImpl implements AiGatewayBudgetService {

    private static final String HARD_BLOCKED_CACHE = "ai-gateway-hard-blocked";

    private final AiGatewayBudgetRepository aiGatewayBudgetRepository;
    private final CacheManager cacheManager;

    public AiGatewayBudgetServiceImpl(
        AiGatewayBudgetRepository aiGatewayBudgetRepository,
        CacheManager cacheManager) {

        this.aiGatewayBudgetRepository = aiGatewayBudgetRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    public AiGatewayBudget create(AiGatewayBudget budget) {
        Validate.notNull(budget, "'budget' must not be null");
        Validate.isTrue(budget.getId() == null, "'id' must be null");

        AiGatewayBudget savedBudget = aiGatewayBudgetRepository.save(budget);

        evictHardBlockedCache(savedBudget.getWorkspaceId());

        return savedBudget;
    }

    @Override
    public void delete(long id) {
        aiGatewayBudgetRepository.findById(id)
            .ifPresent(budget -> evictHardBlockedCache(budget.getWorkspaceId()));

        aiGatewayBudgetRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiGatewayBudget getBudget(long id) {
        return aiGatewayBudgetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Budget not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiGatewayBudget> getBudgetByWorkspaceId(long workspaceId) {
        return aiGatewayBudgetRepository.findByWorkspaceId(workspaceId);
    }

    @Override
    public AiGatewayBudget update(AiGatewayBudget budget) {
        Validate.notNull(budget, "'budget' must not be null");

        AiGatewayBudget existingBudget = aiGatewayBudgetRepository.findById(budget.getId())
            .orElseThrow(() -> new IllegalArgumentException("Budget not found: " + budget.getId()));

        existingBudget.setAlertThreshold(budget.getAlertThreshold());
        existingBudget.setAmount(budget.getAmount());
        existingBudget.setEnabled(budget.isEnabled());
        existingBudget.setEnforcementMode(budget.getEnforcementMode());
        existingBudget.setPeriod(budget.getPeriod());

        AiGatewayBudget savedBudget = aiGatewayBudgetRepository.save(existingBudget);

        evictHardBlockedCache(savedBudget.getWorkspaceId());

        return savedBudget;
    }

    private void evictHardBlockedCache(long workspaceId) {
        Cache cache = cacheManager.getCache(HARD_BLOCKED_CACHE);

        if (cache != null) {
            cache.evict(workspaceId);
        }
    }
}
