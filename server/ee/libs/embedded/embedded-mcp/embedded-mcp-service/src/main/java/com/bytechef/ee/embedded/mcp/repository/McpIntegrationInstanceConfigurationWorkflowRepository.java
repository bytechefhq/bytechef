/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.repository;

import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
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
public interface McpIntegrationInstanceConfigurationWorkflowRepository
    extends ListCrudRepository<McpIntegrationInstanceConfigurationWorkflow, Long> {

    /**
     * Finds all MCP integration workflows that belong to the specified MCP integration.
     *
     * @param mcpIntegrationInstanceConfigurationId the ID of the MCP integration to filter by
     * @return a list of MCP integration workflows with the specified MCP integration ID
     */
    @Query("""
        SELECT * FROM mcp_integration_instance_configuration_workflow
        WHERE mcp_integration_instance_configuration_id = :mcpIntegrationInstanceConfigurationId
        ORDER BY id ASC
        """)
    List<McpIntegrationInstanceConfigurationWorkflow> findAllByMcpIntegrationInstanceConfigurationId(
        @Param("mcpIntegrationInstanceConfigurationId") Long mcpIntegrationInstanceConfigurationId);

    Optional<McpIntegrationInstanceConfigurationWorkflow> findByIntegrationInstanceConfigurationWorkflowId(
        long integrationInstanceConfigurationWorkflowId);

    @Query("""
        SELECT miw.* FROM mcp_integration_instance_configuration_workflow miw
        JOIN integration_instance_configuration_workflow iicw
            ON miw.integration_instance_configuration_workflow_id = iicw.id
        WHERE iicw.workflow_id = :workflowId
        """)
    Optional<McpIntegrationInstanceConfigurationWorkflow> findByWorkflowId(@Param("workflowId") String workflowId);

    @Modifying
    @Query("""
        DELETE FROM mcp_integration_instance_configuration_workflow
        WHERE integration_instance_configuration_workflow_id = :integrationInstanceConfigurationWorkflowId
        """)
    void deleteByIntegrationInstanceConfigurationWorkflowId(
        @Param("integrationInstanceConfigurationWorkflowId") long integrationInstanceConfigurationWorkflowId);
}
