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
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * @author Ivica Cardic
 */
@Configuration
public class McpJwtSecurityConfigurerContributor implements SecurityConfigurerContributor {

    private final AuthorityService authorityService;
    private final McpResourceServerProperties mcpResourceServerProperties;
    private final ObjectProvider<McpServerSecretVerifier> mcpServerSecretVerifierProvider;
    private final @Nullable String publicUrl;
    private final ObjectProvider<SelfIssuedTokenRevocationChecker> selfIssuedTokenRevocationCheckerProvider;
    private final UserService userService;

    @SuppressFBWarnings("EI2")
    public McpJwtSecurityConfigurerContributor(
        McpResourceServerProperties mcpResourceServerProperties, UserService userService,
        AuthorityService authorityService, ObjectProvider<McpServerSecretVerifier> mcpServerSecretVerifierProvider,
        ObjectProvider<SelfIssuedTokenRevocationChecker> selfIssuedTokenRevocationCheckerProvider,
        @Value("${bytechef.public-url:}") @Nullable String publicUrl) {

        this.authorityService = authorityService;
        this.mcpResourceServerProperties = mcpResourceServerProperties;
        this.mcpServerSecretVerifierProvider = mcpServerSecretVerifierProvider;
        this.publicUrl = publicUrl;
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
                authorityService,
                mcpServerSecretVerifierProvider.orderedStream()
                    .toList(),
                selfIssuedTokenRevocationChecker, publicUrl));
    }

    private boolean hasSelfIssuer() {
        List<McpResourceServerProperties.Issuer> issuers = mcpResourceServerProperties.getIssuers();

        return issuers.stream()
            .anyMatch(McpResourceServerProperties.Issuer::isSelf);
    }
}
