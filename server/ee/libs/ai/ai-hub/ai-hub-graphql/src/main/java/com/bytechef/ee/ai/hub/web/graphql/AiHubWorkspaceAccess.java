/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.exception.ForbiddenException;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Workspace gate shared by the AI Hub GraphQL controllers: the caller must be a ByteChef user, not an embedded
 * connected user, and must hold at least the workspace VIEWER role. Chat-level access is enforced separately by the
 * services.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AiHubWorkspaceAccess {

    private static final String VIEWER_ROLE = "VIEWER";

    private AiHubWorkspaceAccess() {
    }

    static void rejectConnectedUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        if (securityContext.getAuthentication() instanceof ConnectedUserAuthentication) {
            throw new ForbiddenException("Connected users cannot use the AI Hub");
        }
    }

    static void verifyViewer(PermissionService permissionService, long workspaceId) {
        rejectConnectedUser();

        if (!permissionService.hasWorkspaceRole(workspaceId, VIEWER_ROLE)) {
            throw new ForbiddenException("Workspace is not accessible to the current user");
        }
    }
}
