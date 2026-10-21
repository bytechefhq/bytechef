/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
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
public interface ConnectUserProjectRepository extends ListCrudRepository<ConnectedUserProject, Long> {

    @Query("""
        SELECT COUNT(cup.*) > 0
        FROM connected_user_project cup
        JOIN project ON cup.project_id = project.id
        JOIN project_deployment ON project.id = project_deployment.project_id
        WHERE project_deployment.id = :projectDeploymentId
        LIMIT 1
        """)
    boolean existsByProjectDeploymentId(@Param("projectDeploymentId") long projectDeploymentId);

    @Query("SELECT COUNT(cup.*) > 0 FROM connected_user_project cup WHERE cup.project_id = :projectId")
    boolean existsByProjectId(@Param("projectId") long projectId);

    @Query("""
        SELECT COUNT(*) > 0
        FROM project_workflow pw
        WHERE pw.workflow_id = :workflowId
          AND (
            EXISTS (SELECT 1 FROM automation_workflow_project awp WHERE awp.project_id = pw.project_id)
            OR EXISTS (SELECT 1 FROM connected_user_project cup WHERE cup.project_id = pw.project_id))
        """)
    boolean existsEmbeddedProjectByWorkflowId(@Param("workflowId") String workflowId);

    @Query("""
        SELECT cup.*
        FROM connected_user_project cup
        JOIN connected_user cu ON cup.connected_user_id = cu.id
        WHERE cu.environment = :environment AND cu.external_id = :externalUserId
        LIMIT 1
        """)
    Optional<ConnectedUserProject> findFirstByEnvironmentAndExternalUserId(
        @Param("externalUserId") String externalUserId, @Param("environment") int environment);

    @Query("""
        SELECT cup.connected_user_id
        FROM connected_user_project cup
        JOIN project ON cup.project_id = project.id
        JOIN project_deployment ON project.id = project_deployment.project_id
        WHERE project_deployment.id = :projectDeploymentId
        LIMIT 1
        """)
    Optional<Long> findConnectedUserIdByProjectDeploymentId(@Param("projectDeploymentId") long projectDeploymentId);

    List<ConnectedUserProject> findAllByConnectedUserId(long connectedUserId);

    Optional<ConnectedUserProject> findByConnectedUserId(Long connectedUserid);

    @Query("SELECT * FROM connected_user_project WHERE id = :id FOR UPDATE")
    Optional<ConnectedUserProject> findByIdForUpdate(@Param("id") long id);
}
