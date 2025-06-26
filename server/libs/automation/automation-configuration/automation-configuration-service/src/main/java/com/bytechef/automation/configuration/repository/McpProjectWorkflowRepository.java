/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.repository;

import com.bytechef.platform.configuration.domain.McpProjectWorkflow;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link McpProjectWorkflow} entities.
 *
 * @author Ivica Cardic
 */
@Repository
public interface McpProjectWorkflowRepository extends ListCrudRepository<McpProjectWorkflow, Long> {

    /**
     * Finds all MCP project workflows that belong to the specified MCP project.
     *
     * @param mcpProjectId the ID of the MCP project to filter by
     * @return a list of MCP project workflows with the specified MCP project ID
     */
    @Query("""
        SELECT * FROM mcp_project_workflow
        WHERE mcp_project_id = :mcpProjectId
        ORDER BY id ASC
        """)
    List<McpProjectWorkflow> findAllByMcpProjectId(@Param("mcpProjectId") Long mcpProjectId);

    /**
     * Finds all MCP project workflows that belong to the specified project deployment workflow.
     *
     * @param projectDeploymentWorkflowId the ID of the project deployment workflow to filter by
     * @return a list of MCP project workflows with the specified project deployment workflow ID
     */
    @Query("""
        SELECT * FROM mcp_project_workflow
        WHERE project_deployment_workflow_id = :projectDeploymentWorkflowId
        ORDER BY id ASC
        """)
    List<McpProjectWorkflow>
        findAllByProjectDeploymentWorkflowId(@Param("projectDeploymentWorkflowId") Long projectDeploymentWorkflowId);
}
