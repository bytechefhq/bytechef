/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewaySpendSummary;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewaySpendSummaryRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.time.Instant;
import java.util.List;
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
class AiGatewaySpendServiceImpl implements AiGatewaySpendService {

    private final AiGatewaySpendSummaryRepository aiGatewaySpendSummaryRepository;

    public AiGatewaySpendServiceImpl(
        AiGatewaySpendSummaryRepository aiGatewaySpendSummaryRepository) {

        this.aiGatewaySpendSummaryRepository = aiGatewaySpendSummaryRepository;
    }

    @Override
    public AiGatewaySpendSummary create(AiGatewaySpendSummary summary) {
        return aiGatewaySpendSummaryRepository.save(summary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewaySpendSummary> getSpendSummaries(Instant start, Instant end) {
        return aiGatewaySpendSummaryRepository.findAllByPeriodStartBetween(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewaySpendSummary> getSpendSummariesByWorkspaceId(
        long workspaceId, Instant start, Instant end) {

        return aiGatewaySpendSummaryRepository.findAllByWorkspaceIdAndPeriodStartBetween(
            workspaceId, start, end);
    }
}
