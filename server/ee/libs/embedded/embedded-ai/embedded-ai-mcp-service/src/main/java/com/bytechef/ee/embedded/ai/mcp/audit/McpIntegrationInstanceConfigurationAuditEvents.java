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
public final class McpIntegrationInstanceConfigurationAuditEvents {

    public static final String MCP_INTEGRATION_INSTANCE_CONFIGURATION_CREATED =
        "MCP_INTEGRATION_INSTANCE_CONFIGURATION_CREATED";
    public static final String MCP_INTEGRATION_INSTANCE_CONFIGURATION_DELETED =
        "MCP_INTEGRATION_INSTANCE_CONFIGURATION_DELETED";
    public static final String MCP_INTEGRATION_INSTANCE_CONFIGURATION_UPDATED =
        "MCP_INTEGRATION_INSTANCE_CONFIGURATION_UPDATED";
    public static final String MCP_INTEGRATION_INSTANCE_CONFIGURATION_VERSION_UPDATED =
        "MCP_INTEGRATION_INSTANCE_CONFIGURATION_VERSION_UPDATED";
    public static final String MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_CREATED =
        "MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_CREATED";
    public static final String MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_DELETED =
        "MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_DELETED";
    public static final String MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ENABLED_UPDATED =
        "MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ENABLED_UPDATED";

    private McpIntegrationInstanceConfigurationAuditEvents() {
    }
}
