/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.security.web.authentication;

import com.bytechef.ee.automation.apiplatform.configuration.service.ApiClientService;
import com.bytechef.platform.security.web.authentication.AuthenticationProviderContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ApiClientAuthenticationProviderContributor implements AuthenticationProviderContributor {

    private final ApiClientService apiClientService;

    @SuppressFBWarnings("EI")
    public ApiClientAuthenticationProviderContributor(ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return new ApiClientAuthenticationProvider(apiClientService);
    }
}
