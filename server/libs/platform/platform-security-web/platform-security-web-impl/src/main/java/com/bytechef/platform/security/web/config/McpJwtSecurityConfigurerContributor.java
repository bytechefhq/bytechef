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

import com.bytechef.platform.security.web.mcp.McpServerSecretVerifier;
import com.bytechef.platform.security.web.mcp.SelfIssuedTokenRevocationChecker;
import com.bytechef.platform.security.web.mcp.oauth2.McpAudienceValidator;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtIdentityMapper;
import com.bytechef.platform.security.web.mcp.oauth2.TenantAwareJwtAuthenticationFilter;
import com.bytechef.platform.user.service.UserService;
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
public class McpJwtSecurityConfigurerContributor implements SecurityConfigurerContributor {

    private final McpResourceServerProperties mcpResourceServerProperties;
    private final ObjectProvider<McpServerSecretVerifier> mcpServerSecretVerifierProvider;
    private final ObjectProvider<SelfIssuedTokenRevocationChecker> selfIssuedTokenRevocationCheckerProvider;
    private final UserService userService;

    @SuppressFBWarnings("EI2")
    public McpJwtSecurityConfigurerContributor(
        McpResourceServerProperties mcpResourceServerProperties, UserService userService,
        ObjectProvider<McpServerSecretVerifier> mcpServerSecretVerifierProvider,
        ObjectProvider<SelfIssuedTokenRevocationChecker> selfIssuedTokenRevocationCheckerProvider) {

        this.mcpResourceServerProperties = mcpResourceServerProperties;
        this.mcpServerSecretVerifierProvider = mcpServerSecretVerifierProvider;
        this.selfIssuedTokenRevocationCheckerProvider = selfIssuedTokenRevocationCheckerProvider;
        this.userService = userService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T getSecurityConfigurerAdapter() {
        if (mcpResourceServerProperties.getIssuers()
            .isEmpty()) {

            return (T) new DisabledMcpOAuth2ResourceServerConfigurer();
        }

        SelfIssuedTokenRevocationChecker selfIssuedTokenRevocationChecker =
            selfIssuedTokenRevocationCheckerProvider.getIfAvailable();

        if (selfIssuedTokenRevocationChecker == null && hasSelfIssuer()) {
            throw new IllegalStateException(
                "bytechef.oauth2.resource-server declares a self issuer, but no SelfIssuedTokenRevocationChecker " +
                    "bean exists; enable the ByteChef authorization server or remove the self issuer");
        }

        return (T) new McpJwtSecurityConfigurer(
            new TenantAwareJwtAuthenticationFilter(
                new McpAudienceValidator(), new McpJwtIdentityMapper(), mcpResourceServerProperties, userService,
                mcpServerSecretVerifierProvider.orderedStream()
                    .toList(),
                selfIssuedTokenRevocationChecker));
    }

    private boolean hasSelfIssuer() {
        List<McpResourceServerProperties.Issuer> issuers = mcpResourceServerProperties.getIssuers();

        return issuers.stream()
            .anyMatch(McpResourceServerProperties.Issuer::isSelf);
    }
}
