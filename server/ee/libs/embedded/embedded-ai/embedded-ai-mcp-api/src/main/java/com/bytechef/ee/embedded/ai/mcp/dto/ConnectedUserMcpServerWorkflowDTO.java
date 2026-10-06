/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.dto;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ConnectedUserMcpServerWorkflowDTO(
    long integrationInstanceId, String componentName, int integrationVersion, String workflowId, String name,
    @Nullable String description, boolean enabled, @Nullable Instant lastExecutionDate) {
}
