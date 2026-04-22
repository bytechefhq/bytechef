/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRequestLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @version ee
 */
public interface AiGatewayRequestLogRepository extends ListCrudRepository<AiGatewayRequestLog, Long> {

    List<AiGatewayRequestLog> findAllByCreatedDateBetween(Instant start, Instant end);

    List<AiGatewayRequestLog> findAllByWorkspaceIdAndCreatedDateBetween(
        Long workspaceId, Instant start, Instant end);

    List<AiGatewayRequestLog> findAllByStatusAndCreatedDateAfter(Integer status, Instant after);

    void deleteAllByCreatedDateBefore(Instant date);

    void deleteAllByWorkspaceIdAndCreatedDateBefore(Long workspaceId, Instant date);

    @Query("SELECT DISTINCT workspace_id FROM ai_gateway_request_log WHERE workspace_id IS NOT NULL")
    List<Long> findDistinctWorkspaceIds();

    /**
     * Returns request logs in the given workspace+window whose id has a matching custom-property row for the given
     * key/value pair. Used by the dashboard's custom-property filter; chain multiple filters client-side for multi-key
     * AND semantics since Spring Data JDBC doesn't support array params well in {@code @Query}.
     */
    @Query("""
        SELECT DISTINCT r.* FROM ai_gateway_request_log r
        JOIN ai_gateway_custom_property p ON p.request_log_id = r.id
        WHERE r.workspace_id = :workspaceId
          AND r.created_date BETWEEN :start AND :end
          AND p.key = :propertyKey
          AND p.value = :propertyValue
        ORDER BY r.created_date DESC
        """)
    List<AiGatewayRequestLog> findAllByWorkspaceIdAndCustomProperty(
        @Param("workspaceId") Long workspaceId,
        @Param("start") Instant start,
        @Param("end") Instant end,
        @Param("propertyKey") String propertyKey,
        @Param("propertyValue") String propertyValue);
}
