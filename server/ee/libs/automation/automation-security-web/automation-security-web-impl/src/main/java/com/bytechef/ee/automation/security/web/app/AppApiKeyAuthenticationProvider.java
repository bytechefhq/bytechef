/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.app;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.ee.automation.app.service.AppService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Authenticates an app request by its secret key. The lookup is by the hash of the key and is deliberately not cached,
 * so rotating or disabling an app takes effect on the next request. A disabled app is rejected here with
 * {@link DisabledException}, which the API key filter maps to 401; the execution facade re-checks the status and
 * answers 403 {@code APP_DISABLED} for an app disabled between authentication and execution.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final AppService appService;

    @SuppressFBWarnings("EI")
    public AppApiKeyAuthenticationProvider(AppService appService) {
        this.appService = appService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        AppAuthenticationToken appAuthenticationToken = (AppAuthenticationToken) authentication;

        String secretKey = appAuthenticationToken.getSecretKey();

        if (secretKey == null) {
            throw new BadCredentialsException("Unknown app key");
        }

        App app = appService.fetchAppBySecretKey(secretKey)
            .orElseThrow(() -> new BadCredentialsException("Unknown app key"));

        if (app.getStatus() != AppStatus.ENABLED) {
            throw new DisabledException("App is disabled");
        }

        Long workspaceId = app.getWorkspaceId();

        if (workspaceId == null) {
            throw new BadCredentialsException("App has no workspace");
        }

        appService.touchSecretKeyLastUsedDate(Objects.requireNonNull(app.getId()));

        return new AppAuthenticationToken(
            new AppPrincipal(
                Objects.requireNonNull(appAuthenticationToken.getTenantId()), app.getId(), workspaceId,
                app.getEnvironment()));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(AppAuthenticationToken.class);
    }
}
