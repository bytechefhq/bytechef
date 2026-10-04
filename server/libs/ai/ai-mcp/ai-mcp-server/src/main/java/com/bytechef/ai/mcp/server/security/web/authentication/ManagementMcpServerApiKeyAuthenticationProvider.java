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

package com.bytechef.ai.mcp.server.security.web.authentication;

import com.bytechef.ai.mcp.server.configuration.ManagementMcpServerAuthentication;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import com.bytechef.platform.security.web.mcp.McpApiKeyCredentials;
import com.bytechef.platform.security.web.mcp.McpApiKeyEntity;
import com.bytechef.platform.security.web.mcp.McpApiKeyEntityRepository;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @author Ivica Cardic
 */
public class ManagementMcpServerApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyService apiKeyService;
    private final McpApiKeyEntityRepository mcpApiKeyEntityRepository;
    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    public ManagementMcpServerApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, AuthorityService authorityService, PropertyService propertyService,
        UserService userService) {

        this.apiKeyService = apiKeyService;
        this.mcpApiKeyEntityRepository = new McpApiKeyEntityRepository(apiKeyService, authorityService, userService);
        this.propertyService = propertyService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthenticationToken apiKeyAuthenticationToken = (ApiKeyAuthenticationToken) authentication;

        if (!(apiKeyAuthenticationToken.getCredentials() instanceof McpApiKeyCredentials mcpApiKeyCredentials)) {
            throw new BadCredentialsException("Authorization credentials do not exist");
        }

        Property property = propertyService.fetchProperty("mcp.server", Property.Scope.PLATFORM, null)
            .orElseThrow(() -> new BadCredentialsException("MCP server secret key is not configured"));

        if (!isMcpServerSecretKeyEqual(property.get("secretKey"), mcpApiKeyCredentials.getMcpServerSecretKey())) {
            throw new BadCredentialsException("Invalid MCP server secret key");
        }

        if (!ManagementMcpServerAuthentication.isAuthenticationRequired(property)) {
            return McpAnonymousAuthenticationToken.ofManagementMcpServer();
        }

        String secretKey = mcpApiKeyCredentials.getSecret();

        if (secretKey == null) {
            throw new BadCredentialsException("Authorization token does not exist");
        }

        McpApiKeyEntity mcpApiKeyEntity = mcpApiKeyEntityRepository.findByKeyId(secretKey);

        if (mcpApiKeyEntity == null) {
            throw new BadCredentialsException("Invalid API key");
        }

        if (mcpApiKeyEntity.getType() != null) {
            throw new BadCredentialsException("Invalid API key");
        }

        if (mcpApiKeyEntity.getEnvironment() != mcpApiKeyCredentials.getEnvironment()) {
            throw new BadCredentialsException("Invalid API key");
        }

        apiKeyService.updateLastUsedDate(mcpApiKeyEntity.getApiKeyId());

        return ApiKeyAuthenticationToken.authenticated(mcpApiKeyEntity, mcpApiKeyEntity.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static boolean isMcpServerSecretKeyEqual(
        @Nullable Object configuredSecretKey, @Nullable String presentedSecretKey) {

        if (!(configuredSecretKey instanceof String configuredSecretKeyString) || presentedSecretKey == null) {
            return false;
        }

        return MessageDigest.isEqual(
            configuredSecretKeyString.getBytes(StandardCharsets.UTF_8),
            presentedSecretKey.getBytes(StandardCharsets.UTF_8));
    }
}
