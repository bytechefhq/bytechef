/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.web.authentication;

import com.bytechef.platform.security.web.config.TwoFactorAuthenticationCustomizer;
import com.bytechef.security.web.authentication.TwoFactorAuthentication;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class MultiTenantAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final TenantService tenantService;
    private final ObjectProvider<TwoFactorAuthenticationCustomizer> twoFactorAuthenticationCustomizerProvider;

    @SuppressFBWarnings("EI")
    public MultiTenantAuthenticationSuccessHandler(
        TenantService tenantService,
        ObjectProvider<TwoFactorAuthenticationCustomizer> twoFactorAuthenticationCustomizerProvider) {

        this.tenantService = tenantService;
        this.twoFactorAuthenticationCustomizerProvider = twoFactorAuthenticationCustomizerProvider;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        List<String> tenantIds = tenantService.getTenantIdsByUserEmail(userDetails.getUsername());

        HttpSession session = request.getSession();

        session.setAttribute(TenantConstants.CURRENT_TENANT_ID, tenantIds.getFirst());

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
