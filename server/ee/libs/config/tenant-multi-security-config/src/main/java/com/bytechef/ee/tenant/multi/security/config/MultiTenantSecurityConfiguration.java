/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.config;

import com.bytechef.edition.annotation.ConditionalOnEEVersion;
import com.bytechef.ee.tenant.multi.security.MultiTenantUserDetailsService;
import com.bytechef.ee.tenant.multi.security.web.authentication.MultiTenantAuthenticationFailureHandler;
import com.bytechef.ee.tenant.multi.security.web.authentication.MultiTenantAuthenticationSuccessHandler;
import com.bytechef.ee.tenant.multi.security.web.filter.MultiTenantFilterAfterContributor;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import com.bytechef.tenant.service.TenantService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnMultiTenant
public class MultiTenantSecurityConfiguration {

    @Bean
    MultiTenantAuthenticationFailureHandler multiTenantAuthenticationFailureHandler() {
        return new MultiTenantAuthenticationFailureHandler();
    }

    @Bean
    MultiTenantAuthenticationSuccessHandler multiTenantAuthenticationSuccessHandler(TenantService tenantService) {
        return new MultiTenantAuthenticationSuccessHandler(tenantService);
    }

    @Bean
    MultiTenantFilterAfterContributor multiTenantFilterAfterContributor() {
        return new MultiTenantFilterAfterContributor();
    }

    @Bean("userDetailsService")
    MultiTenantUserDetailsService multiTenantUserDetailsService(
        AuthorityService authorityService, TenantService tenantService) {

        return new MultiTenantUserDetailsService(authorityService, tenantService);
    }
}
