/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.config;

import com.bytechef.ee.platform.user.scim.web.filter.ScimBearerTokenAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Spring Security configurer that adds the {@link ScimBearerTokenAuthenticationFilter} to the security filter chain.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ScimHttpConfigurer extends AbstractHttpConfigurer<ScimHttpConfigurer, HttpSecurity> {

    static final String SCIM_PATH_PATTERN = "/api/scim/v2/**";

    private final ScimBearerTokenAuthenticationFilter scimBearerTokenAuthenticationFilter;

    ScimHttpConfigurer(ScimBearerTokenAuthenticationFilter scimBearerTokenAuthenticationFilter) {
        this.scimBearerTokenAuthenticationFilter = scimBearerTokenAuthenticationFilter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(HttpSecurity http) {
        CsrfConfigurer<HttpSecurity> csrf = http.getConfigurer(CsrfConfigurer.class);

        if (csrf != null) {
            csrf.ignoringRequestMatchers(SCIM_PATH_PATTERN);
        }
    }

    @Override
    public void configure(HttpSecurity http) {
        http.addFilterBefore(scimBearerTokenAuthenticationFilter, BasicAuthenticationFilter.class);
    }

}
