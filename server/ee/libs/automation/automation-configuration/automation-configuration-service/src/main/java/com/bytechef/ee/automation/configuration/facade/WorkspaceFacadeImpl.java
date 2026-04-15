/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class WorkspaceFacadeImpl implements WorkspaceFacade {

    private final PermissionService permissionService;
    private final WorkspaceService workspaceService;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public WorkspaceFacadeImpl(
        PermissionService permissionService, WorkspaceService workspaceService,
        WorkspaceUserService workspaceUserService) {

        this.permissionService = permissionService;
        this.workspaceService = workspaceService;
        this.workspaceUserService = workspaceUserService;
    }

    @Override
    @PreAuthorize("@permissionService.isTenantAdmin() or @permissionService.isCurrentUser(#id)")
    @Transactional(readOnly = true)
    public List<Workspace> getUserWorkspaces(long id) {
        List<Workspace> workspaces = workspaceService.getWorkspaces();

        if (!permissionService.isTenantAdmin()) {
            List<Long> userWorkspaceIds = workspaceUserService.getUserWorkspaceUsers(id)
                .stream()
                .map(WorkspaceUser::getWorkspaceId)
                .toList();

            workspaces = workspaces.stream()
                .filter(workspace -> userWorkspaceIds.contains(workspace.getId()))
                .toList();
        }

        return workspaces;
    }
}
