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

import com.bytechef.platform.security.web.mcp.McpAuthenticationEntryPoint;
import com.bytechef.platform.security.web.mcp.McpAuthenticationRequiredResolver;
import com.bytechef.platform.security.web.mcp.oauth2.McpAuthenticationRequiredPredicate;
import com.bytechef.platform.security.web.mcp.oauth2.McpDiscoveryAuthenticationFilter;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantProtectedResourceMetadataAuthenticationEntryPoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * @author Ivica Cardic
 */
@Configuration
public class McpDiscoverySecurityConfigurerContributor implements SecurityConfigurerContributor {

    private static final String MCP_PATH_REGEX = "^/api/(automation|management)/.+/mcp";

    private final ObjectProvider<McpAuthenticationRequiredResolver> mcpAuthenticationRequiredResolverProvider;
    private final McpResourceServerProperties mcpResourceServerProperties;

    @SuppressFBWarnings("EI2")
    public McpDiscoverySecurityConfigurerContributor(
        McpResourceServerProperties mcpResourceServerProperties,
        ObjectProvider<McpAuthenticationRequiredResolver> mcpAuthenticationRequiredResolverProvider) {

        this.mcpAuthenticationRequiredResolverProvider = mcpAuthenticationRequiredResolverProvider;
        this.mcpResourceServerProperties = mcpResourceServerProperties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T getSecurityConfigurerAdapter() {
        if (mcpResourceServerProperties.getIssuers()
            .isEmpty()) {

            return (T) new DisabledMcpOAuth2ResourceServerConfigurer();
        }

        McpAuthenticationEntryPoint mcpAuthenticationEntryPoint =
            new McpTenantProtectedResourceMetadataAuthenticationEntryPoint();

        McpAuthenticationRequiredPredicate mcpAuthenticationRequiredPredicate = new McpAuthenticationRequiredPredicate(
            mcpAuthenticationRequiredResolverProvider.orderedStream()
                .toList());

        McpDiscoveryAuthenticationFilter mcpDiscoveryAuthenticationFilter = new McpDiscoveryAuthenticationFilter(
            RegexRequestMatcher.regexMatcher(MCP_PATH_REGEX), mcpAuthenticationEntryPoint,
            mcpAuthenticationRequiredPredicate);

        return (T) new McpDiscoverySecurityConfigurer(mcpDiscoveryAuthenticationFilter, mcpAuthenticationEntryPoint);
    }
}
