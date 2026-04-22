/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewaySpendSummary;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewaySpendSummaryRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link AiGatewaySpendServiceImpl}. The service is a thin pass-through to the repository, so the
 * behaviors worth locking down are (a) that {@code create} persists through {@code save} and (b) that the per-workspace
 * query does not silently widen to the cross-workspace finder (a tenant-isolation regression that static typing would
 * not catch because both finders share the same return type).
 *
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiGatewaySpendServiceTest {

    @Mock
    private AiGatewaySpendSummaryRepository aiGatewaySpendSummaryRepository;

    private AiGatewaySpendService aiGatewaySpendService;

    @BeforeEach
    void setUp() {
        aiGatewaySpendService = new AiGatewaySpendServiceImpl(aiGatewaySpendSummaryRepository);
    }

    @Test
    void testCreateDelegatesToRepositorySave() {
        Instant now = Instant.now();
        AiGatewaySpendSummary summary = new AiGatewaySpendSummary(now.minusSeconds(3600), now);

        when(aiGatewaySpendSummaryRepository.save(any(AiGatewaySpendSummary.class)))
            .thenReturn(summary);

        AiGatewaySpendSummary result = aiGatewaySpendService.create(summary);

        assertThat(result).isSameAs(summary);

        verify(aiGatewaySpendSummaryRepository).save(summary);
    }

    @Test
    void testGetSpendSummariesByWorkspaceIdCallsWorkspaceScopedFinder() {
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = Instant.parse("2025-02-01T00:00:00Z");
        long workspaceId = 42L;

        AiGatewaySpendSummary summary = new AiGatewaySpendSummary(start, end);

        when(aiGatewaySpendSummaryRepository.findAllByWorkspaceIdAndPeriodStartBetween(workspaceId, start, end))
            .thenReturn(List.of(summary));

        List<AiGatewaySpendSummary> summaries =
            aiGatewaySpendService.getSpendSummariesByWorkspaceId(workspaceId, start, end);

        assertThat(summaries).containsExactly(summary);

        verify(aiGatewaySpendSummaryRepository).findAllByWorkspaceIdAndPeriodStartBetween(workspaceId, start, end);
    }

    @Test
    void testGetSpendSummariesCallsUnscopedFinder() {
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = Instant.parse("2025-02-01T00:00:00Z");

        when(aiGatewaySpendSummaryRepository.findAllByPeriodStartBetween(start, end))
            .thenReturn(List.of());

        List<AiGatewaySpendSummary> summaries = aiGatewaySpendService.getSpendSummaries(start, end);

        assertThat(summaries).isEmpty();

        verify(aiGatewaySpendSummaryRepository).findAllByPeriodStartBetween(start, end);
    }
}
