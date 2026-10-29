/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.exception;

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectedUserMcpServerErrorType extends AbstractErrorType {

    public static final ConnectedUserMcpServerErrorType NO_COVERED_INTEGRATION =
        new ConnectedUserMcpServerErrorType(100);

    private ConnectedUserMcpServerErrorType(int errorKey) {
        super(McpIntegrationInstanceTool.class, errorKey);
    }
}
