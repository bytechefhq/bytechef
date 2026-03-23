/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.repository;

import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
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

}
