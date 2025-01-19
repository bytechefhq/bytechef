/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.web.filter;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.util.TenantUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MultiTenantFilter extends OncePerRequestFilter {

    private static final RequestMatcher REQUEST_MATCHER = new NegatedRequestMatcher(
        new OrRequestMatcher(
            new AntPathRequestMatcher("/api/account/**"),
            new AntPathRequestMatcher("/api/**/internal/**")));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        HttpSession session = request.getSession();

        String sessionCurrentTenantId = (String) session.getAttribute(TenantConstants.CURRENT_TENANT_ID);

        if (sessionCurrentTenantId == null) {
            sessionCurrentTenantId = TenantContext.DEFAULT_TENANT_ID;
        }

        TenantUtils.runWithTenantId(sessionCurrentTenantId, () -> filterChain.doFilter(request, response));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return REQUEST_MATCHER.matches(request);
    }
}
