/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.saml2;

import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.platform.user.domain.IdentityProvider;
import com.bytechef.platform.user.service.IdentityProviderService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SsoSaml2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final IdentityProviderService identityProviderService;
    private final RememberMeServices rememberMeServices;
    private final TenantService tenantService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public SsoSaml2AuthenticationSuccessHandler(
        IdentityProviderService identityProviderService, RememberMeServices rememberMeServices,
        TenantService tenantService, UserService userService) {

        this.identityProviderService = identityProviderService;
        this.rememberMeServices = rememberMeServices;
        this.tenantService = tenantService;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException {

        Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();

        String email = extractEmail(principal);
        String firstName = extractFirstAttribute(principal,
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname");
        String lastName = extractFirstAttribute(principal,
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname");

        boolean autoProvision = true;
        String defaultAuthority = AuthorityConstants.ADMIN;

        String registrationId = principal.getRelyingPartyRegistrationId();

        if (registrationId != null
            && registrationId.startsWith(DynamicRelyingPartyRegistrationRepository.SAML_PREFIX)) {
            long identityProviderId = Long.parseLong(
                registrationId.substring(DynamicRelyingPartyRegistrationRepository.SAML_PREFIX.length()));

            IdentityProvider identityProvider = identityProviderService.getIdentityProvider(identityProviderId);

            autoProvision = identityProvider.isAutoProvision();
            defaultAuthority = identityProvider.getDefaultAuthority();
        }

        userService.findOrCreateSocialUser(
            email, firstName, lastName, null, UserConstants.AUTH_PROVIDER_SAML, principal.getName(), autoProvision,
            defaultAuthority);

        List<String> tenantIds = tenantService.getTenantIdsByUserEmail(email);

        HttpSession session = request.getSession();

        session.setAttribute(TenantConstants.CURRENT_TENANT_ID, tenantIds.getFirst());

        rememberMeServices.loginSuccess(request, response, authentication);

        response.sendRedirect("/oauth2/redirect");
    }

    private String extractEmail(Saml2AuthenticatedPrincipal principal) {
        List<Object> emailAttributes = principal.getAttribute(
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress");

        if (emailAttributes != null && !emailAttributes.isEmpty()) {
            return emailAttributes.getFirst()
                .toString();
        }

        return principal.getName();
    }

    private String extractFirstAttribute(Saml2AuthenticatedPrincipal principal, String attributeName) {
        List<Object> attributes = principal.getAttribute(attributeName);

        if (attributes != null && !attributes.isEmpty()) {
            return attributes.getFirst()
                .toString();
        }

        return null;
    }
}
