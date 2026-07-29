/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.workspace.web.graphql;

import com.bytechef.ee.platform.notification.workspace.service.WorkspaceNotificationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * Workspace-scoped read surface over the CE notification registry plus the admin assign/unassign mutations. Channel
 * CRUD stays on the CE notifications REST API — this controller only handles the workspace membership dimension.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
public class WorkspaceNotificationGraphQlController {

    public record WorkspaceScopedNotification(long id, String name, String type) {
    }

    private final WorkspaceNotificationService workspaceNotificationService;

    @SuppressFBWarnings("EI")
    public WorkspaceNotificationGraphQlController(WorkspaceNotificationService workspaceNotificationService) {
        this.workspaceNotificationService = workspaceNotificationService;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<WorkspaceScopedNotification> workspaceNotifications(@Argument long workspaceId) {
        return workspaceNotificationService.getNotifications(workspaceId)
            .stream()
            .map(
                notification -> new WorkspaceScopedNotification(
                    notification.getId(), notification.getName(),
                    notification.getType()
                        .name()))
            .toList();
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public boolean assignNotificationToWorkspace(@Argument long notificationId, @Argument long workspaceId) {
        workspaceNotificationService.assignNotificationToWorkspace(notificationId, workspaceId);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public boolean unassignNotificationFromWorkspace(@Argument long notificationId) {
        workspaceNotificationService.unassignNotification(notificationId);

        return true;
    }
}
