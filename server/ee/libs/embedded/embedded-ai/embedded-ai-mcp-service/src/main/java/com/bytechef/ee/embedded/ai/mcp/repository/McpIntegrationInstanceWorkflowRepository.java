/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.repository;

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceWorkflow;
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
public interface McpIntegrationInstanceWorkflowRepository
    extends ListCrudRepository<McpIntegrationInstanceWorkflow, Long> {

    List<McpIntegrationInstanceWorkflow> findAllByIntegrationInstanceId(long integrationInstanceId);

    Optional<McpIntegrationInstanceWorkflow>
        findByIntegrationInstanceIdAndMcpIntegrationInstanceConfigurationWorkflowId(
            long integrationInstanceId, long mcpIntegrationInstanceConfigurationWorkflowId);

    @Query("""
        SELECT DISTINCT mcp_integration_instance_workflow.integration_instance_id
        FROM mcp_integration_instance_workflow
        JOIN mcp_integration_instance_configuration_workflow
            ON mcp_integration_instance_configuration_workflow.id =
                mcp_integration_instance_workflow.mcp_integration_instance_configuration_workflow_id
        JOIN mcp_integration_instance_configuration
            ON mcp_integration_instance_configuration.id =
                mcp_integration_instance_configuration_workflow.mcp_integration_instance_configuration_id
        WHERE mcp_integration_instance_configuration.mcp_server_id = :mcpServerId
        """)
    List<Long> findIntegrationInstanceIdsByMcpServerId(@Param("mcpServerId") long mcpServerId);
}
