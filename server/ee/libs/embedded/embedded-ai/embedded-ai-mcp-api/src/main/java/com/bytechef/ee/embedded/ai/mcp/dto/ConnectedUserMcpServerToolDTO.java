/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.dto;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ConnectedUserMcpServerToolDTO(
    long id, String componentName, int componentVersion, long integrationInstanceId, String name, boolean enabled) {
}
