/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Authenticates the carved-out embedded endpoints as the API key's own ByteChef user, with that user's real
 * authorities.
 *
 * <p>
 * {@link EmbeddedApiKeyAuthenticationProvider} deliberately does the opposite: it resolves a {@code ConnectedUser} --
 * the customer's end user, who holds no roles in this tenant -- and issues a principal with zero authorities. That is
 * correct for the connected-user embedded API and useless for an operation guarded by {@code ROLE_ADMIN}, since no such
 * facade guard can ever accept it. The endpoints that need a real ByteChef user are therefore carved out of that
 * configurer and handled here instead.
 *
 * <p>
 * Only a typed API key is accepted -- an {@code AUTOMATION} or an {@code EMBEDDED} key. The admin key, which is the one
 * carrying no {@code PlatformType}, is rejected here and reserved for the tenant-wide operations under
 * {@code /api/platform/v1}. Authorization for the operations themselves is unchanged: it stays on the facades, as
 * {@code ROLE_ADMIN} evaluated against the authorities of the key's owning user.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedPlatformUserApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyService apiKeyService;
    private final AuthorityService authorityService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public EmbeddedPlatformUserApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        this.apiKeyService = apiKeyService;
        this.authorityService = authorityService;
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        EmbeddedPlatformUserApiKeyAuthenticationToken embeddedAdminApiKeyAuthenticationToken =
            (EmbeddedPlatformUserApiKeyAuthenticationToken) authentication;

        ApiKey apiKey;

        try {
            apiKey = apiKeyService.getApiKey(
                embeddedAdminApiKeyAuthenticationToken.getSecretKey(),
                embeddedAdminApiKeyAuthenticationToken.getEnvironmentId());
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Unknown API secret key", e);
        }

        if (apiKey.getType() == null) {
            throw new BadCredentialsException("API key required");
        }

        org.springframework.security.core.userdetails.User user = userService.fetchUser(apiKey.getUserId())
            .map(curUser -> createSpringSecurityUser(
                embeddedAdminApiKeyAuthenticationToken.getSecretKey(), curUser))
            .orElseThrow(() -> new UsernameNotFoundException(
                "User with token " + embeddedAdminApiKeyAuthenticationToken.getSecretKey()
                    + " was not found in the database"));

        return new EmbeddedPlatformUserApiKeyAuthenticationToken(user);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(EmbeddedPlatformUserApiKeyAuthenticationToken.class);
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String secretKey, User user) {
        if (!user.isActivated()) {
            throw new UserNotActivatedException("User " + secretKey + " was not activated");
        }

        List<SimpleGrantedAuthority> grantedAuthorities = user.getAuthorityIds()
            .stream()
            .map(authorityService::fetchAuthority)
            .map(Optional::get)
            .map(Authority::getName)
            .map(SimpleGrantedAuthority::new)
            .toList();

        return new org.springframework.security.core.userdetails.User(
            user.getLogin(), user.getPassword(), grantedAuthorities);
    }
}
