/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.workspace.service;

import com.bytechef.platform.notification.domain.Notification;
import java.util.List;
import java.util.Optional;

/**
 * Workspace scoping for the CE notification registry. A notification whose {@code workspace_id} is null is global;
 * consumers that present a workspace-scoped picker should offer {@link #getNotifications(long)} — the workspace's own
 * notifications plus the globals.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceNotificationService {

    void assignNotificationToWorkspace(long notificationId, long workspaceId);

    Optional<Long> fetchWorkspaceIdByNotificationId(long notificationId);

    /** The workspace's own notifications plus the global (unassigned) ones, in name order. */
    List<Notification> getNotifications(long workspaceId);

    void unassignNotification(long notificationId);
}
