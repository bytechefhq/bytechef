/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.configurer;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.ee.embedded.security.web.authentication.EmbeddedPlatformUserApiKeyAuthenticationProvider;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedPlatformUserApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;

/**
 * Authenticates the {@code /api/embedded/v<n>/} endpoints that act for a ByteChef user rather than a connected user.
 * {@link EmbeddedApiKeySecurityConfigurer} excludes {@link #PATH_PATTERN}, so the two must stay the same constant.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedPlatformUserApiKeySecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    public static final String PATH_PATTERN =
        "^/api/embedded/v[0-9]+/(?:automation-project-code-workflows|integration-code-workflows)(?:/.*)?$";

    public EmbeddedPlatformUserApiKeySecurityConfigurer(
        ApiKeyService apiKeyService, EnvironmentService environmentService, AuthorityService authorityService,
        UserService userService) {

        super(
            PATH_PATTERN, new EmbeddedPlatformUserApiKeyAuthenticationConverter(environmentService),
            new EmbeddedPlatformUserApiKeyAuthenticationProvider(apiKeyService, authorityService, userService));
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(regexMatcher(PATH_PATTERN));
    }

    private static class EmbeddedPlatformUserApiKeyAuthenticationConverter
        extends AbstractApiKeyAuthenticationConverter {

        EmbeddedPlatformUserApiKeyAuthenticationConverter(EnvironmentService environmentService) {
            super(environmentService);
        }

        @Override
        protected Authentication doConvert(int environment, String authToken, String tenantId) {
            return new EmbeddedPlatformUserApiKeyAuthenticationToken(environment, authToken, tenantId);
        }
    }
}
