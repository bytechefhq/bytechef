/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.web.graphql.authorization;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Centralizes the workspace-role check used by every AI-Gateway GraphQL controller. One source of truth for the deny
 * path so a change to the authorization rule (e.g. adding a legal-hold scope) applies everywhere instead of having to
 * be replicated in each controller.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class WorkspaceAuthorization {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceAuthorization.class);

    private final PermissionService permissionService;

    public WorkspaceAuthorization(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Throws {@link AccessDeniedException} if the currently-authenticated caller is not a member of {@code workspaceId}
     * at or above the {@code minimumRole} level. Role vocabulary matches
     * {@code com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole} ({@code ADMIN}, {@code EDITOR},
     * {@code VIEWER}).
     *
     * <p>
     * The exception message is uniform ("Not authorized for the requested workspace") so a workspace-A caller cannot
     * enumerate workspace ids by reading the GraphQL error body. The diagnostic context (workspace id + minimum role)
     * is logged at WARN server-side instead.
     */
    public void requireWorkspaceRole(long workspaceId, String minimumRole) {
        if (!permissionService.hasWorkspaceRole(workspaceId, minimumRole)) {
            log.warn("Workspace authorization rejected: workspaceId={} requiredRole={}", workspaceId, minimumRole);

            throw new AccessDeniedException("Not authorized for the requested workspace");
        }
    }
}
