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

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantDeletionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects every request that would enter a tenant pending deletion, whether the tenant comes from the session or from a
 * credential-resolved {@link TenantContext}. A session bound to such a tenant is invalidated.
 *
 * @author Ivica Cardic
 */
public class InactiveTenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InactiveTenantFilter.class);

    private final ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider;

    public InactiveTenantFilter(ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider) {
        this.tenantDeletionServiceProvider = tenantDeletionServiceProvider;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        TenantDeletionService tenantDeletionService = tenantDeletionServiceProvider.getIfAvailable();

        if (tenantDeletionService == null) {
            filterChain.doFilter(request, response);

            return;
        }

        HttpSession session = request.getSession(false);

        if (session != null) {
            Object sessionTenantId = session.getAttribute(TenantConstants.CURRENT_TENANT_ID);

            if (sessionTenantId instanceof String tenantId && tenantDeletionService.isInactive(tenantId)) {
                log.debug("Rejecting session bound to inactive tenant {}", tenantId);

                session.invalidate();

                response.sendError(HttpStatus.UNAUTHORIZED.value());

                return;
            }
        }

        String currentTenantId = TenantContext.getCurrentTenantId();

        if (!TenantContext.DEFAULT_TENANT_ID.equals(currentTenantId)
            && tenantDeletionService.isInactive(currentTenantId)) {

            log.debug("Rejecting request for inactive tenant {}", currentTenantId);

            response.sendError(HttpStatus.UNAUTHORIZED.value());

            return;
        }

        filterChain.doFilter(request, response);
    }
}
