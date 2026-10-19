/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
@SuppressFBWarnings("EI2")
public class WorkspaceUserGraphQlController {

    private final PermissionService permissionService;

    public WorkspaceUserGraphQlController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @QueryMapping
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public Set<String> myWorkspaceScopes(@Argument long workspaceId, @Argument @Nullable Environment environment) {
        return permissionService.getMyWorkspaceScopes(workspaceId);
    }

    @QueryMapping
    public String myWorkspaceRole(@Argument long workspaceId) {
        return permissionService.getMyWorkspaceRole(workspaceId);
    }
}
