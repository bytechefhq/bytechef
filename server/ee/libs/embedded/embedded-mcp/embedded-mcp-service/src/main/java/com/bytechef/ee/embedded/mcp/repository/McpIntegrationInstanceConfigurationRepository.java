/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.repository;

import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfiguration;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link McpIntegrationInstanceConfiguration} entities.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Repository
public interface McpIntegrationInstanceConfigurationRepository
    extends ListCrudRepository<McpIntegrationInstanceConfiguration, Long> {

    void deleteAllByIntegrationInstanceConfigurationId(Long integrationInstanceConfigurationId);

    List<McpIntegrationInstanceConfiguration>
        findAllByIntegrationInstanceConfigurationId(Long integrationInstanceConfigurationId);

    @Query("SELECT mi.* FROM mcp_integration_instance_configuration mi " +
        "JOIN integration_instance_configuration iic ON mi.integration_instance_configuration_id = iic.id " +
        "WHERE iic.integration_id = :integrationId")
    List<McpIntegrationInstanceConfiguration> findAllByIntegrationId(@Param("integrationId") Long integrationId);

    List<McpIntegrationInstanceConfiguration> findAllByMcpServerId(Long mcpServerId);
}
