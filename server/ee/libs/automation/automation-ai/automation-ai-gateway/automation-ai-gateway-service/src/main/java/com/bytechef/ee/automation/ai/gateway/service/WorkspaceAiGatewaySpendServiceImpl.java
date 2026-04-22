/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewaySpendSummary;
import com.bytechef.ee.automation.ai.gateway.repository.WorkspaceAiGatewaySpendSummaryRepository;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewaySpendService;
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
class WorkspaceAiGatewaySpendServiceImpl implements WorkspaceAiGatewaySpendService {

    private final AiGatewaySpendService aiGatewaySpendService;
    private final WorkspaceAiGatewaySpendSummaryRepository workspaceAiGatewaySpendSummaryRepository;

    public WorkspaceAiGatewaySpendServiceImpl(
        AiGatewaySpendService aiGatewaySpendService,
        WorkspaceAiGatewaySpendSummaryRepository workspaceAiGatewaySpendSummaryRepository) {

        this.aiGatewaySpendService = aiGatewaySpendService;
        this.workspaceAiGatewaySpendSummaryRepository = workspaceAiGatewaySpendSummaryRepository;
    }

    @Override
    public AiGatewaySpendSummary createInWorkspace(AiGatewaySpendSummary summary, long workspaceId) {
        AiGatewaySpendSummary savedSummary = aiGatewaySpendService.create(summary);

        workspaceAiGatewaySpendSummaryRepository.save(
            new WorkspaceAiGatewaySpendSummary(savedSummary.getId(), workspaceId));

        return savedSummary;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewaySpendSummary> getSpendSummariesByWorkspaceId(
        long workspaceId, Instant start, Instant end) {

        return workspaceAiGatewaySpendSummaryRepository.findSpendSummariesByWorkspaceIdAndPeriodStartBetween(
            workspaceId, start, end);
    }
}
