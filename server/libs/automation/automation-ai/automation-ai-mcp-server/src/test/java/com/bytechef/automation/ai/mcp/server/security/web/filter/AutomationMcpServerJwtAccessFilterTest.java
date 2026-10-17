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

package com.bytechef.automation.ai.mcp.server.security.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * @author Ivica Cardic
 */
class AutomationMcpServerJwtAccessFilterTest {

    private static final long MCP_SERVER_ID = 9L;
    private static final String MCP_SERVER_SECRET_KEY = "server-secret";
    private static final String PATH = "/api/automation/" + MCP_SERVER_SECRET_KEY + "/mcp";
    private static final long WORKSPACE_ID = 3L;

    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final WorkspaceMcpServerService workspaceMcpServerService = mock(WorkspaceMcpServerService.class);
    private final EnvironmentService environmentService = () -> List.of(Environment.values());

    private final AutomationMcpServerJwtAccessFilter automationMcpServerJwtAccessFilter =
        new AutomationMcpServerJwtAccessFilter(
            environmentService, mcpServerService, permissionService, workspaceMcpServerService);

    @BeforeEach
    void beforeEach() {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.getEnvironment()).thenReturn(Environment.PRODUCTION);
        when(mcpServer.getId()).thenReturn(MCP_SERVER_ID);
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);
        when(workspaceMcpServerService.fetchMcpServerWorkspaceId(MCP_SERVER_ID)).thenReturn(Optional.of(WORKSPACE_ID));

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("user@localhost.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now()
                .plusSeconds(300))
            .build();

        SecurityContextHolder.getContext()
            .setAuthentication(new JwtAuthenticationToken(jwt, List.of(), "user@localhost.com"));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testWorkspaceMemberIsLetThrough() throws Exception {
        when(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).thenReturn("EDITOR");

        assertThat(doFilter(request(null))).isTrue();
    }

    @Test
    void testTenantAdminOutsideTheWorkspaceIsLetThrough() throws Exception {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        assertThat(doFilter(request(null))).isTrue();
    }

    @Test
    void testUserOutsideTheWorkspaceIsForbidden() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(doFilter(request(null), response)).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void testServerWithoutWorkspaceIsForbiddenToNonAdmins() throws Exception {
        when(workspaceMcpServerService.fetchMcpServerWorkspaceId(MCP_SERVER_ID)).thenReturn(Optional.empty());

        assertThat(doFilter(request(null))).isFalse();
    }

    @Test
    void testMatchingEnvironmentHeaderIsLetThrough() throws Exception {
        when(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).thenReturn("EDITOR");

        assertThat(doFilter(request("production"))).isTrue();
    }

    @Test
    void testEnvironmentHeaderOfAnotherEnvironmentIsForbidden() throws Exception {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(doFilter(request("DEVELOPMENT"), response)).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void testUnknownEnvironmentHeaderIsForbidden() throws Exception {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        assertThat(doFilter(request("moon"))).isFalse();
    }

    @Test
    void testNonJwtAuthenticationIsLeftToItsOwnProvider() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(UsernamePasswordAuthenticationToken.authenticated("user", null, List.of()));

        assertThat(doFilter(request("DEVELOPMENT"))).isTrue();
    }

    private boolean doFilter(MockHttpServletRequest request) throws Exception {
        return doFilter(request, new MockHttpServletResponse());
    }

    private boolean doFilter(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        AtomicBoolean chainInvoked = new AtomicBoolean();

        automationMcpServerJwtAccessFilter.doFilter(
            request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        return chainInvoked.get();
    }

    private static MockHttpServletRequest request(String environment) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", PATH);

        request.setServletPath(PATH);

        if (environment != null) {
            request.addHeader("X-ENVIRONMENT", environment);
        }

        return request;
    }
}
