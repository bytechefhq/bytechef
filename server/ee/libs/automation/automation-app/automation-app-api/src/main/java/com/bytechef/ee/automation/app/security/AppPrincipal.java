/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The authenticated principal of a request made to {@code /api/app/v1/**} with an app secret key. It is the app, never
 * a user: the environment and workspace come from the app instance row, not from the request. {@code user} is the
 * signed-in user of a request that also carried a valid user token, and is recorded, not run as.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AppPrincipal(
    String tenantId, long appId, long appInstanceId, long workspaceId, Environment environment,
    @Nullable AppUser user) {

    public AppPrincipal(String tenantId, long appId, long appInstanceId, long workspaceId, Environment environment) {
        this(tenantId, appId, appInstanceId, workspaceId, environment, null);
    }

    public static Optional<AppPrincipal> current() {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof AppPrincipal appPrincipal) {
            return Optional.of(appPrincipal);
        }

        return Optional.empty();
    }

    public static AppPrincipal getCurrent() {
        return current().orElseThrow(() -> new AccessDeniedException("Not an app request"));
    }

    public @Nullable Long userId() {
        return user == null ? null : user.id();
    }
}
