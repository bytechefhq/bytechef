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

import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Ivica Cardic
 */
public class AutomationMcpServerJwtAccessFilter extends OncePerRequestFilter {

    private static final String ENVIRONMENT_HEADER_NAME = "X-ENVIRONMENT";
    private static final Pattern SECRET_KEY_PATH_PATTERN = Pattern.compile("^/api/automation/([^/]+)/mcp/?$");

    private final EnvironmentService environmentService;
    private final McpServerService mcpServerService;
    private final PermissionService permissionService;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI2")
    public AutomationMcpServerJwtAccessFilter(
        EnvironmentService environmentService, McpServerService mcpServerService, PermissionService permissionService,
        WorkspaceMcpServerService workspaceMcpServerService) {

        this.environmentService = environmentService;
        this.mcpServerService = mcpServerService;
        this.permissionService = permissionService;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        Matcher matcher = SECRET_KEY_PATH_PATTERN.matcher(request.getServletPath());

        if (!(authentication instanceof JwtAuthenticationToken) || !matcher.matches()) {
            filterChain.doFilter(request, response);

            return;
        }

        Optional<McpServer> mcpServer = fetchMcpServer(matcher.group(1));

        if (mcpServer.isEmpty() || !isRequestedEnvironment(request, mcpServer.get()) ||
            !isWorkspaceMember(mcpServer.get())) {

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            return;
        }

        filterChain.doFilter(request, response);
    }

    private Optional<McpServer> fetchMcpServer(String mcpServerSecretKey) {
        try {
            return Optional.of(mcpServerService.getMcpServer(mcpServerSecretKey));
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }
    }

    private boolean isRequestedEnvironment(HttpServletRequest request, McpServer mcpServer) {
        String requestedEnvironmentName = request.getHeader(ENVIRONMENT_HEADER_NAME);

        if (requestedEnvironmentName == null) {
            return true;
        }

        Environment requestedEnvironment;

        try {
            requestedEnvironment = environmentService.getEnvironment(requestedEnvironmentName);
        } catch (IllegalArgumentException illegalArgumentException) {
            return false;
        }

        return requestedEnvironment == mcpServer.getEnvironment();
    }

    private boolean isWorkspaceMember(McpServer mcpServer) {
        if (permissionService.isTenantAdmin()) {
            return true;
        }

        return workspaceMcpServerService.fetchMcpServerWorkspaceId(mcpServer.getId())
            .map(workspaceId -> permissionService.getMyWorkspaceRole(workspaceId) != null)
            .orElse(false);
    }
}
