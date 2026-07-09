/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.security.web.config.McpTenantDiscoverySecurityConfiguration;
import com.bytechef.ee.platform.security.web.config.McpTenantIssuerResolverConfiguration;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantProtectedResourceMetadataController;
import com.bytechef.platform.security.web.config.McpOAuth2ResourceServerSecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpProtectedResourceMetadataSecurityConfiguration;
import com.bytechef.platform.user.domain.IdentityProvider;
import com.bytechef.platform.user.service.IdentityProviderService;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Boots the dedicated protected-resource-metadata chains with a single trusted issuer configured (via test properties),
 * so both the static RFC 9728 metadata endpoint and the tenant-aware per-endpoint metadata endpoint the discovery
 * challenge points at are served in isolation - no database required (the JDBC auto-configurations are excluded so no
 * {@code DataSource} is needed; {@code IdentityProviderService} is mocked to control the tenant's providers).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration(excludeName = {
    "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration",
    "org.springframework.boot.data.jdbc.autoconfigure.JdbcRepositoriesAutoConfiguration",
    "org.springframework.boot.sql.init.autoconfigure.SqlInitializationAutoConfiguration"
})
@EnableWebSecurity
@Import({
    McpOAuth2ResourceServerSecurityConfigurerContributor.class, McpProtectedResourceMetadataSecurityConfiguration.class,
    McpTenantIssuerResolverConfiguration.class, McpTenantDiscoverySecurityConfiguration.class,
    McpTenantProtectedResourceMetadataController.class
})
@Configuration
public class McpProtectedResourceMetadataSecurityIntTestConfiguration {

    public static final String TENANT_IDP_ISSUER_URI = "https://idp.customer.test";

    @Bean
    IdentityProviderService identityProviderService() {
        IdentityProvider identityProvider = mock(IdentityProvider.class);

        when(identityProvider.isEnabled()).thenReturn(true);
        when(identityProvider.isMcpAutomation()).thenReturn(true);
        when(identityProvider.isMcpManagement()).thenReturn(true);
        when(identityProvider.getIssuerUri()).thenReturn(TENANT_IDP_ISSUER_URI);

        IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        when(identityProviderService.getIdentityProviders()).thenReturn(List.of(identityProvider));

        return identityProviderService;
    }
}
