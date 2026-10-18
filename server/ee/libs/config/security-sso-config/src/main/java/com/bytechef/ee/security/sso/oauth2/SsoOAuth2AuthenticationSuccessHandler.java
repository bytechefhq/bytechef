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
import com.bytechef.platform.security.web.config.TwoFactorAuthenticationCustomizer;
import com.bytechef.security.web.oauth2.OAuth2AuthenticationSuccessHandler;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * OAuth2 success handler used while SSO is enabled. An SSO login belongs to the tenant that owns its identity provider,
 * which the registration id names; social logins fall back to finding the signed-in user's tenant.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SsoOAuth2AuthenticationSuccessHandler extends OAuth2AuthenticationSuccessHandler {

    private final IdentityProviderLocator identityProviderLocator;

    @SuppressFBWarnings("EI")
    public SsoOAuth2AuthenticationSuccessHandler(
        IdentityProviderLocator identityProviderLocator, RememberMeServices rememberMeServices,
        TenantService tenantService, TwoFactorAuthenticationCustomizer twoFactorAuthenticationCustomizer) {

        super(rememberMeServices, tenantService, twoFactorAuthenticationCustomizer);

        this.identityProviderLocator = identityProviderLocator;
    }

    @Override
    protected Optional<String> resolveTenantId(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oAuth2AuthenticationToken) {
            String registrationId = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();

            if (registrationId.startsWith(IdentityProviderRegistrationId.OIDC_PREFIX)) {
                return identityProviderLocator.findByRegistrationId(registrationId)
                    .map(LocatedIdentityProvider::tenantId);
            }
        }

        return super.resolveTenantId(authentication);
    }
}
