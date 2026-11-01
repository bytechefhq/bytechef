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
public final class ConnectedUserMcpServerAuditEvents {

    public static final String CONNECTED_USER_MCP_SERVER_DELETED = "CONNECTED_USER_MCP_SERVER_DELETED";
    public static final String CONNECTED_USER_MCP_SERVER_ENABLED_UPDATED =
        "CONNECTED_USER_MCP_SERVER_ENABLED_UPDATED";
    public static final String CONNECTED_USER_MCP_SERVER_PROVISIONED = "CONNECTED_USER_MCP_SERVER_PROVISIONED";

    private ConnectedUserMcpServerAuditEvents() {
    }
}
