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
 * @version ee
 * @author Ivica Cardic
 */
public interface WorkspaceNotificationService {

    void assignNotificationToWorkspace(long notificationId, long workspaceId);

    Optional<Long> fetchWorkspaceIdByNotificationId(long notificationId);

    List<Notification> getNotifications(long workspaceId);

    void unassignNotification(long notificationId);
}
