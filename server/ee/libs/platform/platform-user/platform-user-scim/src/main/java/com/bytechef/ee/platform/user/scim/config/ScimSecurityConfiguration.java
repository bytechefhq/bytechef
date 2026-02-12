/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.config;

import com.bytechef.ee.platform.user.scim.web.filter.ScimBearerTokenAuthenticationFilter;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.platform.user.service.IdentityProviderService;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * Security configuration for the SCIM 2.0 module. Registers the Bearer token authentication filter and contributes SCIM
 * paths to the permit-all list.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnMultiTenant
@ConditionalOnProperty(prefix = "bytechef.security.sso", name = "enabled", havingValue = "true")
class ScimSecurityConfiguration {

    @Bean
    AuthorizeHttpRequestContributor scimAuthorizeHttpRequestContributor() {
        return new AuthorizeHttpRequestContributor() {

            @Override
            public List<String> getApiPermitAllRequestMatcherPaths() {
                return List.of("/api/scim/v2/**");
            }
        };
    }

    @Bean
    SecurityConfigurerContributor scimSecurityConfigurerContributor(
        IdentityProviderService identityProviderService, TenantService tenantService) {

        ScimBearerTokenAuthenticationFilter scimBearerTokenAuthenticationFilter =
            new ScimBearerTokenAuthenticationFilter(identityProviderService, tenantService);

        return new SecurityConfigurerContributor() {

            @Override
            @SuppressWarnings("unchecked")
            public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>>
                T getSecurityConfigurerAdapter() {

                return (T) new ScimHttpConfigurer(scimBearerTokenAuthenticationFilter);
            }
        };
    }
}
