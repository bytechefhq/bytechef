/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.listener;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.event.UserDeletingEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Removes a user from every workspace when the user is deleted.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class WorkspaceUserDeletionListener {

    private final PermissionService permissionService;
    private final WorkspaceUserRepository workspaceUserRepository;

    @SuppressFBWarnings("EI")
    public WorkspaceUserDeletionListener(
        PermissionService permissionService, WorkspaceUserRepository workspaceUserRepository) {

        this.permissionService = permissionService;
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        List<WorkspaceUser> workspaceUsers = workspaceUserRepository.findAllByUserId(event.userId());

        workspaceUserRepository.deleteAll(workspaceUsers);

        List<Long> workspaceIds = workspaceUsers.stream()
            .map(WorkspaceUser::getWorkspaceId)
            .distinct()
            .toList();

        for (long workspaceId : workspaceIds) {
            permissionService.evictWorkspaceScopeCache(event.userId(), workspaceId);
        }
    }
}
