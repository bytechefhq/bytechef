/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.repository;

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
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
public interface McpIntegrationInstanceToolRepository extends ListCrudRepository<McpIntegrationInstanceTool, Long> {

    void deleteByIntegrationInstanceId(long integrationInstanceId);

    void deleteByMcpToolId(long mcpToolId);

    List<McpIntegrationInstanceTool> findAllByIntegrationInstanceId(long integrationInstanceId);

    Optional<McpIntegrationInstanceTool> findByIntegrationInstanceIdAndMcpToolId(
        long integrationInstanceId, long mcpToolId);

    @Query("""
        SELECT DISTINCT mcp_integration_instance_tool.integration_instance_id
        FROM mcp_integration_instance_tool
        JOIN mcp_tool ON mcp_tool.id = mcp_integration_instance_tool.mcp_tool_id
        JOIN mcp_component ON mcp_component.id = mcp_tool.mcp_component_id
        WHERE mcp_component.mcp_server_id = :mcpServerId
        """)
    List<Long> findIntegrationInstanceIdsByMcpServerId(@Param("mcpServerId") long mcpServerId);
}
