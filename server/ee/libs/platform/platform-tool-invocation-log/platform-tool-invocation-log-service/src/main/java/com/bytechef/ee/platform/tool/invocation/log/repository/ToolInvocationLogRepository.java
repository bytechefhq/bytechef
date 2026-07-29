/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.repository;

import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ToolInvocationLogRepository extends ListCrudRepository<ToolInvocationLog, Long> {

    @Query("""
        SELECT * FROM tool_invocation_log
        WHERE (:surface IS NULL OR surface = :surface)
            AND (:outcome IS NULL OR outcome = :outcome)
            AND (:mcpServerId IS NULL OR mcp_server_id = :mcpServerId)
            AND (:connectedUserId IS NULL OR connected_user_id = :connectedUserId)
            AND (:integrationInstanceId IS NULL OR integration_instance_id = :integrationInstanceId)
            AND (:startDate IS NULL OR created_date >= :startDate)
            AND (:endDate IS NULL OR created_date <= :endDate)
        ORDER BY created_date DESC
        LIMIT :limit OFFSET :offset
        """)
    List<ToolInvocationLog> findFiltered(
        @Param("surface") @Nullable Integer surface, @Param("outcome") @Nullable Integer outcome,
        @Param("mcpServerId") @Nullable Long mcpServerId, @Param("connectedUserId") @Nullable Long connectedUserId,
        @Param("integrationInstanceId") @Nullable Long integrationInstanceId,
        @Param("startDate") @Nullable Instant startDate, @Param("endDate") @Nullable Instant endDate,
        @Param("limit") int limit, @Param("offset") long offset);

    @Query("""
        SELECT COUNT(*) FROM tool_invocation_log
        WHERE (:surface IS NULL OR surface = :surface)
            AND (:outcome IS NULL OR outcome = :outcome)
            AND (:mcpServerId IS NULL OR mcp_server_id = :mcpServerId)
            AND (:connectedUserId IS NULL OR connected_user_id = :connectedUserId)
            AND (:integrationInstanceId IS NULL OR integration_instance_id = :integrationInstanceId)
            AND (:startDate IS NULL OR created_date >= :startDate)
            AND (:endDate IS NULL OR created_date <= :endDate)
        """)
    long countFiltered(
        @Param("surface") @Nullable Integer surface, @Param("outcome") @Nullable Integer outcome,
        @Param("mcpServerId") @Nullable Long mcpServerId, @Param("connectedUserId") @Nullable Long connectedUserId,
        @Param("integrationInstanceId") @Nullable Long integrationInstanceId,
        @Param("startDate") @Nullable Instant startDate, @Param("endDate") @Nullable Instant endDate);

    @Modifying
    @Query("DELETE FROM tool_invocation_log WHERE created_date < :instant")
    int deleteByCreatedDateBefore(@Param("instant") Instant instant);
}
