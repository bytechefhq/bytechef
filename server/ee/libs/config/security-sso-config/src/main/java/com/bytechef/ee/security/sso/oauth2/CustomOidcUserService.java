/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.oauth2;

import com.bytechef.ee.platform.user.domain.IdentityProviderRegistrationId;
import com.bytechef.ee.security.sso.IdentityProviderLocator;
import com.bytechef.ee.security.sso.IdentityProviderLocator.LocatedIdentityProvider;
import com.bytechef.ee.security.sso.SsoUserProvisioner;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.security.web.oauth2.CustomOidcUser;
import com.bytechef.security.web.oauth2.SocialOidcUserService;
import com.bytechef.security.web.oauth2.SocialUserProvisioner;
import com.bytechef.security.web.oauth2.SocialUserProvisioner.ProvisionedUser;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Enterprise OIDC user service, installed in place of the CE {@code SocialOidcUserService} when SSO is enabled. A
 * per-tenant external identity provider (registration id {@code sso-...}) provisions through {@link SsoUserProvisioner}
 * with that provider's settings; every other registration is social login and goes through
 * {@link SocialUserProvisioner} exactly as it would without SSO.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.security.sso", name = "enabled", havingValue = "true")
public class CustomOidcUserService extends OidcUserService {

    private static final String IDENTITY_PROVIDER_NOT_FOUND_ERROR_CODE = "identity_provider_not_found";

    private final IdentityProviderLocator identityProviderLocator;
    private final SocialUserProvisioner socialUserProvisioner;
    private final SsoUserProvisioner ssoUserProvisioner;

    @SuppressFBWarnings("EI")
    public CustomOidcUserService(
        IdentityProviderLocator identityProviderLocator, SocialUserProvisioner socialUserProvisioner,
        SsoUserProvisioner ssoUserProvisioner) {

        this.identityProviderLocator = identityProviderLocator;
        this.socialUserProvisioner = socialUserProvisioner;
        this.ssoUserProvisioner = ssoUserProvisioner;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        if (oidcUser == null) {
            throw new OAuth2AuthenticationException("No user returned by OIDC provider");
        }

        return toCustomOidcUser(userRequest, oidcUser);
    }

    CustomOidcUser toCustomOidcUser(OidcUserRequest userRequest, OidcUser oidcUser) {
        String registrationId = userRequest.getClientRegistration()
            .getRegistrationId();

        if (!registrationId.startsWith(IdentityProviderRegistrationId.OIDC_PREFIX)) {
            ProvisionedUser provisionedUser = socialUserProvisioner.provision(
                SocialOidcUserService.toSocialIdentity(userRequest, oidcUser));

            return new CustomOidcUser(provisionedUser.login(), provisionedUser.authorities(), oidcUser);
        }

        String email = oidcUser.getEmail();

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not available from OIDC provider");
        }

        if (Boolean.FALSE.equals(oidcUser.getEmailVerified())) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("email_not_verified", "Email " + email + " is not verified", null));
        }

        LocatedIdentityProvider locatedIdentityProvider = identityProviderLocator.findByRegistrationId(registrationId)
            .orElseThrow(() -> new OAuth2AuthenticationException(
                new OAuth2Error(
                    IDENTITY_PROVIDER_NOT_FOUND_ERROR_CODE, "Unknown identity provider " + registrationId, null)));

        ProvisionedUser provisionedUser = ssoUserProvisioner.provision(
            locatedIdentityProvider, email, oidcUser.getGivenName(), oidcUser.getFamilyName(), oidcUser.getPicture(),
            oidcUser.getSubject());

        return new CustomOidcUser(provisionedUser.login(), provisionedUser.authorities(), oidcUser);
    }
}
