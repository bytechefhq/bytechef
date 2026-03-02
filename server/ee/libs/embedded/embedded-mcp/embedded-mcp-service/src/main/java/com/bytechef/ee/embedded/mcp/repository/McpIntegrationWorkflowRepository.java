/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.repository;

import com.bytechef.ee.embedded.mcp.domain.McpIntegrationWorkflow;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link McpIntegrationWorkflow} entities.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Repository
public interface McpIntegrationWorkflowRepository extends ListCrudRepository<McpIntegrationWorkflow, Long> {

    /**
     * Finds all MCP integration workflows that belong to the specified MCP integration.
     *
     * @param mcpIntegrationId the ID of the MCP integration to filter by
     * @return a list of MCP integration workflows with the specified MCP integration ID
     */
    @Query("""
        SELECT * FROM mcp_integration_workflow
        WHERE mcp_integration_id = :mcpIntegrationId
        ORDER BY id ASC
        """)
    List<McpIntegrationWorkflow> findAllByMcpIntegrationId(@Param("mcpIntegrationId") Long mcpIntegrationId);
}
