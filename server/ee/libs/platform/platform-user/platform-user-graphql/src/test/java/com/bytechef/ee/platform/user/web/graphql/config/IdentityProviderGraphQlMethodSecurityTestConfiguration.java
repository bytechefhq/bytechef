/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.web.graphql.config;

import com.bytechef.ee.platform.user.facade.IdentityProviderFacade;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.graphql.execution.SecurityDataFetcherExceptionResolver;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackageClasses = IdentityProviderFacade.class, useDefaultFilters = false,
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = IdentityProviderFacade.class))
@Configuration
@EnableMethodSecurity
public class IdentityProviderGraphQlMethodSecurityTestConfiguration {

    @Bean
    SecurityDataFetcherExceptionResolver securityDataFetcherExceptionResolver() {
        return new SecurityDataFetcherExceptionResolver();
    }
}
