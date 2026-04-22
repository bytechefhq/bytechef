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

import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewaySpendSummary;
import com.bytechef.ee.automation.ai.gateway.repository.WorkspaceAiGatewaySpendSummaryRepository;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewaySpendService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link WorkspaceAiGatewaySpendServiceImpl}. The behaviors worth locking down are (a) that
 * {@code createInWorkspace} delegates to the platform CRUD service AND inserts the workspace membership row in the same
 * call, and (b) that the per-workspace query routes through the workspace JOIN finder rather than the cross-workspace
 * platform finder — a tenant-isolation regression that static typing would not catch because both finders share the
 * same return type.
 *
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceAiGatewaySpendServiceTest {

    @Mock
    private AiGatewaySpendService aiGatewaySpendService;

    @Mock
    private WorkspaceAiGatewaySpendSummaryRepository workspaceAiGatewaySpendSummaryRepository;

    private WorkspaceAiGatewaySpendService workspaceAiGatewaySpendService;

    @BeforeEach
    void setUp() {
        workspaceAiGatewaySpendService = new WorkspaceAiGatewaySpendServiceImpl(
            aiGatewaySpendService, workspaceAiGatewaySpendSummaryRepository);
    }

    @Test
    void testCreateInWorkspaceDelegatesAndInsertsMembership() {
        Instant now = Instant.now();
        AiGatewaySpendSummary summary = new AiGatewaySpendSummary(now.minusSeconds(3600), now);
        AiGatewaySpendSummary persisted = new AiGatewaySpendSummary(now.minusSeconds(3600), now);

        org.springframework.test.util.ReflectionTestUtils.setField(persisted, "id", 42L);

        when(aiGatewaySpendService.create(any(AiGatewaySpendSummary.class))).thenReturn(persisted);

        AiGatewaySpendSummary result = workspaceAiGatewaySpendService.createInWorkspace(summary, 7L);

        assertThat(result).isSameAs(persisted);

        verify(aiGatewaySpendService).create(summary);
        verify(workspaceAiGatewaySpendSummaryRepository).save(any(WorkspaceAiGatewaySpendSummary.class));
    }

    @Test
    void testGetSpendSummariesByWorkspaceIdCallsWorkspaceScopedFinder() {
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = Instant.parse("2025-02-01T00:00:00Z");
        long workspaceId = 42L;

        AiGatewaySpendSummary summary = new AiGatewaySpendSummary(start, end);

        when(workspaceAiGatewaySpendSummaryRepository.findSpendSummariesByWorkspaceIdAndPeriodStartBetween(
            workspaceId, start, end))
                .thenReturn(List.of(summary));

        List<AiGatewaySpendSummary> summaries =
            workspaceAiGatewaySpendService.getSpendSummariesByWorkspaceId(workspaceId, start, end);

        assertThat(summaries).containsExactly(summary);

        verify(workspaceAiGatewaySpendSummaryRepository).findSpendSummariesByWorkspaceIdAndPeriodStartBetween(
            workspaceId, start, end);
    }
}
