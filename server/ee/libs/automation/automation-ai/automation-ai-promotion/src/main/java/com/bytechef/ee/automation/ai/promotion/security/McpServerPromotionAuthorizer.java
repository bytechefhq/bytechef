/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.promotion.security;

import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("mcpServerPromotionAuthorizer")
@ConditionalOnEEVersion
public class McpServerPromotionAuthorizer {

    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public McpServerPromotionAuthorizer(WorkspaceMcpServerService workspaceMcpServerService) {
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    public long workspaceIdOfMcpServer(long mcpServerId) {
        return workspaceMcpServerService.fetchMcpServerWorkspaceId(mcpServerId)
            .orElseThrow(() -> new AccessDeniedException("Access Denied"));
    }
}
