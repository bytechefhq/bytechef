/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.exception;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class McpAutomationWorkflowNotFoundException extends RuntimeException {

    public McpAutomationWorkflowNotFoundException(String workflowUuid) {
        super("MCP automation workflow not found: " + workflowUuid);
    }
}
