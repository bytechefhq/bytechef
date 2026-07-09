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

import com.bytechef.platform.security.web.mcp.oauth2.McpAudienceValidator;
import com.bytechef.platform.security.web.mcp.oauth2.McpFederatedIssuerAuthenticator;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtIdentityMapper;
import com.bytechef.platform.security.web.mcp.oauth2.TenantAwareJwtAuthenticationFilter;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * Contributes the base {@link TenantAwareJwtAuthenticationFilter} onto the MCP endpoints' chain (after the resource
 * server's {@code BearerTokenAuthenticationFilter}). Active whenever at least one trusted issuer is configured;
 * otherwise it contributes a no-op. Kept separate from {@link McpOAuth2ResourceServerSecurityConfigurerContributor}
 * because the filter is added relative to the resource server's own {@code BearerTokenAuthenticationFilter}, which that
 * contributor installs.
 *
 * <p>
 * The per-tenant external-IdP {@link McpFederatedIssuerAuthenticator} is optional (resolved via
 * {@link ObjectProvider}): present on an enterprise build, so a non-statically-configured issuer is authenticated
 * through federation; absent on a CE-only build, so such an issuer is rejected. Either way the base static-issuer
 * policy (scope, audience, revocation, identity) is enforced.
 *
 * @author Ivica Cardic
 */
@Configuration
public class McpJwtSecurityConfigurerContributor implements SecurityConfigurerContributor {

    private final ObjectProvider<McpFederatedIssuerAuthenticator> mcpFederatedIssuerAuthenticatorProvider;
    private final McpResourceServerProperties mcpResourceServerProperties;
    private final UserService userService;

    @SuppressFBWarnings("EI2")
    public McpJwtSecurityConfigurerContributor(
        McpResourceServerProperties mcpResourceServerProperties, UserService userService,
        ObjectProvider<McpFederatedIssuerAuthenticator> mcpFederatedIssuerAuthenticatorProvider) {

        this.mcpFederatedIssuerAuthenticatorProvider = mcpFederatedIssuerAuthenticatorProvider;
        this.mcpResourceServerProperties = mcpResourceServerProperties;
        this.userService = userService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T getSecurityConfigurerAdapter() {
        if (mcpResourceServerProperties.getIssuers()
            .isEmpty()) {

            return (T) new DisabledMcpOAuth2ResourceServerConfigurer();
        }

        return (T) new McpJwtSecurityConfigurer(
            new TenantAwareJwtAuthenticationFilter(
                new McpAudienceValidator(), new McpJwtIdentityMapper(), mcpResourceServerProperties, userService,
                mcpFederatedIssuerAuthenticatorProvider.getIfAvailable()));
    }
}
