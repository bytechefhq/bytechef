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

package com.bytechef.automation.ai.a2a.server.security.web.authentication;

import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.platform.constant.PlatformType;
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
public class AutomationA2AServerApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyService apiKeyService;
    private final A2aServerService a2aServerService;
    private final McpApiKeyEntityRepository mcpApiKeyEntityRepository;

    @SuppressFBWarnings("EI")
    public AutomationA2AServerApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, AuthorityService authorityService, A2aServerService a2aServerService,
        UserService userService) {

        this.apiKeyService = apiKeyService;
        this.a2aServerService = a2aServerService;
        this.mcpApiKeyEntityRepository = new McpApiKeyEntityRepository(apiKeyService, authorityService, userService);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthenticationToken apiKeyAuthenticationToken = (ApiKeyAuthenticationToken) authentication;

        if (!(apiKeyAuthenticationToken.getCredentials() instanceof McpApiKeyCredentials mcpApiKeyCredentials)) {
            throw new BadCredentialsException("Authorization credentials do not exist");
        }

        A2aServer a2aServer = getA2aServer(mcpApiKeyCredentials.getMcpServerSecretKey());

        if (!a2aServer.isEnabled()) {
            throw new BadCredentialsException("A2A server is disabled");
        }

        if (!a2aServer.isAuthenticationRequired()) {
            return McpAnonymousAuthenticationToken.ofAutomationA2aServer(a2aServer.getId());
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

        if (a2aServer.getEnvironment() != mcpApiKeyEntity.getEnvironment()) {
            throw new BadCredentialsException("Invalid API key");
        }

        apiKeyService.updateLastUsedDate(mcpApiKeyEntity.getApiKeyId());

        return ApiKeyAuthenticationToken.authenticated(mcpApiKeyEntity, mcpApiKeyEntity.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private A2aServer getA2aServer(String a2aServerSecretKey) {
        return a2aServerService.fetchA2aServer(a2aServerSecretKey)
            .orElseThrow(() -> new BadCredentialsException("Invalid A2A server secret key"));
    }
}
