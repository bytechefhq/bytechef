/*
 * Copyright 2023-present ByteChef Inc.
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
public class ApiClientAuthenticationProvider implements AuthenticationProvider {

    private final ApiClientService apiClientService;

    @SuppressFBWarnings("EI")
    public ApiClientAuthenticationProvider(ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiClientKeyAuthenticationToken apiClientKeyAuthenticationToken =
            (ApiClientKeyAuthenticationToken) authentication;

        Optional<ApiClient> apiClientOptional = apiClientService.fetchApiClient(
            apiClientKeyAuthenticationToken.getSecretKey());

        if (apiClientOptional.isEmpty()) {
            throw new BadCredentialsException("Unknown API secret key");
        }

        ApiClient apiClient = apiClientOptional.get();

        return new ApiClientKeyAuthenticationToken(createSpringSecurityUser(apiClient.getName()));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(ApiClientKeyAuthenticationToken.class);
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String secretKey) {
        return new org.springframework.security.core.userdetails.User(secretKey, "", List.of());
    }
}
