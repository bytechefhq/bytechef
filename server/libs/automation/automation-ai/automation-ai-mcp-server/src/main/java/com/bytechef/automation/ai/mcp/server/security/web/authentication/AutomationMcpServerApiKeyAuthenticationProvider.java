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

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import com.bytechef.platform.security.web.mcp.McpApiKeyCredentials;
import com.bytechef.platform.security.web.mcp.McpApiKeyEntity;
import com.bytechef.platform.security.web.mcp.McpApiKeyEntityRepository;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationProvider;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Authenticates automation MCP requests: delegates secret validation to mcp-server-security, then enforces that the key
 * is an AUTOMATION key whose environment matches the target MCP server's environment.
 *
 * @author Ivica Cardic
 */
public class AutomationMcpServerApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyAuthenticationProvider<McpApiKeyEntity> apiKeyAuthenticationProvider;
    private final ApiKeyService apiKeyService;
    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI")
    public AutomationMcpServerApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, AuthorityService authorityService, McpServerService mcpServerService,
        UserService userService) {

        this.apiKeyAuthenticationProvider = new ApiKeyAuthenticationProvider<>(
            new McpApiKeyEntityRepository(apiKeyService, authorityService, userService));
        this.apiKeyService = apiKeyService;
        this.mcpServerService = mcpServerService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthenticationToken apiKeyAuthenticationToken = (ApiKeyAuthenticationToken) authentication;

        McpApiKeyCredentials mcpApiKeyCredentials = (McpApiKeyCredentials) apiKeyAuthenticationToken.getCredentials();

        if (mcpApiKeyCredentials == null) {
            throw new BadCredentialsException("Credentials do not exist");
        }

        McpServer mcpServer = getMcpServer(mcpApiKeyCredentials.getMcpServerSecretKey());

        if (!mcpServer.isAuthenticationRequired()) {
            return new McpAnonymousAuthenticationToken(mcpApiKeyCredentials.getMcpServerSecretKey());
        }

        if (mcpApiKeyCredentials.getSecret() == null) {
            throw new BadCredentialsException("Authorization token does not exist");
        }

        Authentication authenticatedAuthentication = apiKeyAuthenticationProvider.authenticate(authentication);

        if (authenticatedAuthentication == null) {
            return null;
        }

        McpApiKeyEntity mcpApiKeyEntity = (McpApiKeyEntity) authenticatedAuthentication.getPrincipal();

        if (mcpApiKeyEntity == null) {
            throw new BadCredentialsException("Invalid API key");
        }

        if (mcpApiKeyEntity.getType() != PlatformType.AUTOMATION) {
            throw new BadCredentialsException("Invalid API key");
        }

        if (mcpServer.getEnvironment() != mcpApiKeyEntity.getEnvironment()) {
            throw new BadCredentialsException("Invalid API key");
        }

        apiKeyService.updateLastUsedDate(mcpApiKeyEntity.getApiKeyId());

        return authenticatedAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private McpServer getMcpServer(String mcpServerSecretKey) {
        try {
            return mcpServerService.getMcpServer(mcpServerSecretKey);
        } catch (Exception exception) {
            throw new BadCredentialsException("Invalid MCP server secret key", exception);
        }
    }
}
