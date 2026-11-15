/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record McpAutomationWorkflowConfigurationDTO(String workflowUuid, Map<String, ?> parameters) {
}
