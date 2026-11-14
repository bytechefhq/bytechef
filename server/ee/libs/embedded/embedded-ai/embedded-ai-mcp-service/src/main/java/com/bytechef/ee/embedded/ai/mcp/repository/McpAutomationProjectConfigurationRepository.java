/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.repository;

import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnEEVersion
public interface McpAutomationProjectConfigurationRepository
    extends ListCrudRepository<McpAutomationProjectConfiguration, Long> {

    List<McpAutomationProjectConfiguration>
        findAllByAutomationProjectConfigurationId(long automationProjectConfigurationId);

    List<McpAutomationProjectConfiguration> findAllByMcpServerId(long mcpServerId);
}
