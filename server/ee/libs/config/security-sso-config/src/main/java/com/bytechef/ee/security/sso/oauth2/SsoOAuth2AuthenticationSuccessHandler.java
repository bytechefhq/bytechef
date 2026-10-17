/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.oauth2;

import com.bytechef.ee.security.sso.SsoTenantUserResolver;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.authentication.TenantUser;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SsoOAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RememberMeServices rememberMeServices;
    private final SsoTenantUserResolver ssoTenantUserResolver;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public SsoOAuth2AuthenticationSuccessHandler(
        RememberMeServices rememberMeServices, TenantService tenantService, UserService userService) {

        this.rememberMeServices = rememberMeServices;
        this.ssoTenantUserResolver = new SsoTenantUserResolver(tenantService, userService);
        this.tenantService = tenantService;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException {

        OAuth2User oAuth2User = (OAuth2User) Objects.requireNonNull(authentication.getPrincipal(), "principal");

        Optional<TenantUser> optionalTenantUser = ssoTenantUserResolver.resolveTenantUser(
            getIdentifier(oAuth2User), authentication.getAuthorities());

        if (optionalTenantUser.isEmpty()) {
            rejectSignIn(request, response);

            return;
        }

        TenantUser tenantUser = optionalTenantUser.get();

        if (tenantService.isMultiTenantEnabled()) {
            HttpSession session = request.getSession();

            session.setAttribute(TenantConstants.CURRENT_TENANT_ID, tenantUser.getTenantId());
        }

        rememberMeServices.loginSuccess(
            request, response,
            UsernamePasswordAuthenticationToken.authenticated(tenantUser, null, authentication.getAuthorities()));

        response.sendRedirect("/oauth2/redirect");
    }

    private static String getIdentifier(OAuth2User oAuth2User) {
        if (oAuth2User instanceof OidcUser oidcUser && oidcUser.getEmail() != null) {
            return oidcUser.getEmail();
        }

        if (oAuth2User.getAttribute("email") instanceof String email) {
            return email;
        }

        return oAuth2User.getName();
    }

    private void rejectSignIn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        rememberMeServices.loginFail(request, response);

        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect("/login?error=oauth2");
    }
}
