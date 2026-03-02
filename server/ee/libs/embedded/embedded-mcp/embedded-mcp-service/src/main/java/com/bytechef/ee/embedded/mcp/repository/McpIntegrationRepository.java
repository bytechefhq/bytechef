/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.repository;

import com.bytechef.ee.embedded.mcp.domain.McpIntegration;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link McpIntegration} entities.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Repository
public interface McpIntegrationRepository extends ListCrudRepository<McpIntegration, Long> {

    /**
     * Find all integrations associated with a specific MCP server.
     *
     * @param mcpServerId the ID of the MCP server
     * @return list of integrations associated with the specified server
     */
    List<McpIntegration> findAllByMcpServerId(Long mcpServerId);
}
