/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.config;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.TenantIdpFederatedIssuerAuthenticator;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.mcp.oauth2.McpAudienceValidator;
import com.bytechef.platform.security.web.mcp.oauth2.McpFederatedIssuerAuthenticator;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtIdentityMapper;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
public class McpTenantIssuerResolverConfiguration {

    @Bean
    McpTenantIssuerResolver mcpTenantIssuerResolver(IdentityProviderService identityProviderService) {
        return new McpTenantIssuerResolver(identityProviderService);
    }

    @Bean
    McpFederatedIssuerAuthenticator mcpFederatedIssuerAuthenticator(
        @Value("${bytechef.public-url:}") @Nullable String publicUrl) {

        return new TenantIdpFederatedIssuerAuthenticator(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), publicUrl);
    }
}
