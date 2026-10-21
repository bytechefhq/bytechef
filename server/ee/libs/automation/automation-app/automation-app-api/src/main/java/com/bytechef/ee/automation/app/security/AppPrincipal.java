/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The authenticated principal of a request made to {@code /api/app/v1/**} with an app secret key. It is the app, never
 * a user: the environment and workspace come from the app row, not from the request.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AppPrincipal(String tenantId, long appId, long workspaceId, Environment environment) {

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
}
