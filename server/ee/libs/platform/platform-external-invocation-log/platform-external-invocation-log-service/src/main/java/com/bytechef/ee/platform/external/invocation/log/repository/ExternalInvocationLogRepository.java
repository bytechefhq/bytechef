/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.repository;

import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
public interface ExternalInvocationLogRepository extends ListCrudRepository<ExternalInvocationLog, Long> {

    @Query("""
        SELECT * FROM external_invocation_log
        WHERE type = :type
            AND (:workspaceId IS NULL OR workspace_id = :workspaceId)
            AND (:environment IS NULL OR environment = :environment)
            AND (:source IS NULL OR source = :source)
            AND (:status IS NULL OR status = :status)
            AND (:mcpServerId IS NULL OR mcp_server_id = :mcpServerId)
            AND (:a2aServerId IS NULL OR a2a_server_id = :a2aServerId)
            AND (:connectedUserId IS NULL OR connected_user_id = :connectedUserId)
            AND (:integrationInstanceId IS NULL OR integration_instance_id = :integrationInstanceId)
            AND (CAST(:startDate AS TIMESTAMP) IS NULL OR created_date >= CAST(:startDate AS TIMESTAMP))
            AND (CAST(:endDate AS TIMESTAMP) IS NULL OR created_date <= CAST(:endDate AS TIMESTAMP))
        ORDER BY created_date DESC
        LIMIT :limit OFFSET :offset
        """)
    List<ExternalInvocationLog> findFiltered(
        @Param("type") int type, @Param("workspaceId") @Nullable Long workspaceId,
        @Param("environment") @Nullable Integer environment,
        @Param("source") @Nullable Integer source, @Param("status") @Nullable Integer status,
        @Param("mcpServerId") @Nullable Long mcpServerId, @Param("a2aServerId") @Nullable Long a2aServerId,
        @Param("connectedUserId") @Nullable Long connectedUserId,
        @Param("integrationInstanceId") @Nullable Long integrationInstanceId,
        @Param("startDate") @Nullable Instant startDate, @Param("endDate") @Nullable Instant endDate,
        @Param("limit") int limit, @Param("offset") long offset);

    @Query("""
        SELECT COUNT(*) FROM external_invocation_log
        WHERE type = :type
            AND (:workspaceId IS NULL OR workspace_id = :workspaceId)
            AND (:environment IS NULL OR environment = :environment)
            AND (:source IS NULL OR source = :source)
            AND (:status IS NULL OR status = :status)
            AND (:mcpServerId IS NULL OR mcp_server_id = :mcpServerId)
            AND (:a2aServerId IS NULL OR a2a_server_id = :a2aServerId)
            AND (:connectedUserId IS NULL OR connected_user_id = :connectedUserId)
            AND (:integrationInstanceId IS NULL OR integration_instance_id = :integrationInstanceId)
            AND (CAST(:startDate AS TIMESTAMP) IS NULL OR created_date >= CAST(:startDate AS TIMESTAMP))
            AND (CAST(:endDate AS TIMESTAMP) IS NULL OR created_date <= CAST(:endDate AS TIMESTAMP))
        """)
    long countFiltered(
        @Param("type") int type, @Param("workspaceId") @Nullable Long workspaceId,
        @Param("environment") @Nullable Integer environment,
        @Param("source") @Nullable Integer source, @Param("status") @Nullable Integer status,
        @Param("mcpServerId") @Nullable Long mcpServerId, @Param("a2aServerId") @Nullable Long a2aServerId,
        @Param("connectedUserId") @Nullable Long connectedUserId,
        @Param("integrationInstanceId") @Nullable Long integrationInstanceId,
        @Param("startDate") @Nullable Instant startDate, @Param("endDate") @Nullable Instant endDate);

    @Query("""
        SELECT EXISTS (
            SELECT 1 FROM external_invocation_log
            WHERE type = :type AND environment = :environment AND status = :status
        )
        """)
    boolean existsByTypeAndEnvironmentAndStatus(
        @Param("type") int type, @Param("environment") int environment, @Param("status") int status);

    @Query("""
        SELECT * FROM external_invocation_log
        WHERE type = :type AND environment = :environment AND status NOT IN (:nonFailedStatuses)
        ORDER BY created_date DESC
        LIMIT 1
        """)
    Optional<ExternalInvocationLog> findLastFailed(
        @Param("type") int type, @Param("environment") int environment,
        @Param("nonFailedStatuses") Collection<Integer> nonFailedStatuses);

    @Modifying
    @Query("DELETE FROM external_invocation_log WHERE created_date < :instant")
    int deleteByCreatedDateBefore(@Param("instant") Instant instant);
}
