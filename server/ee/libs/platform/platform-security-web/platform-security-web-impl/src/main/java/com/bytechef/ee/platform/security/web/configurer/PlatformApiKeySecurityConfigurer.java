/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.configurer;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.ee.platform.security.web.authentication.PlatformApiKeyAuthenticationProvider;
import com.bytechef.ee.platform.security.web.authentication.PlatformApiKeyAuthenticationToken;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.Objects;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class PlatformApiKeySecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    protected static final String PATH_PATTERN = "^/api/platform/v[0-9]+/.+";

    public PlatformApiKeySecurityConfigurer(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        super(
            PATH_PATTERN, new PlatformApiKeyAuthenticationConverter(),
            new PlatformApiKeyAuthenticationProvider(apiKeyService, authorityService, userService));
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(regexMatcher(PATH_PATTERN));
        // For CORS requests
        csrf.ignoringRequestMatchers(request -> Objects.equals(request.getMethod(), "OPTIONS"));
    }

    private static class PlatformApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

        @Override
        protected Authentication doConvert(int environment, String authToken, String tenantId) {
            return new PlatformApiKeyAuthenticationToken(authToken, tenantId);
        }
    }
}
