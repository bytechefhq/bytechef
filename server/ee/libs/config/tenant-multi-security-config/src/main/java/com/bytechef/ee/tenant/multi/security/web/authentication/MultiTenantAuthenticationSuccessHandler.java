/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.web.authentication;

import com.bytechef.platform.security.web.config.TwoFactorAuthenticationCustomizer;
import com.bytechef.security.web.authentication.TenantUserDetails;
import com.bytechef.security.web.authentication.TwoFactorAuthentication;
import com.bytechef.tenant.constant.TenantConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class MultiTenantAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RememberMeServices rememberMeServices;
    private final ObjectProvider<TwoFactorAuthenticationCustomizer> twoFactorAuthenticationCustomizerProvider;

    @SuppressFBWarnings("EI")
    public MultiTenantAuthenticationSuccessHandler(
        RememberMeServices rememberMeServices,
        ObjectProvider<TwoFactorAuthenticationCustomizer> twoFactorAuthenticationCustomizerProvider) {

        this.rememberMeServices = rememberMeServices;
        this.twoFactorAuthenticationCustomizerProvider = twoFactorAuthenticationCustomizerProvider;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

        if (!(authentication.getPrincipal() instanceof TenantUserDetails tenantUserDetails)) {
            rememberMeServices.loginFail(request, response);

            SecurityContextHolder.clearContext();

            HttpSession existingSession = request.getSession(false);

            if (existingSession != null) {
                existingSession.invalidate();
            }

            response.setStatus(HttpStatus.UNAUTHORIZED.value());

            return;
        }

        HttpSession session = request.getSession();

        session.setAttribute(TenantConstants.CURRENT_TENANT_ID, tenantUserDetails.getTenantId());

        TwoFactorAuthenticationCustomizer twoFactorAuthenticationCustomizer =
            twoFactorAuthenticationCustomizerProvider.getIfAvailable();

        if (twoFactorAuthenticationCustomizer != null &&
            twoFactorAuthenticationCustomizer.isTotpEnabled(authentication)) {

            SecurityContext securityContext = SecurityContextHolder.getContext();

            securityContext.setAuthentication(new TwoFactorAuthentication(authentication));

            new HttpSessionSecurityContextRepository().saveContext(securityContext, request, response);

            response.setStatus(HttpStatus.ACCEPTED.value());
        } else {
            response.setStatus(HttpStatus.OK.value());
        }
    }
}
