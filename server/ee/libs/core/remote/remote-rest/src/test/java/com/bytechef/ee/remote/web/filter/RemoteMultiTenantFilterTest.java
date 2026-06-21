/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.web.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.constant.TenantConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RemoteMultiTenantFilterTest {

    private final RemoteMultiTenantFilter filter = new RemoteMultiTenantFilter();

    @Test
    void testMissingHeaderReturnsBadRequest() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TenantConstants.CURRENT_TENANT_ID)).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(400);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void testMalformedHeaderReturnsBadRequest() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TenantConstants.CURRENT_TENANT_ID)).thenReturn("a;b");

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(400);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void testValidHeaderProceeds() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TenantConstants.CURRENT_TENANT_ID)).thenReturn("000001");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
