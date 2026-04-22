/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayBudget;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudget;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Workspace ↔ AI Gateway budget membership repository. The budget service writes to this in the same transaction as the
 * parent insert; readers go through {@code findBudgetByWorkspaceId} for the workspace dimension.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewayBudgetRepository extends ListCrudRepository<WorkspaceAiGatewayBudget, Long> {

    Optional<WorkspaceAiGatewayBudget> findByAiGatewayBudgetId(long aiGatewayBudgetId);

    Optional<WorkspaceAiGatewayBudget> findByWorkspaceId(long workspaceId);

    @Query("""
        SELECT ai_gateway_budget.*
        FROM ai_gateway_budget
        JOIN workspace_ai_gateway_budget
            ON workspace_ai_gateway_budget.ai_gateway_budget_id = ai_gateway_budget.id
        WHERE workspace_ai_gateway_budget.workspace_id = :workspaceId
        """)
    Optional<AiGatewayBudget> findBudgetByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
