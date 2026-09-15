/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.ai.mcp.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @author Ivica Cardic
 */
class AutomationMcpComponentConnectionUsageCheckerTest {

    private static final long CONNECTION_ID = 8L;
    private static final long MCP_SERVER_ID = 5L;
    private static final long WORKSPACE_ID = 42L;

    private final McpServerRepository mcpServerRepository = mock(McpServerRepository.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final WorkspaceMcpServerService workspaceMcpServerService = mock(WorkspaceMcpServerService.class);

    private final AutomationMcpComponentConnectionUsageChecker automationMcpComponentConnectionUsageChecker =
        new AutomationMcpComponentConnectionUsageChecker(
            mcpServerRepository, permissionService, workspaceMcpServerService);

    @Test
    void testAllowsAConnectionUsableInTheServersWorkspaceAndEnvironment() {
        givenServerInWorkspace();

        when(permissionService.canUseConnectionInWorkspace(CONNECTION_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(true);

        assertThatCode(
            () -> automationMcpComponentConnectionUsageChecker.checkConnectionUsage(MCP_SERVER_ID, CONNECTION_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void testRefusesAConnectionNotUsableInTheServersWorkspaceAndEnvironment() {
        givenServerInWorkspace();

        when(permissionService.canUseConnectionInWorkspace(CONNECTION_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(false);

        assertThatThrownBy(
            () -> automationMcpComponentConnectionUsageChecker.checkConnectionUsage(MCP_SERVER_ID, CONNECTION_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRefusesANonAdminForAServerOfNoWorkspace() {
        when(mcpServerRepository.findById(MCP_SERVER_ID)).thenReturn(Optional.of(createMcpServer()));
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(MCP_SERVER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> automationMcpComponentConnectionUsageChecker.checkConnectionUsage(MCP_SERVER_ID, CONNECTION_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testAllowsATenantAdminForAServerOfNoWorkspace() {
        when(mcpServerRepository.findById(MCP_SERVER_ID)).thenReturn(Optional.of(createMcpServer()));
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(MCP_SERVER_ID)).thenReturn(Optional.empty());
        when(permissionService.isTenantAdmin()).thenReturn(true);

        assertThatCode(
            () -> automationMcpComponentConnectionUsageChecker.checkConnectionUsage(MCP_SERVER_ID, CONNECTION_ID))
                .doesNotThrowAnyException();
    }

    private void givenServerInWorkspace() {
        when(mcpServerRepository.findById(MCP_SERVER_ID)).thenReturn(Optional.of(createMcpServer()));
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(MCP_SERVER_ID))
            .thenReturn(Optional.of(WORKSPACE_ID));
    }

    private static McpServer createMcpServer() {
        McpServer mcpServer = new McpServer("server", PlatformType.AUTOMATION, Environment.PRODUCTION);

        mcpServer.setId(MCP_SERVER_ID);

        return mcpServer;
    }
}
