/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import com.bytechef.platform.constant.PlatformType;
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
 * Authenticates an API key as its own ByteChef user, with that user's authorities, instead of as a connected user. Only
 * an embedded key is accepted.
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
        EmbeddedPlatformUserApiKeyAuthenticationToken embeddedPlatformUserApiKeyAuthenticationToken =
            (EmbeddedPlatformUserApiKeyAuthenticationToken) authentication;

        ApiKey apiKey = apiKeyService
            .fetchApiKey(embeddedPlatformUserApiKeyAuthenticationToken.getSecretKey(),
                embeddedPlatformUserApiKeyAuthenticationToken.getEnvironmentId())
            .orElseThrow(() -> new BadCredentialsException("Unknown API secret key"));

        if (apiKey.getType() != PlatformType.EMBEDDED) {
            throw new BadCredentialsException("Embedded API key required");
        }

        Long userId = apiKey.getUserId();

        if (userId == null) {
            throw new BadCredentialsException("API key has no owner");
        }

        org.springframework.security.core.userdetails.User user = userService.fetchUser(userId)
            .map(this::createSpringSecurityUser)
            .orElseThrow(() -> new UsernameNotFoundException("User " + userId + " was not found in the database"));

        return new EmbeddedPlatformUserApiKeyAuthenticationToken(
            embeddedPlatformUserApiKeyAuthenticationToken.getEnvironmentId(), user, apiKey.getId());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(EmbeddedPlatformUserApiKeyAuthenticationToken.class);
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(User user) {
        if (!user.isActivated()) {
            throw new UserNotActivatedException("User " + user.getLogin() + " was not activated");
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
