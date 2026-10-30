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
import com.bytechef.platform.security.web.mcp.McpEndpoint;
import com.bytechef.platform.security.web.mcp.oauth2.McpAuthenticationRequiredPredicate;
import com.bytechef.platform.security.web.mcp.oauth2.McpDiscoveryAuthenticationFilter;
import com.bytechef.platform.security.web.mcp.oauth2.McpEndpointRequestMatcher;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantProtectedResourceMetadataAuthenticationEntryPoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * @author Ivica Cardic
 */
@Configuration
public class McpDiscoverySecurityConfigurerContributor implements SecurityConfigurerContributor {

    private final ObjectProvider<McpAuthenticationRequiredResolver> mcpAuthenticationRequiredResolverProvider;
    private final ObjectProvider<McpEndpoint> mcpEndpointProvider;
    private final McpResourceServerProperties mcpResourceServerProperties;

    @SuppressFBWarnings("EI2")
    public McpDiscoverySecurityConfigurerContributor(
        McpResourceServerProperties mcpResourceServerProperties,
        ObjectProvider<McpAuthenticationRequiredResolver> mcpAuthenticationRequiredResolverProvider,
        ObjectProvider<McpEndpoint> mcpEndpointProvider) {

        this.mcpAuthenticationRequiredResolverProvider = mcpAuthenticationRequiredResolverProvider;
        this.mcpEndpointProvider = mcpEndpointProvider;
        this.mcpResourceServerProperties = mcpResourceServerProperties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T getSecurityConfigurerAdapter() {
        List<McpEndpoint> mcpEndpoints = mcpEndpointProvider.orderedStream()
            .toList();

        if (mcpEndpoints.isEmpty() || mcpResourceServerProperties.getIssuers()
            .isEmpty()) {

            return (T) new DisabledMcpOAuth2ResourceServerConfigurer();
        }

        McpAuthenticationEntryPoint mcpAuthenticationEntryPoint =
            new McpTenantProtectedResourceMetadataAuthenticationEntryPoint();

        McpAuthenticationRequiredPredicate mcpAuthenticationRequiredPredicate = new McpAuthenticationRequiredPredicate(
            mcpAuthenticationRequiredResolverProvider.orderedStream()
                .toList());

        McpDiscoveryAuthenticationFilter mcpDiscoveryAuthenticationFilter = new McpDiscoveryAuthenticationFilter(
            new McpEndpointRequestMatcher(mcpEndpoints), mcpAuthenticationEntryPoint,
            mcpAuthenticationRequiredPredicate);

        return (T) new McpDiscoverySecurityConfigurer(mcpDiscoveryAuthenticationFilter, mcpAuthenticationEntryPoint);
    }
}
