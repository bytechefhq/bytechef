/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayProject;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Workspace ↔ AI Gateway project membership repository.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewayProjectRepository extends ListCrudRepository<WorkspaceAiGatewayProject, Long> {

    Optional<WorkspaceAiGatewayProject> findByAiGatewayProjectId(long aiGatewayProjectId);

    List<WorkspaceAiGatewayProject> findAllByWorkspaceId(long workspaceId);

    @Query("""
        SELECT ai_gateway_project.*
        FROM ai_gateway_project
        JOIN workspace_ai_gateway_project
            ON workspace_ai_gateway_project.ai_gateway_project_id = ai_gateway_project.id
        WHERE workspace_ai_gateway_project.workspace_id = :workspaceId
        """)
    List<AiGatewayProject> findProjectsByWorkspaceId(@Param("workspaceId") long workspaceId);

    @Query("""
        SELECT ai_gateway_project.*
        FROM ai_gateway_project
        JOIN workspace_ai_gateway_project
            ON workspace_ai_gateway_project.ai_gateway_project_id = ai_gateway_project.id
        WHERE workspace_ai_gateway_project.workspace_id = :workspaceId
            AND ai_gateway_project.slug = :slug
        """)
    Optional<AiGatewayProject> findProjectByWorkspaceIdAndSlug(
        @Param("workspaceId") long workspaceId, @Param("slug") String slug);
}
