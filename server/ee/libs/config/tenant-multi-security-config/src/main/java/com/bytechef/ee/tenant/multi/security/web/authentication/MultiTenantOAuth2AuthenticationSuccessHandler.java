/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.web.authentication;

import com.bytechef.security.web.oauth2.CustomOAuth2User;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MultiTenantOAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RememberMeServices rememberMeServices;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public MultiTenantOAuth2AuthenticationSuccessHandler(
        RememberMeServices rememberMeServices, TenantService tenantService) {

        this.rememberMeServices = rememberMeServices;
        this.tenantService = tenantService;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        List<String> tenantIds = tenantService.getTenantIdsByUserEmail(oAuth2User.getName());

        HttpSession session = request.getSession();

        session.setAttribute(TenantConstants.CURRENT_TENANT_ID, tenantIds.getFirst());

        rememberMeServices.loginSuccess(request, response, authentication);

        response.sendRedirect("/oauth2/redirect");
    }
}
