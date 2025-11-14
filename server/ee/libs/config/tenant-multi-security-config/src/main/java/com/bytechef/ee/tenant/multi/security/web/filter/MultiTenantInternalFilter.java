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

@FilterRegistration(name = "multi-tenant-internal-filter", urlPatterns = {
    "/api/*", "/graphql"
})
public class MultiTenantInternalFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpSession session = request.getSession();
        String requestURI = request.getRequestURI();
        String authorizationHeader = request.getHeader("Authorization");

        String sessionCurrentTenantId = (String) session.getAttribute(TenantConstants.CURRENT_TENANT_ID);
        boolean shouldRunAsTenantId = false;

        if (requestURI.startsWith("/api/") && requestURI.contains("/internal/") &&
            (authorizationHeader == null || !authorizationHeader.startsWith("Bearer "))) {

            shouldRunAsTenantId = true;
        } else if (requestURI.equals("/graphql")) {
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
