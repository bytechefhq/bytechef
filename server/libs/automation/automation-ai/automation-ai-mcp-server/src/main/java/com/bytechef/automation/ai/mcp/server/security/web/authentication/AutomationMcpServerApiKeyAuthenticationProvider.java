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

package com.bytechef.automation.ai.mcp.server.security.web.authentication;

import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.WorkspaceApiKeyService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import com.bytechef.platform.security.web.mcp.McpApiKeyCredentials;
import com.bytechef.platform.security.web.mcp.McpApiKeyEntity;
import com.bytechef.platform.security.web.mcp.McpApiKeyEntityRepository;
import com.bytechef.platform.security.web.mcp.McpServerDisabledException;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @author Ivica Cardic
 */
public class AutomationMcpServerApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyService apiKeyService;
    private final McpApiKeyEntityRepository mcpApiKeyEntityRepository;
    private final McpServerService mcpServerService;
    private final PermissionService permissionService;
    private final WorkspaceApiKeyService workspaceApiKeyService;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public AutomationMcpServerApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, AuthorityService authorityService, McpServerService mcpServerService,
        PermissionService permissionService, UserService userService, WorkspaceApiKeyService workspaceApiKeyService,
        WorkspaceMcpServerService workspaceMcpServerService) {

        this.apiKeyService = apiKeyService;
        this.mcpApiKeyEntityRepository = new McpApiKeyEntityRepository(apiKeyService, authorityService, userService);
        this.mcpServerService = mcpServerService;
        this.permissionService = permissionService;
        this.workspaceApiKeyService = workspaceApiKeyService;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthenticationToken apiKeyAuthenticationToken = (ApiKeyAuthenticationToken) authentication;

        if (!(apiKeyAuthenticationToken.getCredentials() instanceof McpApiKeyCredentials mcpApiKeyCredentials)) {
            throw new BadCredentialsException("Credentials do not exist");
        }

        McpServer mcpServer = getMcpServer(mcpApiKeyCredentials.getMcpServerSecretKey());

        Environment requestedEnvironment = mcpApiKeyCredentials.getRequestedEnvironment();

        if (requestedEnvironment != null && requestedEnvironment != mcpServer.getEnvironment()) {
            throw new BadCredentialsException("X-ENVIRONMENT does not match the MCP server environment");
        }

        if (!mcpServer.isAuthenticationRequired()) {
            checkEnabled(mcpServer);

            return McpAnonymousAuthenticationToken.ofAutomationMcpServer(mcpServer.getId());
        }

        String secretKey = mcpApiKeyCredentials.getSecret();

        if (secretKey == null) {
            throw new BadCredentialsException("Authorization token does not exist");
        }

        McpApiKeyEntity mcpApiKeyEntity = mcpApiKeyEntityRepository.findByKeyId(secretKey);

        if (mcpApiKeyEntity == null) {
            throw new BadCredentialsException("Invalid API key");
        }

        if (mcpApiKeyEntity.getType() != PlatformType.AUTOMATION) {
            throw new BadCredentialsException("Invalid API key");
        }

        if (mcpServer.getEnvironment() != mcpApiKeyEntity.getEnvironment()) {
            throw new BadCredentialsException("Invalid API key");
        }

        long workspaceId = fetchSharedWorkspaceId(mcpApiKeyEntity.getApiKeyId(), mcpServer.getId())
            .orElseThrow(() -> new BadCredentialsException("Invalid API key"));

        Authentication apiKeyAuthentication = ApiKeyAuthenticationToken.authenticated(
            mcpApiKeyEntity, mcpApiKeyEntity.getAuthorities());

        if (!isWorkspaceMember(apiKeyAuthentication, workspaceId)) {
            throw new BadCredentialsException("Invalid API key");
        }

        checkEnabled(mcpServer);

        apiKeyService.updateLastUsedDate(mcpApiKeyEntity.getApiKeyId());

        return apiKeyAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static void checkEnabled(McpServer mcpServer) {
        if (!mcpServer.isEnabled()) {
            throw new McpServerDisabledException(McpServerDisabledException.MCP_SERVER_MESSAGE);
        }
    }

    private Optional<Long> fetchSharedWorkspaceId(long apiKeyId, Long mcpServerId) {
        Optional<Long> apiKeyWorkspaceId = workspaceApiKeyService.fetchApiKeyWorkspaceId(apiKeyId);

        if (apiKeyWorkspaceId.isEmpty() ||
            !Objects.equals(apiKeyWorkspaceId, workspaceMcpServerService.fetchMcpServerWorkspaceId(mcpServerId))) {

            return Optional.empty();
        }

        return apiKeyWorkspaceId;
    }

    private boolean isWorkspaceMember(Authentication apiKeyAuthentication, long workspaceId) {
        return SecurityUtils.runAs(
            apiKeyAuthentication, () -> permissionService.getMyWorkspaceRole(workspaceId) != null);
    }

    private McpServer getMcpServer(@Nullable String mcpServerSecretKey) {
        if (mcpServerSecretKey == null) {
            throw new BadCredentialsException("Invalid MCP server secret key");
        }

        McpServer mcpServer;

        try {
            mcpServer = mcpServerService.getMcpServer(mcpServerSecretKey);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new BadCredentialsException("Invalid MCP server secret key", illegalArgumentException);
        }

        if (mcpServer.getType() != PlatformType.AUTOMATION) {
            throw new BadCredentialsException("Invalid MCP server secret key");
        }

        return mcpServer;
    }
}
