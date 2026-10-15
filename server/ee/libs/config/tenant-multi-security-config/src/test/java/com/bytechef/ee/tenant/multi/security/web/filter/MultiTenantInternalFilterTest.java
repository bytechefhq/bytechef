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
 * @author Ivica Cardic
 */
class MultiTenantInternalFilterTest {

    private final MultiTenantInternalFilter multiTenantInternalFilter = new MultiTenantInternalFilter();

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
