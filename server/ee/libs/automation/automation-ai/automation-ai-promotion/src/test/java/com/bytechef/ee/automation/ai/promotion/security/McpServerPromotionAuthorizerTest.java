/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.promotion.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class McpServerPromotionAuthorizerTest {

    private final WorkspaceMcpServerService workspaceMcpServerService = mock(WorkspaceMcpServerService.class);

    private final McpServerPromotionAuthorizer mcpServerPromotionAuthorizer =
        new McpServerPromotionAuthorizer(workspaceMcpServerService);

    @Test
    void testWorkspaceIdOfMcpServerIsResolvedThroughItsWorkspaceAssignment() {
        when(workspaceMcpServerService.fetchMcpServerWorkspaceId(100L)).thenReturn(Optional.of(5L));

        assertThat(mcpServerPromotionAuthorizer.workspaceIdOfMcpServer(100L)).isEqualTo(5L);
    }

    @Test
    void testMcpServerWithoutAWorkspaceIsRejectedAsAccessDeniedRatherThanAsNotFound() {
        when(workspaceMcpServerService.fetchMcpServerWorkspaceId(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcpServerPromotionAuthorizer.workspaceIdOfMcpServer(404L))
            .isInstanceOf(AccessDeniedException.class);
    }
}
