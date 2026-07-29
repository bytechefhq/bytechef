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

package com.bytechef.platform.billing.web.filter;

import com.bytechef.platform.billing.dto.TrialDTO;
import com.bytechef.platform.billing.service.TrialService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces trial limits on every authenticated request. Caching is handled at the service layer via
 * {@link TrialService#validateTrial()} ()}, which is tenant-scoped and invalidated on subscription changes.
 *
 * @author Matija Petanjek
 */
public class TrialFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TrialFilter.class);

    private final TrialService trialService;

    public TrialFilter(TrialService trialService) {
        this.trialService = trialService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || isAnonymous(authentication)) {
            filterChain.doFilter(request, response);

            return;
        }

        HttpSession session = request.getSession(true);

        String tenantId = (String) session.getAttribute(TenantConstants.CURRENT_TENANT_ID);

        if (tenantId == null) {
            filterChain.doFilter(request, response);

            return;
        }

        TenantContext.runWithTenantId(tenantId, () -> checkTrialLimit(request, response, filterChain, tenantId));
    }

    private void checkTrialLimit(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, String tenantId)
        throws Exception {

        TrialDTO trial = trialService.validateTrial();

        if (trial.expired()) {

            log.debug("Trial expired for tenant {}. Sending trial expired response.", tenantId);

            sendTrialExpiredResponse(response);

            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.startsWith("/api/account") ||
            path.startsWith("/api/platform/billing") ||
            path.startsWith("/api/platform/internal/billing") ||
            path.startsWith("/api/platform/internal/environments");
    }

    private boolean isAnonymous(Authentication authentication) {
        return authentication.getAuthorities()
            .stream()
            .anyMatch(authority -> "ROLE_ANONYMOUS".equals(authority.getAuthority()));
    }

    private void sendTrialExpiredResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.PAYMENT_REQUIRED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter()
            .write(
                "{\"title\":\"Trial Expired\",\"detail\":\"Your trial has ended. Please upgrade to continue.\",\"status\":402}");
    }
}
