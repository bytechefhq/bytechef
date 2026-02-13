/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.web.configurer;

import com.bytechef.ee.security.sso.web.filter.SsoEnforcementFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configures the {@link SsoEnforcementFilter} within the Spring Security filter chain, positioning it before the
 * {@link UsernamePasswordAuthenticationFilter} so that SSO-enforced domains are blocked before form login processing.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SsoEnforcementHttpConfigurer extends AbstractHttpConfigurer<SsoEnforcementHttpConfigurer, HttpSecurity> {

    private final SsoEnforcementFilter ssoEnforcementFilter;

    @SuppressFBWarnings("EI")
    public SsoEnforcementHttpConfigurer(SsoEnforcementFilter ssoEnforcementFilter) {
        this.ssoEnforcementFilter = ssoEnforcementFilter;
    }

    @Override
    public void configure(HttpSecurity http) {
        http.addFilterBefore(ssoEnforcementFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
