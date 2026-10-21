/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.app;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppKeyAccess;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.ee.automation.app.security.AppUser;
import com.bytechef.ee.automation.app.security.AppUserAuthenticationException;
import com.bytechef.ee.automation.app.security.AppUserAuthenticator;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Authenticates an app request by its secret key. The lookup is by the hash of the key and is deliberately not cached,
 * so rotating a key or suspending an app or instance takes effect on the next request. A key that {@link AppKeyAccess}
 * does not allow is rejected here with {@link DisabledException}, which the API key filter maps to 401; the execution
 * facades re-check the status and answer 403 {@code APP_DISABLED} for an app or instance disabled between
 * authentication and execution. After the key, a user token, when present, is verified on every request and is never
 * downgraded to no user; without one, an instance that requires sign-in answers {@code APP_USER_REQUIRED}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final AppInstanceService appInstanceService;
    private final AppService appService;
    private final AppUserAuthenticator appUserAuthenticator;

    @SuppressFBWarnings("EI")
    public AppApiKeyAuthenticationProvider(
        AppInstanceService appInstanceService, AppService appService, AppUserAuthenticator appUserAuthenticator) {

        this.appInstanceService = appInstanceService;
        this.appService = appService;
        this.appUserAuthenticator = appUserAuthenticator;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        AppAuthenticationToken appAuthenticationToken = (AppAuthenticationToken) authentication;

        String secretKey = appAuthenticationToken.getSecretKey();

        if (secretKey == null) {
            throw new BadCredentialsException("Unknown app key");
        }

        AppInstance appInstance = appInstanceService.fetchAppInstanceBySecretKey(secretKey)
            .orElseThrow(() -> new BadCredentialsException("Unknown app key"));

        App app = appService.fetchApp(appInstance.getAppId())
            .orElseThrow(() -> new BadCredentialsException("Unknown app key"));

        if (!AppKeyAccess.isAllowed(app.getStatus(), appInstance.getStatus())) {
            throw new DisabledException("App is disabled");
        }

        Long workspaceId = appInstance.getWorkspaceId();

        if (workspaceId == null) {
            throw new BadCredentialsException("App has no workspace");
        }

        long appInstanceId = Objects.requireNonNull(appInstance.getId());

        appInstanceService.touchSecretKeyLastUsedDate(appInstanceId);

        String tenantId = Objects.requireNonNull(appAuthenticationToken.getTenantId());

        AppUser appUser = authenticateUser(appAuthenticationToken, appInstance, tenantId);

        return new AppAuthenticationToken(
            new AppPrincipal(
                tenantId, app.getId(), appInstanceId, workspaceId, appInstance.getEnvironment(), appUser));
    }

    private @Nullable AppUser authenticateUser(
        AppAuthenticationToken appAuthenticationToken, AppInstance appInstance, String tenantId) {

        String userToken = appAuthenticationToken.getUserToken();

        if (userToken != null) {
            return appUserAuthenticator.authenticate(appInstance, tenantId, userToken);
        }

        if (appInstance.getUserAuthMode() == AppUserAuthMode.REQUIRED && !appAuthenticationToken.isUserOptional()) {
            throw new AppUserAuthenticationException(AppErrorType.APP_USER_REQUIRED, "A signed-in user is required");
        }

        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(AppAuthenticationToken.class);
    }
}
