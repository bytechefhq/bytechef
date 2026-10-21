/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.app;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.tenant.TenantIdValidator;
import com.bytechef.tenant.domain.TenantKey;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * Authenticates {@code /api/app/v*} requests with {@code Authorization: Bearer <app key>}. A rejected key answers a
 * marked {@code {"code", "message"}} error, like the app controllers do.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppApiKeySecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    public static final String PATH_PATTERN = "^/api/app/v[0-9]+/.+";

    private final AppApiSecurityErrorHandler appApiSecurityErrorHandler = new AppApiSecurityErrorHandler();

    public AppApiKeySecurityConfigurer(AppService appService, EnvironmentService environmentService) {
        super(
            PATH_PATTERN, new AppApiKeyAuthenticationConverter(environmentService),
            new AppApiKeyAuthenticationProvider(appService));
    }

    @Override
    protected AuthenticationFailureHandler getAuthenticationFailureHandler() {
        return appApiSecurityErrorHandler;
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(regexMatcher(PATH_PATTERN));
    }

    static class AppApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

        AppApiKeyAuthenticationConverter(EnvironmentService environmentService) {
            super(environmentService);
        }

        /**
         * Reads the tenant from the key, ignoring {@code X-ENVIRONMENT}, and turns every malformed key into a
         * {@link BadCredentialsException}.
         */
        @Override
        public Authentication convert(HttpServletRequest request) {
            String authToken = getAuthToken(request);

            String tenantId;

            try {
                TenantKey tenantKey = TenantKey.parse(authToken);

                tenantId = tenantKey.getTenantId();
            } catch (RuntimeException runtimeException) {
                throw new BadCredentialsException("Malformed app key", runtimeException);
            }

            if (!TenantIdValidator.isValid(tenantId)) {
                throw new BadCredentialsException("Malformed app key");
            }

            return new AppAuthenticationToken(authToken, tenantId);
        }
    }
}
