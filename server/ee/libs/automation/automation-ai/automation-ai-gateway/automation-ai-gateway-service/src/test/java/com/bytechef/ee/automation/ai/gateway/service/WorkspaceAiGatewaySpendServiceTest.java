/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewaySpendService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link WorkspaceAiGatewaySpendServiceImpl}. The behaviors worth locking down are (a) that
 * {@code createInWorkspace} stamps the owning workspace onto the summary before delegating to the platform CRUD
 * service, and (b) that the per-workspace query routes through the workspace-scoped finder rather than the
 * cross-workspace one — a tenant-isolation regression that static typing would not catch because both finders share the
 * same return type.
 *
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceAiGatewaySpendServiceTest {

    @Mock
    private AiGatewaySpendService aiGatewaySpendService;

    private WorkspaceAiGatewaySpendService workspaceAiGatewaySpendService;

    @BeforeEach
    void setUp() {
        workspaceAiGatewaySpendService = new WorkspaceAiGatewaySpendServiceImpl(aiGatewaySpendService);
    }

    @Test
    void testCreateInWorkspaceDelegatesAndStampsWorkspaceId() {
        Instant now = Instant.now();
        AiGatewaySpendSummary summary = new AiGatewaySpendSummary(now.minusSeconds(3600), now);
        AiGatewaySpendSummary persisted = new AiGatewaySpendSummary(now.minusSeconds(3600), now);

        org.springframework.test.util.ReflectionTestUtils.setField(persisted, "id", 42L);

        when(aiGatewaySpendService.create(any(AiGatewaySpendSummary.class))).thenReturn(persisted);

        AiGatewaySpendSummary result = workspaceAiGatewaySpendService.createInWorkspace(summary, 7L);

        assertThat(result).isSameAs(persisted);

        ArgumentCaptor<AiGatewaySpendSummary> summaryArgumentCaptor =
            ArgumentCaptor.forClass(AiGatewaySpendSummary.class);

        verify(aiGatewaySpendService).create(summaryArgumentCaptor.capture());

        AiGatewaySpendSummary createdSummary = summaryArgumentCaptor.getValue();

        assertThat(createdSummary).isSameAs(summary);
        assertThat(createdSummary.getWorkspaceId())
            .as("the owning workspace must be stamped on the row before it is inserted — it is the only record of " +
                "the association now that the membership table is gone")
            .isEqualTo(7L);
    }

    @Test
    void testGetSpendSummariesByWorkspaceIdCallsWorkspaceScopedFinder() {
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = Instant.parse("2025-02-01T00:00:00Z");
        long workspaceId = 42L;

        AiGatewaySpendSummary summary = new AiGatewaySpendSummary(start, end);

        when(aiGatewaySpendService.getSpendSummariesByWorkspaceId(workspaceId, start, end))
            .thenReturn(List.of(summary));

        List<AiGatewaySpendSummary> summaries =
            workspaceAiGatewaySpendService.getSpendSummariesByWorkspaceId(workspaceId, start, end);

        assertThat(summaries).containsExactly(summary);

        verify(aiGatewaySpendService).getSpendSummariesByWorkspaceId(workspaceId, start, end);
        verify(aiGatewaySpendService, never()).getSpendSummaries(start, end);
    }
}
