/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.config;

import com.bytechef.ee.platform.user.scim.ScimConstants;
import com.bytechef.ee.platform.user.scim.web.filter.ScimBearerTokenAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Adds the SCIM bearer token filter and exempts the SCIM API from CSRF protection: identity providers call it with a
 * bearer token and no browser session, so there is no cookie for a cross-site request to ride on.
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
    public void init(HttpSecurity http) {
        CsrfConfigurer<?> csrf = http.getConfigurer(CsrfConfigurer.class);

        if (csrf != null) {
            csrf.ignoringRequestMatchers(
                PathPatternRequestMatcher.withDefaults()
                    .matcher(ScimConstants.PATH_PATTERN));
        }
    }

    @Override
    public void configure(HttpSecurity http) {
        http.addFilterBefore(scimBearerTokenAuthenticationFilter, BasicAuthenticationFilter.class);
    }
}
