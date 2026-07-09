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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * Adds the {@link McpDiscoveryAuthenticationFilter} early in the chain (ahead of the credential filters) so an
 * unauthenticated MCP request receives the OAuth2 discovery response instead of the API-key filter's bare 401.
 *
 * @author Ivica Cardic
 */
public class McpDiscoverySecurityConfigurer
    extends AbstractHttpConfigurer<McpDiscoverySecurityConfigurer, HttpSecurity> {

    private final McpDiscoveryAuthenticationFilter mcpDiscoveryAuthenticationFilter;

    @SuppressFBWarnings("EI2")
    public McpDiscoverySecurityConfigurer(McpDiscoveryAuthenticationFilter mcpDiscoveryAuthenticationFilter) {
        this.mcpDiscoveryAuthenticationFilter = mcpDiscoveryAuthenticationFilter;
    }

    @Override
    public void configure(HttpSecurity http) {
        http.addFilterBefore(mcpDiscoveryAuthenticationFilter, SecurityContextHolderFilter.class);
    }
}
