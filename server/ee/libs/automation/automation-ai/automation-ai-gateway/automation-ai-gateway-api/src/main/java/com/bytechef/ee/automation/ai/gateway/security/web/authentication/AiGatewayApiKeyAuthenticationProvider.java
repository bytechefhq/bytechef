/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.security.web.authentication;

import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @version ee
 */
public class AiGatewayApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyService apiKeyService;
    private final AuthorityService authorityService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public AiGatewayApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        this.apiKeyService = apiKeyService;
        this.authorityService = authorityService;
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        AiGatewayApiKeyAuthenticationToken aiGatewayApiKeyAuthenticationToken =
            (AiGatewayApiKeyAuthenticationToken) authentication;

        ApiKey apiKey;

        try {
            apiKey = apiKeyService.getApiKey(
                aiGatewayApiKeyAuthenticationToken.getSecretKey(),
                aiGatewayApiKeyAuthenticationToken.getEnvironmentId());
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new BadCredentialsException("Unknown API secret key", illegalArgumentException);
        }

        org.springframework.security.core.userdetails.User user = userService.fetchUser(apiKey.getUserId())
            .map(this::createSpringSecurityUser)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User associated with the provided API key was not found in the database"));

        return new AiGatewayApiKeyAuthenticationToken(user, apiKey.getId());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(AiGatewayApiKeyAuthenticationToken.class);
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(User user) {
        if (!user.isActivated()) {
            throw new UserNotActivatedException("User " + user.getLogin() + " was not activated");
        }

        List<SimpleGrantedAuthority> grantedAuthorities = user.getAuthorityIds()
            .stream()
            .map(authorityId -> authorityService.fetchAuthority(authorityId)
                .orElseThrow(() -> new AuthenticationServiceException(
                    "Authority with id " + authorityId + " not found for user '" + user.getLogin() +
                        "' during API key authentication")))
            .map(Authority::getName)
            .map(SimpleGrantedAuthority::new)
            .toList();

        if (grantedAuthorities.isEmpty()) {
            throw new AuthenticationServiceException(
                "User '" + user.getLogin() + "' has no resolvable authorities");
        }

        return new org.springframework.security.core.userdetails.User(
            user.getLogin(), user.getPassword(), grantedAuthorities);
    }

}
