/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.configurer;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.ee.embedded.security.web.authentication.EmbeddedAdminApiKeyAuthenticationProvider;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedAdminApiKeyAuthenticationToken;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;

/**
 * Authenticates the admin endpoints mounted under {@code /api/embedded/v<n>/}.
 *
 * <p>
 * These paths are deliberately excluded from {@link EmbeddedApiKeySecurityConfigurer}, which claims every other
 * {@code /api/embedded/v<n>/} path and authenticates it as a {@code ConnectedUser} holding zero authorities -- a
 * principal no {@code ROLE_ADMIN} facade guard can accept. {@code PATH_PATTERN} here and the exclusion there are the
 * same constant for that reason: if the two ever drifted apart, an admin endpoint would silently fall back to
 * connected-user auth and answer 403 forever, or worse, be reachable by a tenant's end-user credential.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedAdminApiKeySecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    /**
     * Admin operations under {@code /api/embedded/}. Read by {@link EmbeddedApiKeySecurityConfigurer} as well, as its
     * carve-out.
     */
    public static final String PATH_PATTERN =
        "^/api/embedded/v[0-9]+/automation-project-code-workflows(?:/.*)?$";

    public EmbeddedAdminApiKeySecurityConfigurer(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        super(
            PATH_PATTERN, new EmbeddedAdminApiKeyAuthenticationConverter(),
            new EmbeddedAdminApiKeyAuthenticationProvider(apiKeyService, authorityService, userService));
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(regexMatcher(PATH_PATTERN));
    }

    private static class EmbeddedAdminApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

        @Override
        protected Authentication doConvert(int environment, String authToken, String tenantId) {
            return new EmbeddedAdminApiKeyAuthenticationToken(environment, authToken, tenantId);
        }
    }
}
