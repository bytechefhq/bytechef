/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.exception.ForbiddenException;
import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpServer;
import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpServerFacade;
import com.bytechef.ee.ai.hub.web.graphql.AiHubMcpServerGraphQlController.AiHubMcpServerResult;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 * @author Ivica Cardic
 */
class AiHubMcpServerGraphQlControllerTest {

    private static final long USER_ID = 10L;
    private static final long WORKSPACE_ID = 7L;

    private final AiHubMcpServerFacade mcpServerFacade = mock(AiHubMcpServerFacade.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final UserService userService = mock(UserService.class);
    private final AiHubMcpServerGraphQlController controller = new AiHubMcpServerGraphQlController(
        mcpServerFacade, permissionService, userService);

    @BeforeEach
    void beforeEach() {
        User user = mock(User.class);

        when(user.getId()).thenReturn(USER_ID);
        when(userService.getCurrentUser()).thenReturn(user);
        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).thenReturn(true);
    }

    @Test
    void testAiHubMcpServersListsTheCallersServersWithoutExposingTheToken() {
        AiHubMcpServer mcpServer = new AiHubMcpServer(
            USER_ID, WORKSPACE_ID, 0, "docs", "https://mcp.example.com/mcp", "encrypted-secret");

        mcpServer.setId(5L);

        when(mcpServerFacade.listMcpServers(USER_ID, WORKSPACE_ID)).thenReturn(List.of(mcpServer));

        assertThat(controller.aiHubMcpServers(WORKSPACE_ID)).containsExactly(
            new AiHubMcpServerResult(5L, "docs", "https://mcp.example.com/mcp", true, true));
    }

    @Test
    void testRemoveAiHubMcpServerIsScopedToTheCallerAndWorkspace() {
        when(mcpServerFacade.removeMcpServer(USER_ID, WORKSPACE_ID, 5L)).thenReturn(false);

        assertThat(controller.removeAiHubMcpServer(WORKSPACE_ID, 5L)).isFalse();

        verify(mcpServerFacade).removeMcpServer(USER_ID, WORKSPACE_ID, 5L);
    }

    @Test
    void testCallerWithoutAWorkspaceRoleIsRejected() {
        assertThatThrownBy(() -> controller.aiHubMcpServers(99L)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> controller.removeAiHubMcpServer(99L, 5L)).isInstanceOf(ForbiddenException.class);

        verify(mcpServerFacade, never()).listMcpServers(anyLong(), anyLong());
        verify(mcpServerFacade, never()).removeMcpServer(anyLong(), anyLong(), anyLong());
    }
}
