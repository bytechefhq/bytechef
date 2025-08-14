/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.security.web.config;

import com.bytechef.ee.automation.apiplatform.configuration.service.ApiClientService;
import com.bytechef.ee.automation.apiplatform.handler.security.web.authentication.ApiPlatformAuthenticationProvider;
import com.bytechef.platform.security.web.config.AuthenticationProviderContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ApiPlatformAuthenticationProviderContributor implements AuthenticationProviderContributor {

    private final ApiClientService apiClientService;

    @SuppressFBWarnings("EI")
    public ApiPlatformAuthenticationProviderContributor(ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return new ApiPlatformAuthenticationProvider(apiClientService);
    }
}
