/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.domain.IdentityProviderDomain;
import com.bytechef.ee.security.sso.IdentityProviderLocator.LocatedIdentityProvider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.oauth2.SocialUserProvisioner.ProvisionedUser;
import com.bytechef.security.web.oauth2.SocialUserResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;

/**
 * Resolves the ByteChef user behind an OIDC or SAML SSO login, inside the tenant that owns the identity provider and
 * with that provider's provisioning settings.
 *
 * <p>
 * Two rules keep one identity provider from reaching accounts it does not own: users are keyed by provider
 * ({@code SSO-{id}} / {@code SAML-{id}}), so a subject issued by one IdP never matches an account linked through
 * another, and the email must be on one of the provider's configured domains before it may link to, or create, an
 * account.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.security.sso", name = "enabled", havingValue = "true")
public class SsoUserProvisioner {

    static final String EMAIL_DOMAIN_NOT_ALLOWED_ERROR_CODE = "email_domain_not_allowed";

    private final AuthorityService authorityService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public SsoUserProvisioner(AuthorityService authorityService, UserService userService) {
        this.authorityService = authorityService;
        this.userService = userService;
    }

    public ProvisionedUser provision(
        LocatedIdentityProvider locatedIdentityProvider, String email, String firstName, String lastName,
        String imageUrl, String subject) {

        IdentityProvider identityProvider = locatedIdentityProvider.identityProvider();

        if (!isOnIdentityProviderDomain(identityProvider, email)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(
                    EMAIL_DOMAIN_NOT_ALLOWED_ERROR_CODE,
                    "Email " + email + " is not on a domain of identity provider " + identityProvider.getName(),
                    null));
        }

        String authProvider = getAuthProvider(identityProvider);

        return IdentityProviderLocator.callWithTenantId(
            locatedIdentityProvider.tenantId(), () -> {
                User user = SocialUserResolver.resolveUser(
                    userService, email, firstName, lastName, imageUrl, authProvider, subject,
                    identityProvider.isAutoProvision(), identityProvider.getDefaultAuthority());

                List<GrantedAuthority> grantedAuthorities = user.getAuthorityIds()
                    .stream()
                    .map(authorityId -> authorityService.fetchAuthority(authorityId)
                        .orElseThrow(() -> new OAuth2AuthenticationException(
                            "Unknown authority id " + authorityId + " for user " + user.getLogin())))
                    .map(Authority::getName)
                    .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                    .toList();

                return new ProvisionedUser(user.getLogin(), grantedAuthorities);
            });
    }

    static String getAuthProvider(IdentityProvider identityProvider) {
        String type = UserConstants.AUTH_PROVIDER_SAML.equals(identityProvider.getType()) ? "SAML" : "SSO";

        return type + "-" + identityProvider.getId();
    }

    private static boolean isOnIdentityProviderDomain(IdentityProvider identityProvider, String email) {
        int atIndex = email.lastIndexOf('@');

        if (atIndex < 0) {
            return false;
        }

        String emailDomain = email.substring(atIndex + 1)
            .toLowerCase(Locale.ROOT);

        Set<IdentityProviderDomain> domains = identityProvider.getDomains();

        return domains != null && domains.stream()
            .map(IdentityProviderDomain::getDomain)
            .anyMatch(domain -> domain.equalsIgnoreCase(emailDomain));
    }
}
