/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record McpAutomationProjectConfigurationDTO(
    long id, long mcpServerId, long automationProjectConfigurationId, String automationProjectConfigurationName,
    long projectId, int projectVersion, boolean automationProjectConfigurationEnabled,
    List<McpAutomationProjectConfigurationWorkflowDTO> workflows) {
}
