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
import java.util.Objects;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationProvider;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Authenticates management MCP requests: delegates secret validation to mcp-server-security, then enforces that the key
 * is an admin key (no platform type), that the URL path secret matches the configured MCP server secret, and that the
 * key's environment matches the requested environment.
 *
 * <p>
 * An API key is required unless the tenant's {@code mcp.server} property carries an explicit
 * {@code authenticationRequired = false}; a property missing the entry requires one, so a legacy row written before the
 * entry existed no longer yields an authority-less {@code McpAnonymousAuthenticationToken}. The same rule is expressed
 * by {@code ManagementMcpAuthenticationRequiredResolver}, and the two must stay in agreement.
 *
 * @author Ivica Cardic
 */
public class ManagementMcpServerApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyAuthenticationProvider<McpApiKeyEntity> apiKeyAuthenticationProvider;
    private final ApiKeyService apiKeyService;
    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    public ManagementMcpServerApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, AuthorityService authorityService, PropertyService propertyService,
        UserService userService) {

        this.apiKeyAuthenticationProvider = new ApiKeyAuthenticationProvider<>(
            new McpApiKeyEntityRepository(apiKeyService, authorityService, userService));
        this.apiKeyService = apiKeyService;
        this.propertyService = propertyService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthenticationToken apiKeyAuthenticationToken = (ApiKeyAuthenticationToken) authentication;

        McpApiKeyCredentials mcpApiKeyCredentials = (McpApiKeyCredentials) apiKeyAuthenticationToken.getCredentials();

        if (mcpApiKeyCredentials == null) {
            throw new BadCredentialsException("Authorization credentials do not exist");
        }

        Property property = propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null);

        if (!Objects.equals(property.get("secretKey"), mcpApiKeyCredentials.getMcpServerSecretKey())) {
            throw new BadCredentialsException("Invalid MCP server secret key");
        }

        if (Boolean.FALSE.equals(property.get("authenticationRequired"))) {
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

        if (mcpApiKeyEntity.getType() != null) {
            throw new BadCredentialsException("Invalid API key");
        }

        if (mcpApiKeyEntity.getEnvironment() != mcpApiKeyCredentials.getEnvironment()) {
            throw new BadCredentialsException("Invalid API key");
        }

        apiKeyService.updateLastUsedDate(mcpApiKeyEntity.getApiKeyId());

        return authenticatedAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
