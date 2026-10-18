/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.saml2;

import com.bytechef.ee.security.sso.IdentityProviderLocator;
import com.bytechef.ee.security.sso.IdentityProviderLocator.LocatedIdentityProvider;
import com.bytechef.ee.security.sso.SsoUserProvisioner;
import com.bytechef.platform.security.web.config.TwoFactorAuthenticationCustomizer;
import com.bytechef.security.web.authentication.TwoFactorAuthenticationSupport;
import com.bytechef.security.web.oauth2.SocialUserProvisioner.ProvisionedUser;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * SAML SSO success handler. Resolves the ByteChef user in the tenant that owns the identity provider, then replaces the
 * SAML authentication, whose name is the IdP's NameID, with one whose principal is the ByteChef login and whose
 * authorities are the user's own, so the rest of the application sees the same principal as after any other login.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SsoSaml2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    static final String EMAIL_ATTRIBUTE = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";

    private static final List<String> EMAIL_ATTRIBUTES = List.of(
        EMAIL_ATTRIBUTE, "email", "mail", "urn:oid:0.9.2342.19200300.100.1.3");
    private static final List<String> FIRST_NAME_ATTRIBUTES = List.of(
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname", "givenName", "firstName",
        "urn:oid:2.5.4.42");
    private static final List<String> LAST_NAME_ATTRIBUTES = List.of(
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname", "sn", "surname", "lastName",
        "urn:oid:2.5.4.4");

    private final IdentityProviderLocator identityProviderLocator;
    private final RememberMeServices rememberMeServices;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SsoUserProvisioner ssoUserProvisioner;
    private final TenantService tenantService;
    private final TwoFactorAuthenticationCustomizer twoFactorAuthenticationCustomizer;

    @SuppressFBWarnings("EI")
    public SsoSaml2AuthenticationSuccessHandler(
        IdentityProviderLocator identityProviderLocator, RememberMeServices rememberMeServices,
        SsoUserProvisioner ssoUserProvisioner, TenantService tenantService,
        TwoFactorAuthenticationCustomizer twoFactorAuthenticationCustomizer) {

        this.identityProviderLocator = identityProviderLocator;
        this.rememberMeServices = rememberMeServices;
        this.ssoUserProvisioner = ssoUserProvisioner;
        this.tenantService = tenantService;
        this.twoFactorAuthenticationCustomizer = twoFactorAuthenticationCustomizer;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException {

        SamlAssertion samlAssertion = SamlAssertion.of(authentication);

        if (samlAssertion == null) {
            reject(request, response, "assertion_not_available");

            return;
        }

        Optional<LocatedIdentityProvider> locatedIdentityProvider = identityProviderLocator.findByRegistrationId(
            samlAssertion.registrationId());

        if (locatedIdentityProvider.isEmpty()) {
            reject(request, response, "identity_provider_not_found");

            return;
        }

        String email = samlAssertion.firstAttribute(EMAIL_ATTRIBUTES);

        if (email == null) {
            email = samlAssertion.nameId();
        }

        if (email == null || !email.contains("@")) {
            reject(request, response, "email_not_available");

            return;
        }

        SecurityContextHolder.clearContext();

        ProvisionedUser provisionedUser;

        try {
            provisionedUser = ssoUserProvisioner.provision(
                locatedIdentityProvider.get(), email, samlAssertion.firstAttribute(FIRST_NAME_ATTRIBUTES),
                samlAssertion.firstAttribute(LAST_NAME_ATTRIBUTES), null, samlAssertion.nameId());
        } catch (OAuth2AuthenticationException oAuth2AuthenticationException) {
            reject(
                request, response, oAuth2AuthenticationException.getError()
                    .getErrorCode());

            return;
        }

        Authentication userAuthentication = UsernamePasswordAuthenticationToken.authenticated(
            new User(provisionedUser.login(), "", provisionedUser.authorities()), null,
            provisionedUser.authorities());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(userAuthentication);

        SecurityContextHolder.setContext(securityContext);

        securityContextRepository.saveContext(securityContext, request, response);

        if (tenantService.isMultiTenantEnabled()) {
            request.getSession()
                .setAttribute(TenantConstants.CURRENT_TENANT_ID, locatedIdentityProvider.get()
                    .tenantId());
        }

        if (twoFactorAuthenticationCustomizer != null &&
            twoFactorAuthenticationCustomizer.isTotpEnabled(userAuthentication)) {

            TwoFactorAuthenticationSupport.beginSecondFactor(request, response, userAuthentication);

            response.sendRedirect("/login?mfa=required");

            return;
        }

        rememberMeServices.loginSuccess(request, response, userAuthentication);

        response.sendRedirect("/oauth2/redirect");
    }

    @SuppressFBWarnings(
        value = "UNVALIDATED_REDIRECT",
        justification = "The Location is this application's login page; the error code is one of this class's constants")
    private void reject(HttpServletRequest request, HttpServletResponse response, String errorCode)
        throws IOException {

        rememberMeServices.loginFail(request, response);

        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect("/login?error=saml&code=" + URLEncoder.encode(errorCode, StandardCharsets.UTF_8));
    }

    /**
     * The parts of a validated SAML assertion this handler needs. Spring Security 7 puts the relying party registration
     * id on the {@link Saml2AssertionAuthentication} rather than on the principal, whose own accessor then returns
     * {@code null}; attributes are read from the principal, falling back to the assertion accessor.
     */
    record SamlAssertion(String registrationId, String nameId, Map<String, List<Object>> attributes) {

        static SamlAssertion of(Authentication authentication) {
            String registrationId = null;
            Saml2ResponseAssertionAccessor saml2ResponseAssertionAccessor = null;

            if (authentication instanceof Saml2AssertionAuthentication saml2AssertionAuthentication) {
                registrationId = saml2AssertionAuthentication.getRelyingPartyRegistrationId();
                saml2ResponseAssertionAccessor = saml2AssertionAuthentication.getCredentials();
            }

            if (authentication.getPrincipal() instanceof Saml2AuthenticatedPrincipal saml2AuthenticatedPrincipal) {
                if (registrationId == null) {
                    registrationId = saml2AuthenticatedPrincipal.getRelyingPartyRegistrationId();
                }

                return new SamlAssertion(
                    registrationId, saml2AuthenticatedPrincipal.getName(), saml2AuthenticatedPrincipal.getAttributes());
            }

            if (saml2ResponseAssertionAccessor != null) {
                return new SamlAssertion(
                    registrationId, saml2ResponseAssertionAccessor.getNameId(),
                    saml2ResponseAssertionAccessor.getAttributes());
            }

            return null;
        }

        String firstAttribute(List<String> attributeNames) {
            if (attributes == null) {
                return null;
            }

            for (String attributeName : attributeNames) {
                List<Object> values = attributes.get(attributeName);

                if (values != null && !values.isEmpty() && values.getFirst() != null) {
                    return values.getFirst()
                        .toString();
                }
            }

            return null;
        }
    }
}
