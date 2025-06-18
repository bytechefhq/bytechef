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

    @Query("""
        SELECT cupw.*
        FROM connected_user_project_workflow cupw
        WHERE cupw.connected_user_project_id = :connectedUserProjectId
        """)
    List<ConnectedUserProjectWorkflow> findAllByConnectedUserProjectId(
        @Param("connectedUserProjectId") Long connectedUserProjectId);

    @Query("""
        SELECT connected_user_project_workflow_connection.connection_id FROM connected_user_project_workflow_connection
        WHERE connection_id = :connectionId
        """)
    List<Long> findConnectedUserProjectWorkflowConnectionIdsByConnectionId(@Param("connectionId") long connectionId);
}
