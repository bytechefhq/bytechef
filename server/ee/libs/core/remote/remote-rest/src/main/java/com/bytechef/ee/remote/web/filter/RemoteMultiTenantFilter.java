/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.web.filter;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteMultiTenantFilter extends OncePerRequestFilter {

    private static final PathPatternRequestMatcher.Builder BUILDER = PathPatternRequestMatcher.withDefaults();

    private static final RequestMatcher REQUEST_MATCHER = new NegatedRequestMatcher(BUILDER.matcher("/remote/**"));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String currentTenantId = request.getHeader(TenantConstants.CURRENT_TENANT_ID);

        TenantContext.runWithTenantId(currentTenantId, () -> filterChain.doFilter(request, response));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return REQUEST_MATCHER.matches(request);
    }
}
