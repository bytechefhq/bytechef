/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.security.web.authentication;

import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiPlatformApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyService apiKeyService;

    @SuppressFBWarnings("EI")
    public ApiPlatformApiKeyAuthenticationProvider(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiPlatformApiKeyAuthenticationToken apiPlatformApiKeyAuthenticationToken =
            (ApiPlatformApiKeyAuthenticationToken) authentication;

        ApiKey apiKey;

        try {
            apiKey = apiKeyService.getApiKey(
                apiPlatformApiKeyAuthenticationToken.getSecretKey(),
                apiPlatformApiKeyAuthenticationToken.getEnvironmentId());
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Unknown API secret key", e);
        }

        return new ApiPlatformApiKeyAuthenticationToken(createSpringSecurityUser(apiKey.getName()));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(ApiPlatformApiKeyAuthenticationToken.class);
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String secretKey) {
        return new org.springframework.security.core.userdetails.User(secretKey, "", List.of());
    }
}
