/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.service;

import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpAutomationProjectConfigurationService {

    McpAutomationProjectConfiguration create(McpAutomationProjectConfiguration mcpAutomationProjectConfiguration);

    void delete(long id);

    Optional<McpAutomationProjectConfiguration> fetch(long id);

    List<McpAutomationProjectConfiguration>
        getAutomationProjectConfigurationMcpAutomationProjectConfigurations(long automationProjectConfigurationId);

    List<McpAutomationProjectConfiguration> getMcpServerMcpAutomationProjectConfigurations(long mcpServerId);
}
