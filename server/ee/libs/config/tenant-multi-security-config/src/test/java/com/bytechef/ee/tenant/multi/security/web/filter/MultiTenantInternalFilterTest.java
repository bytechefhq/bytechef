/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class MultiTenantInternalFilterTest {

    private final MultiTenantInternalFilter multiTenantInternalFilter = new MultiTenantInternalFilter();

    @Test
    void testAnonymousTriggerFormRequestPassesWithoutASessionTenant() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
            "GET", "/api/automation/internal/trigger-form/MDAwMDAzOjA6MTA1MDp1dWlkOnRyaWdnZXJfMQ==");

        AtomicReference<Boolean> passed = new AtomicReference<>(false);

        multiTenantInternalFilter.doFilter(
            request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> passed.set(true));

        assertThat(passed.get()).isTrue();
        assertThat(request.getSession(false)).isNull();
    }

    @Test
    void testOtherAnonymousAutomationInternalRequestIsStillRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/automation/internal/projects");

        assertThatThrownBy(() -> multiTenantInternalFilter.doFilter(
            request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tenant ID is not set in the session");
    }

    @Test
    void testGraphQlRequestWithABearerTokenPassesWithoutASessionTenant() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/graphql");

        request.addHeader("Authorization", "Bearer token");

        AtomicReference<Boolean> passed = new AtomicReference<>(false);

        multiTenantInternalFilter.doFilter(
            request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> passed.set(true));

        assertThat(passed.get()).isTrue();
    }

    @Test
    void testGraphQlRequestWithoutABearerTokenRequiresASessionTenant() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/graphql");

        assertThatThrownBy(() -> multiTenantInternalFilter.doFilter(
            request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tenant ID is not set in the session");
    }

    @Test
    void testGraphQlRequestWithoutABearerTokenRunsAsTheSessionTenant() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/graphql");

        request.getSession()
            .setAttribute(TenantConstants.CURRENT_TENANT_ID, "000042");

        AtomicReference<String> tenantId = new AtomicReference<>();

        multiTenantInternalFilter.doFilter(
            request, new MockHttpServletResponse(),
            (servletRequest, servletResponse) -> tenantId.set(TenantContext.getCurrentTenantId()));

        assertThat(tenantId.get()).isEqualTo("000042");
    }
}
