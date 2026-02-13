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
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Spring Security configurer that adds the {@link ScimBearerTokenAuthenticationFilter} to the security filter chain.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ScimHttpConfigurer extends AbstractHttpConfigurer<ScimHttpConfigurer, HttpSecurity> {

    private final ScimBearerTokenAuthenticationFilter scimBearerTokenAuthenticationFilter;

    ScimHttpConfigurer(ScimBearerTokenAuthenticationFilter scimBearerTokenAuthenticationFilter) {
        this.scimBearerTokenAuthenticationFilter = scimBearerTokenAuthenticationFilter;
    }

    @Override
    public void configure(HttpSecurity http) {
        http.addFilterBefore(scimBearerTokenAuthenticationFilter, BasicAuthenticationFilter.class);
    }

}
