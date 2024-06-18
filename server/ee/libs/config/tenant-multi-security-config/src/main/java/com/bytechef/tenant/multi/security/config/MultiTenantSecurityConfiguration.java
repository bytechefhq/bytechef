/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.tenant.multi.security.config;

import com.bytechef.tenant.multi.security.MultiTenantUserDetailsService;
import com.bytechef.tenant.multi.security.web.authentication.MultiTenantAuthenticationFailureHandler;
import com.bytechef.tenant.multi.security.web.authentication.MultiTenantAuthenticationSuccessHandler;
import com.bytechef.tenant.multi.security.web.filter.MultiTenantFilterAfterContributor;
import com.bytechef.tenant.service.TenantService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(value = "bytechef.tenant.mode", havingValue = "multi")
@ConditionalOnEEVersion
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
    MultiTenantUserDetailsService multiTenantUserDetailsService(TenantService tenantService) {
        return new MultiTenantUserDetailsService(tenantService);
    }
}
