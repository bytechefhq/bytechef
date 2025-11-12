/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.security.web.authentication;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiClientService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
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

    private final ApiClientService apiClientService;

    @SuppressFBWarnings("EI")
    public ApiPlatformApiKeyAuthenticationProvider(ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiPlatformApiKeyAuthenticationToken apiPlatformApiKeyAuthenticationToken =
            (ApiPlatformApiKeyAuthenticationToken) authentication;

        Optional<ApiClient> apiClientOptional = apiClientService.fetchApiClient(
            apiPlatformApiKeyAuthenticationToken.getSecretKey(),
            apiPlatformApiKeyAuthenticationToken.getEnvironmentId());

        if (apiClientOptional.isEmpty()) {
            throw new BadCredentialsException("Unknown API secret key");
        }

        ApiClient apiClient = apiClientOptional.get();

        return new ApiPlatformApiKeyAuthenticationToken(createSpringSecurityUser(apiClient.getName()));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(ApiPlatformApiKeyAuthenticationToken.class);
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String secretKey) {
        return new org.springframework.security.core.userdetails.User(secretKey, "", List.of());
    }
}
