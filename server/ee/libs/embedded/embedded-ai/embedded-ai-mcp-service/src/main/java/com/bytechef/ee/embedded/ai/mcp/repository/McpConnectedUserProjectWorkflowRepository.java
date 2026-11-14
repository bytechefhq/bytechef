/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.repository;

import com.bytechef.ee.embedded.ai.mcp.domain.McpConnectedUserProjectWorkflow;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
@ConditionalOnEEVersion
public interface McpConnectedUserProjectWorkflowRepository
    extends ListCrudRepository<McpConnectedUserProjectWorkflow, Long> {

    Optional<McpConnectedUserProjectWorkflow>
        findByConnectedUserProjectWorkflowIdAndMcpAutomationProjectConfigurationWorkflowId(
            long connectedUserProjectWorkflowId, long mcpAutomationProjectConfigurationWorkflowId);

    @Query("""
        SELECT DISTINCT connected_user_project.connected_user_id
        FROM mcp_connected_user_project_workflow
        JOIN connected_user_project_workflow
            ON connected_user_project_workflow.id =
                mcp_connected_user_project_workflow.connected_user_project_workflow_id
        JOIN connected_user_project
            ON connected_user_project.id = connected_user_project_workflow.connected_user_project_id
        JOIN mcp_automation_project_configuration_workflow
            ON mcp_automation_project_configuration_workflow.id =
                mcp_connected_user_project_workflow.mcp_automation_project_configuration_workflow_id
        JOIN mcp_automation_project_configuration
            ON mcp_automation_project_configuration.id =
                mcp_automation_project_configuration_workflow.mcp_automation_project_configuration_id
        WHERE mcp_automation_project_configuration.mcp_server_id = :mcpServerId
        """)
    List<Long> findConnectedUserIdsByMcpServerId(@Param("mcpServerId") long mcpServerId);

    @Query("""
        SELECT mcp_connected_user_project_workflow.*
        FROM mcp_connected_user_project_workflow
        JOIN connected_user_project_workflow
            ON connected_user_project_workflow.id =
                mcp_connected_user_project_workflow.connected_user_project_workflow_id
        JOIN connected_user_project
            ON connected_user_project.id = connected_user_project_workflow.connected_user_project_id
        WHERE connected_user_project.connected_user_id = :connectedUserId
        """)
    List<McpConnectedUserProjectWorkflow> findAllByConnectedUserId(@Param("connectedUserId") long connectedUserId);

    @Query("""
        SELECT mcp_connected_user_project_workflow.*
        FROM mcp_connected_user_project_workflow
        JOIN mcp_automation_project_configuration_workflow
            ON mcp_automation_project_configuration_workflow.id =
                mcp_connected_user_project_workflow.mcp_automation_project_configuration_workflow_id
        JOIN mcp_automation_project_configuration
            ON mcp_automation_project_configuration.id =
                mcp_automation_project_configuration_workflow.mcp_automation_project_configuration_id
        WHERE mcp_automation_project_configuration.mcp_server_id = :mcpServerId
        """)
    List<McpConnectedUserProjectWorkflow> findAllByMcpServerId(@Param("mcpServerId") long mcpServerId);
}
