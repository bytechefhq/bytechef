/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.app;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.ee.automation.app.security.AppUserAuthenticator;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.tenant.TenantIdValidator;
import com.bytechef.tenant.domain.TenantKey;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Authenticates {@code /api/app/v*} requests with {@code Authorization: Bearer <app key>}, and the signed-in user with
 * the optional {@value #USER_TOKEN_HEADER} header. A rejected key or user token answers a marked
 * {@code {"code", "message"}} error, like the app controllers do.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppApiKeySecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    public static final String PATH_PATTERN = "^/api/app/v[0-9]+/.+";
    public static final String USER_TOKEN_HEADER = "X-ByteChef-User-Token";

    private static final String USER_OPTIONAL_PATH_PATTERN =
        "^/api/app/v[0-9]+/(context|connections|actions|tools|workflows|api-collections)(\\?.*)?$";

    private final AppApiSecurityErrorHandler appApiSecurityErrorHandler = new AppApiSecurityErrorHandler();

    public AppApiKeySecurityConfigurer(
        AppInstanceService appInstanceService, AppService appService, AppUserAuthenticator appUserAuthenticator,
        EnvironmentService environmentService) {

        super(
            PATH_PATTERN, new AppApiKeyAuthenticationConverter(environmentService),
            new AppApiKeyAuthenticationProvider(appInstanceService, appService, appUserAuthenticator));
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

        private static final RequestMatcher USER_OPTIONAL_REQUEST_MATCHER = regexMatcher(
            HttpMethod.GET, USER_OPTIONAL_PATH_PATTERN);

        AppApiKeyAuthenticationConverter(EnvironmentService environmentService) {
            super(environmentService);
        }

        /**
         * Reads the tenant from the key, ignoring {@code X-ENVIRONMENT}, and turns every malformed key into a
         * {@link BadCredentialsException}. The {@value AppApiKeySecurityConfigurer#USER_TOKEN_HEADER} header is carried
         * to the provider unread. An instance that requires sign-in answers only the read-only requests that describe
         * the instance without a user: {@code GET /context}, where an app reads its sign-in settings, and the discovery
         * lists {@code GET /connections}, {@code /actions}, {@code /tools}, {@code /workflows} and
         * {@code /api-collections}, which carry what the app may call and nothing of any user.
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

            return new AppAuthenticationToken(
                authToken, tenantId, request.getHeader(USER_TOKEN_HEADER),
                USER_OPTIONAL_REQUEST_MATCHER.matches(request));
        }
    }
}
