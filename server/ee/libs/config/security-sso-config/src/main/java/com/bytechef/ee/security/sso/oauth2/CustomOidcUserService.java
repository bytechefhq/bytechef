/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.oauth2;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.oauth2.CustomOidcUser;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Enterprise OIDC user service for SSO login: like the CE {@code SocialOidcUserService}, but for a per-tenant external
 * identity provider (registration id {@code sso-{id}}) it reads that provider's {@code autoProvision} and
 * {@code defaultAuthority} from the {@code IdentityProvider} record instead of using the social-login defaults.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.security.sso", name = "enabled", havingValue = "true")
public class CustomOidcUserService extends OidcUserService {

    private static final String SSO_PREFIX = "sso-";

    private final AuthorityService authorityService;
    private final IdentityProviderService identityProviderService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public CustomOidcUserService(
        AuthorityService authorityService, IdentityProviderService identityProviderService, UserService userService) {

        this.authorityService = authorityService;
        this.identityProviderService = identityProviderService;
        this.userService = userService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        if (oidcUser == null) {
            throw new OAuth2AuthenticationException("No user returned by OIDC provider");
        }

        String email = oidcUser.getEmail();

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not available from OIDC provider");
        }

        String registrationId = userRequest.getClientRegistration()
            .getRegistrationId();

        String authProvider = registrationId.startsWith(SSO_PREFIX) ? "SSO" : registrationId.toUpperCase();

        boolean autoProvision = true;
        // Non-SSO OIDC registrations join an existing instance as the non-privileged ROLE_USER. SSO-configured identity
        // providers override this with their admin-chosen defaultAuthority below.
        String defaultAuthority = AuthorityConstants.USER;

        if (registrationId.startsWith(SSO_PREFIX)) {
            long identityProviderId = Long.parseLong(registrationId.substring(SSO_PREFIX.length()));

            IdentityProvider identityProvider = identityProviderService.getIdentityProvider(identityProviderId);

            autoProvision = identityProvider.isAutoProvision();
            defaultAuthority = identityProvider.getDefaultAuthority();
        }

        User user = userService.findOrCreateSocialUser(
            email, oidcUser.getGivenName(), oidcUser.getFamilyName(), oidcUser.getPicture(), authProvider,
            oidcUser.getSubject(), autoProvision, defaultAuthority);

        List<SimpleGrantedAuthority> grantedAuthorities = user.getAuthorityIds()
            .stream()
            .map(authorityService::fetchAuthority)
            .map(Optional::get)
            .map(Authority::getName)
            .map(SimpleGrantedAuthority::new)
            .toList();

        return new CustomOidcUser(user.getLogin(), grantedAuthorities, oidcUser);
    }
}
