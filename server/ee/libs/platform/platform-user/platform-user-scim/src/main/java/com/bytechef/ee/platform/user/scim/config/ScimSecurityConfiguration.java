/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.config;

import com.bytechef.ee.platform.user.scim.web.filter.ScimBearerTokenAuthenticationFilter;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.tenant.service.TenantService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * Secures the SCIM API with the identity provider's bearer token, in both tenant modes. SCIM tokens belong to identity
 * provider records, so the API is available exactly when SSO is.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.security.sso", name = "enabled", havingValue = "true")
class ScimSecurityConfiguration {

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
