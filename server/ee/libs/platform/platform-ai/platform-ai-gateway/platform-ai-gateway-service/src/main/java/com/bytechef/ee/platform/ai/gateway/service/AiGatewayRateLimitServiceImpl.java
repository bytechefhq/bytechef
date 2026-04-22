/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimit;
import com.bytechef.ee.platform.ai.gateway.repository.AiGatewayRateLimitRepository;
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
class AiGatewayRateLimitServiceImpl implements AiGatewayRateLimitService {

    private final AiGatewayRateLimitRepository aiGatewayRateLimitRepository;

    public AiGatewayRateLimitServiceImpl(AiGatewayRateLimitRepository aiGatewayRateLimitRepository) {
        this.aiGatewayRateLimitRepository = aiGatewayRateLimitRepository;
    }

    @Override
    public AiGatewayRateLimit create(AiGatewayRateLimit rateLimit) {
        Validate.notNull(rateLimit, "'rateLimit' must not be null");
        Validate.isTrue(rateLimit.getId() == null, "'id' must be null");

        return aiGatewayRateLimitRepository.save(rateLimit);
    }

    @Override
    public void delete(long id) {
        aiGatewayRateLimitRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiGatewayRateLimit getRateLimit(long id) {
        return aiGatewayRateLimitRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Rate limit not found: " + id));
    }

    @Override
    public AiGatewayRateLimit update(AiGatewayRateLimit rateLimit) {
        Validate.notNull(rateLimit, "'rateLimit' must not be null");

        AiGatewayRateLimit existingRateLimit = aiGatewayRateLimitRepository.findById(rateLimit.getId())
            .orElseThrow(() -> new IllegalArgumentException("Rate limit not found: " + rateLimit.getId()));

        existingRateLimit.setEnabled(rateLimit.isEnabled());
        existingRateLimit.setLimitType(rateLimit.getLimitType());
        existingRateLimit.setLimitValue(rateLimit.getLimitValue());
        existingRateLimit.setName(rateLimit.getName());
        existingRateLimit.setProjectId(rateLimit.getProjectId());
        existingRateLimit.setPropertyKey(rateLimit.getPropertyKey());
        existingRateLimit.setScope(rateLimit.getScope());
        existingRateLimit.setWindowSeconds(rateLimit.getWindowSeconds());

        return aiGatewayRateLimitRepository.save(existingRateLimit);
    }
}
