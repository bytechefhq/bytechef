/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.security.web.configurer;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.ee.automation.apiplatform.configuration.service.ApiClientService;
import com.bytechef.ee.automation.apiplatform.handler.security.web.authentication.ApiPlatformApiKeyAuthenticationProvider;
import com.bytechef.ee.automation.apiplatform.handler.security.web.authentication.ApiPlatformApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiPlatformApiKeySecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    protected static final String PATH_PATTERN = "^/api/o/.+";

    public ApiPlatformApiKeySecurityConfigurer(ApiClientService apiClientService) {

        super(
            PATH_PATTERN, new ApiPlatformApiKeyAuthenticationConverter(),
            new ApiPlatformApiKeyAuthenticationProvider(apiClientService));
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(regexMatcher(PATH_PATTERN));
    }

    private static class ApiPlatformApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

        @Override
        protected Authentication doConvert(int environment, String authToken, String tenantId) {
            return new ApiPlatformApiKeyAuthenticationToken(environment, authToken, tenantId);
        }
    }
}
