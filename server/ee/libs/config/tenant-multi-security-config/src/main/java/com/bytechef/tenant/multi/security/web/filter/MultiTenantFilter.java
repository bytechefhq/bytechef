/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.tenant.multi.security.web.filter;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MultiTenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        HttpSession session = request.getSession();

        String currentTenantId = (String) session.getAttribute(TenantConstants.CURRENT_TENANT_ID);

        TenantContext.setCurrentTenantId(currentTenantId);

        filterChain.doFilter(request, response);
    }
}
