/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayRateLimit;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Workspace ↔ AI Gateway rate limit membership repository.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewayRateLimitRepository extends ListCrudRepository<WorkspaceAiGatewayRateLimit, Long> {

    Optional<WorkspaceAiGatewayRateLimit> findByAiGatewayRateLimitId(long aiGatewayRateLimitId);

    @Query("""
        SELECT ai_gateway_rate_limit.*
        FROM ai_gateway_rate_limit
        JOIN workspace_ai_gateway_rate_limit
            ON workspace_ai_gateway_rate_limit.ai_gateway_rate_limit_id = ai_gateway_rate_limit.id
        WHERE workspace_ai_gateway_rate_limit.workspace_id = :workspaceId
        """)
    List<AiGatewayRateLimit> findRateLimitsByWorkspaceId(@Param("workspaceId") long workspaceId);

    @Query("""
        SELECT ai_gateway_rate_limit.*
        FROM ai_gateway_rate_limit
        JOIN workspace_ai_gateway_rate_limit
            ON workspace_ai_gateway_rate_limit.ai_gateway_rate_limit_id = ai_gateway_rate_limit.id
        WHERE workspace_ai_gateway_rate_limit.workspace_id = :workspaceId
            AND ai_gateway_rate_limit.enabled = true
        """)
    List<AiGatewayRateLimit> findEnabledRateLimitsByWorkspaceId(@Param("workspaceId") long workspaceId);
}
