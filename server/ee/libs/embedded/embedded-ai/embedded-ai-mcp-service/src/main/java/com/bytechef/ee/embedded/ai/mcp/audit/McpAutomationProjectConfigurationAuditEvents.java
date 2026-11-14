/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.audit;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class McpAutomationProjectConfigurationAuditEvents {

    public static final String MCP_AUTOMATION_PROJECT_CONFIGURATION_CREATED =
        "MCP_AUTOMATION_PROJECT_CONFIGURATION_CREATED";
    public static final String MCP_AUTOMATION_PROJECT_CONFIGURATION_DELETED =
        "MCP_AUTOMATION_PROJECT_CONFIGURATION_DELETED";
    public static final String MCP_AUTOMATION_PROJECT_CONFIGURATION_UPDATED =
        "MCP_AUTOMATION_PROJECT_CONFIGURATION_UPDATED";
    public static final String MCP_AUTOMATION_PROJECT_CONFIGURATION_WORKFLOW_ENABLED_UPDATED =
        "MCP_AUTOMATION_PROJECT_CONFIGURATION_WORKFLOW_ENABLED_UPDATED";

    private McpAutomationProjectConfigurationAuditEvents() {
    }
}
