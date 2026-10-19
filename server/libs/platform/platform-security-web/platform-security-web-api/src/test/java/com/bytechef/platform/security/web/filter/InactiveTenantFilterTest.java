/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.security.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantDeletionService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings("unchecked")
class InactiveTenantFilterTest {

    private final FilterChain filterChain = mock(FilterChain.class);
    private final ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider = mock(ObjectProvider.class);
    private final TenantDeletionService tenantDeletionService = mock(TenantDeletionService.class);

    @BeforeEach
    void beforeEach() {
        when(tenantDeletionServiceProvider.getIfAvailable()).thenReturn(tenantDeletionService);
    }

    @Test
    void testInvalidatesSessionOfInactiveTenant() throws Exception {
        when(tenantDeletionService.isInactive("000001")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();

        session.setAttribute(TenantConstants.CURRENT_TENANT_ID, "000001");

        request.setSession(session);

        MockHttpServletResponse response = new MockHttpServletResponse();

        new InactiveTenantFilter(tenantDeletionServiceProvider).doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(session.isInvalid()).isTrue();

        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testRejectsCredentialRequestWhoseTenantContextIsInactive() throws Exception {
        when(tenantDeletionService.isInactive("000001")).thenReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();

        TenantContext.runWithTenantId(
            "000001",
            () -> new InactiveTenantFilter(tenantDeletionServiceProvider)
                .doFilter(new MockHttpServletRequest(), response, filterChain));

        assertThat(response.getStatus()).isEqualTo(401);

        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testPassesActiveTenant() throws Exception {
        when(tenantDeletionService.isInactive("000002")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();

        session.setAttribute(TenantConstants.CURRENT_TENANT_ID, "000002");

        request.setSession(session);

        MockHttpServletResponse response = new MockHttpServletResponse();

        TenantContext.runWithTenantId(
            "000002",
            () -> new InactiveTenantFilter(tenantDeletionServiceProvider).doFilter(request, response, filterChain));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(session.isInvalid()).isFalse();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testPassesWhenServiceAbsent() throws Exception {
        ObjectProvider<TenantDeletionService> absentProvider = mock(ObjectProvider.class);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new InactiveTenantFilter(absentProvider).doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
