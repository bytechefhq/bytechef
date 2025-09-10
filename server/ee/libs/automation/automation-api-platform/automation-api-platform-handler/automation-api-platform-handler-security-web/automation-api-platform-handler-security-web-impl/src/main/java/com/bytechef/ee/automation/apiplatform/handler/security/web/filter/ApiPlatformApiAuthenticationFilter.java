/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.security.web.filter;

import com.bytechef.ee.automation.apiplatform.handler.security.web.authentication.ApiPlatformKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.web.filter.AbstractApiAuthenticationFilter;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiPlatformApiAuthenticationFilter extends AbstractApiAuthenticationFilter {

    @SuppressFBWarnings("EI")
    public ApiPlatformApiAuthenticationFilter(AuthenticationManager authenticationManager) {
        super("^/api/o/.+", authenticationManager);
    }

    protected Authentication getAuthentication(HttpServletRequest request) {
        Environment environment = getEnvironment(request);
        String token = getAuthToken(request);

        TenantKey tenantKey = TenantKey.parse(token);

        return new ApiPlatformKeyAuthenticationToken(environment.ordinal(), token, tenantKey.getTenantId());
    }
}
