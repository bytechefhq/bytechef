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
 * @version ee
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

    public void requireWorkspaceRole(long workspaceId, String minimumRole) {
        if (!permissionService.hasWorkspaceRole(workspaceId, minimumRole)) {
            log.warn("Workspace authorization rejected: workspaceId={} requiredRole={}", workspaceId, minimumRole);

            throw new AccessDeniedException("Not authorized for the requested workspace");
        }
    }
}
