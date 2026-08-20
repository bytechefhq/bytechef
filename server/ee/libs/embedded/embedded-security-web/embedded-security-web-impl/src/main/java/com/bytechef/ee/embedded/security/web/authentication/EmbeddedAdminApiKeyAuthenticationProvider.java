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
 * Authenticates the embedded admin endpoints as the API key's own ByteChef user, with that user's real authorities.
 *
 * <p>
 * {@link EmbeddedApiKeyAuthenticationProvider} deliberately does the opposite: it resolves a {@code ConnectedUser} --
 * the customer's end user, who holds no roles in this tenant -- and issues a principal with zero authorities. That is
 * correct for the tenant-facing embedded API and useless for an admin operation, since no {@code ROLE_ADMIN} facade
 * guard can ever accept it. Admin endpoints under {@code /api/embedded/} are therefore carved out of that configurer
 * and handled here instead.
 *
 * <p>
 * Only admin API keys are accepted. An admin key is the one carrying no {@code PlatformType}, since
 * {@code getAdminApiKeys(environmentId)} is {@code getApiKeys(environmentId, null)} -- a non-null type means an
 * automation or embedded key was presented. The operations behind these endpoints act on the whole tenant rather than
 * on one workspace or environment, so accepting a workspace-scoped key would promise a containment they do not provide.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedAdminApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyService apiKeyService;
    private final AuthorityService authorityService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public EmbeddedAdminApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        this.apiKeyService = apiKeyService;
        this.authorityService = authorityService;
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        EmbeddedAdminApiKeyAuthenticationToken embeddedAdminApiKeyAuthenticationToken =
            (EmbeddedAdminApiKeyAuthenticationToken) authentication;

        ApiKey apiKey;

        try {
            apiKey = apiKeyService.getApiKey(
                embeddedAdminApiKeyAuthenticationToken.getSecretKey(),
                embeddedAdminApiKeyAuthenticationToken.getEnvironmentId());
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Unknown API secret key", e);
        }

        if (apiKey.getType() != null) {
            throw new BadCredentialsException("Admin API key required");
        }

        org.springframework.security.core.userdetails.User user = userService.fetchUser(apiKey.getUserId())
            .map(curUser -> createSpringSecurityUser(
                embeddedAdminApiKeyAuthenticationToken.getSecretKey(), curUser))
            .orElseThrow(() -> new UsernameNotFoundException(
                "User with token " + embeddedAdminApiKeyAuthenticationToken.getSecretKey()
                    + " was not found in the database"));

        return new EmbeddedAdminApiKeyAuthenticationToken(user);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(EmbeddedAdminApiKeyAuthenticationToken.class);
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
