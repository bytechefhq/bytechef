/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.security.web.filter;

import com.bytechef.ee.automation.apiplatform.handler.security.web.authentication.ApiClientKeyAuthenticationToken;
import com.bytechef.platform.security.web.filter.AbstractPublicApiAuthenticationFilter;
import com.bytechef.platform.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiClientAuthenticationFilter extends AbstractPublicApiAuthenticationFilter {

    @SuppressFBWarnings("EI")
    public ApiClientAuthenticationFilter(AuthenticationManager authenticationManager) {
        super("^/api/o/.+", authenticationManager);
    }

    protected Authentication getAuthentication(HttpServletRequest request) {
        String token = getAuthToken(request);

        TenantKey tenantKey = TenantKey.parse(token);

        return new ApiClientKeyAuthenticationToken(token, tenantKey.getTenantId());
    }
}
