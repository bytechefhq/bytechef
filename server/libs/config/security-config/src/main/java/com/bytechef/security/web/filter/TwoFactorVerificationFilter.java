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

package com.bytechef.security.web.filter;

import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.authentication.TotpAttemptLimiter;
import com.bytechef.security.web.authentication.TwoFactorAuthentication;
import com.bytechef.security.web.authentication.TwoFactorAuthenticationSupport;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Completes a login that is waiting for its TOTP code. The code is checked against the user in the tenant the pending
 * session belongs to; failures are capped per login by {@link TotpAttemptLimiter}. On success the session id is
 * rotated, the primary authentication replaces the pending one, and a remember-me cookie requested at login is issued
 * now.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.security.two-factor-authentication", name = "enabled", havingValue = "true")
public class TwoFactorVerificationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final RememberMeServices rememberMeServices;
    private final TenantService tenantService;
    private final TotpAttemptLimiter totpAttemptLimiter = new TotpAttemptLimiter();
    private final UserService userService;

    @SuppressFBWarnings("EI")
    TwoFactorVerificationFilter(
        ObjectMapper objectMapper, RememberMeServices rememberMeServices, TenantService tenantService,
        UserService userService) {

        this.objectMapper = objectMapper;
        this.rememberMeServices = rememberMeServices;
        this.tenantService = tenantService;
        this.userService = userService;
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        if (!isMfaVerifyRequest(request)) {
            filterChain.doFilter(request, response);

            return;
        }

        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        if (!(authentication instanceof TwoFactorAuthentication twoFactorAuthentication)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());

            return;
        }

        String code = extractCode(request);

        if (code == null) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());

            return;
        }

        Authentication primaryAuthentication = twoFactorAuthentication.getPrimary();
        String login = primaryAuthentication.getName();

        if (totpAttemptLimiter.isLocked(login)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

            return;
        }

        String tenantId = resolveTenantId(request, login);

        if (tenantId == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());

            return;
        }

        boolean valid = callWithTenantId(tenantId, () -> userService.verifyTotpCode(login, code));

        if (!valid) {
            totpAttemptLimiter.recordFailure(login);

            if (totpAttemptLimiter.isLocked(login)) {
                invalidateSession(request);
            }

            response.setStatus(HttpStatus.UNAUTHORIZED.value());

            return;
        }

        totpAttemptLimiter.recordSuccess(login);

        request.changeSessionId();

        SecurityContext verifiedSecurityContext = SecurityContextHolder.createEmptyContext();

        verifiedSecurityContext.setAuthentication(primaryAuthentication);

        SecurityContextHolder.setContext(verifiedSecurityContext);

        new HttpSessionSecurityContextRepository().saveContext(verifiedSecurityContext, request, response);

        issueRequestedRememberMe(request, response, primaryAuthentication, tenantId);

        response.setStatus(HttpStatus.OK.value());
    }

    private String extractCode(HttpServletRequest request) throws IOException {
        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(request.getInputStream());
        } catch (JacksonException jacksonException) {
            return null;
        }

        JsonNode codeNode = jsonNode == null ? null : jsonNode.get("code");

        if (codeNode != null && codeNode.isString()) {
            return codeNode.asString();
        }

        return null;
    }

    private void issueRequestedRememberMe(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication, String tenantId) {

        HttpSession session = request.getSession(false);

        if (session == null ||
            session.getAttribute(TwoFactorAuthenticationSupport.REMEMBER_ME_REQUESTED_ATTRIBUTE) == null) {

            return;
        }

        session.removeAttribute(TwoFactorAuthenticationSupport.REMEMBER_ME_REQUESTED_ATTRIBUTE);

        HttpServletRequest rememberMeRequest = new HttpServletRequestWrapper(request) {

            @Override
            public String getParameter(String name) {
                return "remember-me".equals(name) ? "true" : super.getParameter(name);
            }
        };

        callWithTenantId(tenantId, () -> {
            rememberMeServices.loginSuccess(rememberMeRequest, response, authentication);

            return null;
        });
    }

    private String resolveTenantId(HttpServletRequest request, String login) {
        if (!tenantService.isMultiTenantEnabled()) {
            return TenantContext.getCurrentTenantId();
        }

        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute(TenantConstants.CURRENT_TENANT_ID) instanceof String tenantId) {
            return tenantId;
        }

        List<String> tenantIds = tenantService.getTenantIdsByUserLogin(login);

        return tenantIds.isEmpty() ? null : tenantIds.getFirst();
    }

    private static <T> T callWithTenantId(String tenantId, Supplier<T> supplier) {
        String previousTenantId = TenantContext.getCurrentTenantId();

        TenantContext.setCurrentTenantId(tenantId);

        try {
            return supplier.get();
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }
    }

    private static void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();
    }

    private boolean isMfaVerifyRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && "/api/mfa/verify".equals(request.getServletPath());
    }
}
