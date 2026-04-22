/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewaySpendSummary;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Workspace ↔ AI Gateway spend-summary membership repository.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewaySpendSummaryRepository
    extends ListCrudRepository<WorkspaceAiGatewaySpendSummary, Long> {

    Optional<WorkspaceAiGatewaySpendSummary> findByAiGatewaySpendSummaryId(long aiGatewaySpendSummaryId);

    @Query("""
        SELECT ai_gateway_spend_summary.*
        FROM ai_gateway_spend_summary
        JOIN workspace_ai_gateway_spend_summary
            ON workspace_ai_gateway_spend_summary.ai_gateway_spend_summary_id = ai_gateway_spend_summary.id
        WHERE workspace_ai_gateway_spend_summary.workspace_id = :workspaceId
            AND ai_gateway_spend_summary.period_start BETWEEN :start AND :end
        """)
    List<AiGatewaySpendSummary> findSpendSummariesByWorkspaceIdAndPeriodStartBetween(
        @Param("workspaceId") long workspaceId, @Param("start") Instant start, @Param("end") Instant end);
}
