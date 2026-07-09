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

import com.bytechef.platform.security.web.mcp.oauth2.TenantAwareJwtAuthenticationFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

/**
 * Wires the base {@link TenantAwareJwtAuthenticationFilter} after the resource server's
 * {@link BearerTokenAuthenticationFilter}: once a Bearer JWT has been validated, this filter establishes the tenant
 * from the endpoint URL, enforces the per-endpoint scope, mandatory audience binding, and per-request user revocation,
 * and installs the ByteChef identity. This is the base (CE) resource-server policy - it stands alone without any
 * external-IdP federation; the federation trust filter is added separately by the enterprise contributor.
 *
 * @author Ivica Cardic
 */
public class McpJwtSecurityConfigurer extends AbstractHttpConfigurer<McpJwtSecurityConfigurer, HttpSecurity> {

    private final TenantAwareJwtAuthenticationFilter tenantAwareJwtAuthenticationFilter;

    @SuppressFBWarnings("EI2")
    public McpJwtSecurityConfigurer(TenantAwareJwtAuthenticationFilter tenantAwareJwtAuthenticationFilter) {
        this.tenantAwareJwtAuthenticationFilter = tenantAwareJwtAuthenticationFilter;
    }

    @Override
    public void configure(HttpSecurity http) {
        http.addFilterAfter(tenantAwareJwtAuthenticationFilter, BearerTokenAuthenticationFilter.class);
    }
}
