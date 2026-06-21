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
class RemoteServiceAuthenticationFilterTest {

    @Test
    void testValidTokenProceeds() throws Exception {
        RemoteServiceAuthenticationFilter filter = new RemoteServiceAuthenticationFilter("secret-token");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TenantConstants.INTERNAL_SERVICE_TOKEN)).thenReturn("secret-token");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void testMissingTokenRejected() throws Exception {
        RemoteServiceAuthenticationFilter filter = new RemoteServiceAuthenticationFilter("secret-token");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TenantConstants.INTERNAL_SERVICE_TOKEN)).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void testWrongTokenRejected() throws Exception {
        RemoteServiceAuthenticationFilter filter = new RemoteServiceAuthenticationFilter("secret-token");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TenantConstants.INTERNAL_SERVICE_TOKEN)).thenReturn("wrong");

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void testBlankServerTokenRejected() throws Exception {
        RemoteServiceAuthenticationFilter filter = new RemoteServiceAuthenticationFilter("");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TenantConstants.INTERNAL_SERVICE_TOKEN)).thenReturn("anything");

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(401);
        verify(chain, never()).doFilter(any(), any());
    }
}
