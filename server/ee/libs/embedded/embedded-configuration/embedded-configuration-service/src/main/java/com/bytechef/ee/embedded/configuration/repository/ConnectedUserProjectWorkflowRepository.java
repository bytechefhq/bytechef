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

    Optional<ConnectedUserProjectWorkflow> findByConnectedUserProjectIdAndAutomationWorkflowUuid(
        long connectedUserProjectId, String automationWorkflowUuid);

    @Query("""
        SELECT cupw.*
        FROM connected_user_project_workflow cupw
        WHERE cupw.connected_user_project_id = :connectedUserProjectId
        """)
    List<ConnectedUserProjectWorkflow> findAllByConnectedUserProjectId(
        @Param("connectedUserProjectId") Long connectedUserProjectId);

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
        WHERE cupw.project_deployment_id = :projectDeploymentId
        """)
    List<ConnectedUserProjectWorkflow> findAllByProjectDeploymentId(
        @Param("projectDeploymentId") long projectDeploymentId);

    @Query("""
        SELECT cupw.*
        FROM connected_user_project_workflow cupw
        WHERE cupw.automation_workflow_uuid IN (:automationWorkflowUuids)
        """)
    List<ConnectedUserProjectWorkflow> findAllByAutomationWorkflowUuidIn(
        @Param("automationWorkflowUuids") Set<String> automationWorkflowUuids);
}
