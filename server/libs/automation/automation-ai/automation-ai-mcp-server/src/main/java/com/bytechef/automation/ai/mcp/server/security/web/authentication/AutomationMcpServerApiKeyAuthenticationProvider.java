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

    @SuppressFBWarnings("EI")
    public AutomationMcpServerApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, AuthorityService authorityService, McpServerService mcpServerService,
        UserService userService) {

        this.apiKeyService = apiKeyService;
        this.mcpApiKeyEntityRepository = new McpApiKeyEntityRepository(apiKeyService, authorityService, userService);
        this.mcpServerService = mcpServerService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthenticationToken apiKeyAuthenticationToken = (ApiKeyAuthenticationToken) authentication;

        if (!(apiKeyAuthenticationToken.getCredentials() instanceof McpApiKeyCredentials mcpApiKeyCredentials)) {
            throw new BadCredentialsException("Credentials do not exist");
        }

        McpServer mcpServer = getMcpServer(mcpApiKeyCredentials.getMcpServerSecretKey());

        if (!mcpServer.isEnabled()) {
            throw new BadCredentialsException("MCP server is disabled");
        }

        if (!mcpServer.isAuthenticationRequired()) {
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

        apiKeyService.updateLastUsedDate(mcpApiKeyEntity.getApiKeyId());

        return ApiKeyAuthenticationToken.authenticated(mcpApiKeyEntity, mcpApiKeyEntity.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private McpServer getMcpServer(String mcpServerSecretKey) {
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
