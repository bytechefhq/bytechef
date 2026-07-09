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

package com.bytechef.platform.security.web.config;

import com.bytechef.platform.security.web.mcp.oauth2.McpDiscoveryAuthenticationFilter;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantProtectedResourceMetadataAuthenticationEntryPoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * Contributes the MCP OAuth2 discovery filter. Active only in EE and only when at least one trusted issuer is
 * configured; otherwise it contributes a no-op so an unauthenticated MCP request keeps its Phase 1 bare 401 (no
 * {@code WWW-Authenticate} header). When active, an unauthenticated MCP request receives {@code 401} with a
 * {@code WWW-Authenticate: Bearer} header carrying the RFC 9728 protected-resource-metadata pointer.
 *
 * @author Ivica Cardic
 */
@Configuration
public class McpDiscoverySecurityConfigurerContributor implements SecurityConfigurerContributor {

    private static final String MCP_PATH_REGEX = "^/api/(automation|management)/.+/mcp";

    private final McpResourceServerProperties mcpResourceServerProperties;

    @SuppressFBWarnings("EI2")
    public McpDiscoverySecurityConfigurerContributor(McpResourceServerProperties mcpResourceServerProperties) {
        this.mcpResourceServerProperties = mcpResourceServerProperties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T getSecurityConfigurerAdapter() {
        if (mcpResourceServerProperties.getIssuers()
            .isEmpty()) {

            return (T) new DisabledMcpOAuth2ResourceServerConfigurer();
        }

        // The discovery challenge itself is federation-neutral (a generic RFC 9728 per-request pointer), so it is
        // constructed directly rather than injected through an SPI - it is base behavior that moves to CE with this
        // contributor. Only the tenant-aware metadata document it points at (served by the EE tenant metadata chain)
        // is enterprise-specific.
        AuthenticationEntryPoint authenticationEntryPoint =
            new McpTenantProtectedResourceMetadataAuthenticationEntryPoint();

        McpDiscoveryAuthenticationFilter mcpDiscoveryAuthenticationFilter = new McpDiscoveryAuthenticationFilter(
            RegexRequestMatcher.regexMatcher(MCP_PATH_REGEX), authenticationEntryPoint);

        return (T) new McpDiscoverySecurityConfigurer(mcpDiscoveryAuthenticationFilter);
    }
}
