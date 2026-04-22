/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql.authorization;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Centralizes the workspace-role check previously duplicated across seven AI-Gateway GraphQL controllers. One source of
 * truth for the deny path means a change to the authorization rule (e.g. adding a legal-hold scope) applies everywhere
 * instead of having to be replicated in each controller.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class WorkspaceAuthorization {

    private final PermissionService permissionService;

    public WorkspaceAuthorization(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Throws {@link AccessDeniedException} if the currently-authenticated caller is not a member of {@code workspaceId}
     * at or above the {@code minimumRole} level. Role vocabulary matches
     * {@code com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole} ({@code ADMIN}, {@code EDITOR},
     * {@code VIEWER}).
     */
    public void requireWorkspaceRole(long workspaceId, String minimumRole) {
        if (!permissionService.hasWorkspaceRole(workspaceId, minimumRole)) {
            throw new AccessDeniedException(
                "Not authorized for workspace " + workspaceId + " (requires " + minimumRole + ")");
        }
    }
}
