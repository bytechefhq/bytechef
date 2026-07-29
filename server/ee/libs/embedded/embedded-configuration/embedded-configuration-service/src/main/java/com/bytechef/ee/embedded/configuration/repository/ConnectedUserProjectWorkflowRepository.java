/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface ConnectedUserProjectWorkflowRepository extends ListCrudRepository<ConnectedUserProjectWorkflow, Long> {

    Optional<ConnectedUserProjectWorkflow> findByConnectedUserProjectIdAndProjectWorkflowId(
        long connectedUserProjectId, long projectWorkflowId);

    Optional<ConnectedUserProjectWorkflow> findByConnectedUserProjectIdAndCatalogWorkflowUuid(
        long connectedUserProjectId, String catalogWorkflowUuid);

    @Query("""
        SELECT cupw.*
        FROM connected_user_project_workflow cupw
        WHERE cupw.connected_user_project_id = :connectedUserProjectId
        """)
    List<ConnectedUserProjectWorkflow> findAllByConnectedUserProjectId(
        @Param("connectedUserProjectId") Long connectedUserProjectId);

    /**
     * A connected user's automation-bridge references hang off their {@code ConnectedUserProject}, one level down from
     * the connected user itself, so this joins through that table rather than filtering directly -- mirroring how
     * {@code IntegrationInstanceService#getConnectedUserIntegrationInstances(connectedUserId, ...)} already resolves
     * the integration side from just the connected user's id.
     */
    @Query("""
        SELECT cupw.*
        FROM connected_user_project_workflow cupw
        JOIN connected_user_project cup ON cupw.connected_user_project_id = cup.id
        WHERE cup.connected_user_id = :connectedUserId
        """)
    List<ConnectedUserProjectWorkflow> findAllByConnectedUserId(@Param("connectedUserId") long connectedUserId);

    @Query("""
        SELECT cupw.*
        FROM connected_user_project_workflow cupw
        WHERE cupw.catalog_workflow_uuid IN (:catalogWorkflowUuids)
        """)
    List<ConnectedUserProjectWorkflow> findAllByCatalogWorkflowUuidIn(
        @Param("catalogWorkflowUuids") Set<String> catalogWorkflowUuids);
}
