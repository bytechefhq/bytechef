/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudget;
import com.bytechef.ee.platform.ai.gateway.repository.AiGatewayBudgetRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
class AiGatewayBudgetServiceImpl implements AiGatewayBudgetService {

    private final AiGatewayBudgetRepository aiGatewayBudgetRepository;

    public AiGatewayBudgetServiceImpl(AiGatewayBudgetRepository aiGatewayBudgetRepository) {
        this.aiGatewayBudgetRepository = aiGatewayBudgetRepository;
    }

    @Override
    public AiGatewayBudget create(AiGatewayBudget budget) {
        Validate.notNull(budget, "'budget' must not be null");
        Validate.isTrue(budget.getId() == null, "'id' must be null");

        return aiGatewayBudgetRepository.save(budget);
    }

    @Override
    public void delete(long id) {
        aiGatewayBudgetRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiGatewayBudget getBudget(long id) {
        return aiGatewayBudgetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Budget not found: " + id));
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

        return aiGatewayBudgetRepository.save(existingBudget);
    }
}
