/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.remote.web.filter;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class MultiTenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String currentTenantId = request.getHeader(TenantConstants.CURRENT_TENANT_ID);

        TenantContext.setCurrentTenantId(currentTenantId);

        filterChain.doFilter(request, response);
    }
}
