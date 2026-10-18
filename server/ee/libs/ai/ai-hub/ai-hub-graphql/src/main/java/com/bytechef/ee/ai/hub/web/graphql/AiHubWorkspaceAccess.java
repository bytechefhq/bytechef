/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.exception.ForbiddenException;

/**
 * Workspace gate shared by the AI Hub GraphQL controllers: the caller must hold at least the workspace VIEWER role.
 * Chat-level access is enforced separately by the services.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AiHubWorkspaceAccess {

    private static final String VIEWER_ROLE = "VIEWER";

    private AiHubWorkspaceAccess() {
    }

    static void verifyViewer(PermissionService permissionService, long workspaceId) {
        if (!permissionService.hasWorkspaceRole(workspaceId, VIEWER_ROLE)) {
            throw new ForbiddenException("Workspace is not accessible to the current user");
        }
    }
}
