/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.web.filter;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.boot.web.servlet.FilterRegistration;

/**
 * @version ee
 */
@FilterRegistration(name = "multi-tenant-internal-filter", urlPatterns = {
    "/api/*", "/graphql"
})
public class MultiTenantInternalFilter implements Filter {

    private static final String APPROVAL_FORM_PATH = "/api/platform/internal/approval-form/";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String requestURI = request.getRequestURI();

        if (requestURI.startsWith(APPROVAL_FORM_PATH)) {
            filterChain.doFilter(servletRequest, servletResponse);

            return;
        }

        HttpSession session = request.getSession();
        String authorizationHeader = request.getHeader("Authorization");

        String sessionCurrentTenantId = (String) session.getAttribute(TenantConstants.CURRENT_TENANT_ID);
        boolean shouldRunAsTenantId = false;

        if (requestURI.startsWith("/api/") && requestURI.contains("/internal/") &&
            (authorizationHeader == null || !authorizationHeader.startsWith("Bearer "))) {

            shouldRunAsTenantId = true;
        } else if (requestURI.equals("/graphql") &&
            (authorizationHeader == null || !authorizationHeader.startsWith("Bearer "))) {

            shouldRunAsTenantId = true;
        } else if (requestURI.contains("/api/account")) {
            shouldRunAsTenantId = true;
        }

        if (shouldRunAsTenantId) {
            if (sessionCurrentTenantId == null) {
                if (requestURI.contains("/api/account")) {
                    sessionCurrentTenantId = TenantContext.DEFAULT_TENANT_ID;
                } else {
                    throw new IllegalStateException("Tenant ID is not set in the session");
                }
            }

            TenantContext.runWithTenantId(
                sessionCurrentTenantId, () -> filterChain.doFilter(servletRequest, servletResponse));
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }
}
