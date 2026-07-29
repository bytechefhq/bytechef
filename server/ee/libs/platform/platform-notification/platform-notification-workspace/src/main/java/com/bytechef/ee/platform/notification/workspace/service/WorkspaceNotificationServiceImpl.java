/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.workspace.service;

import com.bytechef.ee.platform.notification.workspace.repository.NotificationWorkspaceRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.service.NotificationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@Transactional
public class WorkspaceNotificationServiceImpl implements WorkspaceNotificationService {

    private final NotificationService notificationService;
    private final NotificationWorkspaceRepository notificationWorkspaceRepository;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceNotificationServiceImpl(
        NotificationService notificationService, NotificationWorkspaceRepository notificationWorkspaceRepository) {

        this.notificationService = notificationService;
        this.notificationWorkspaceRepository = notificationWorkspaceRepository;
    }

    @Override
    public void assignNotificationToWorkspace(long notificationId, long workspaceId) {
        notificationWorkspaceRepository.updateWorkspaceId(notificationId, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> fetchWorkspaceIdByNotificationId(long notificationId) {
        return notificationWorkspaceRepository.findWorkspaceIdByNotificationId(notificationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotifications(long workspaceId) {
        // A null workspace_id means global — the pre-scoping behavior, and what every notification created before
        // scoping existed still is. This IS-NULL branch is load-bearing: drop it and global notifications stop
        // being offered anywhere; widen it and they get offered to workspaces that never opted in.
        return notificationService.getNotifications()
            .stream()
            .filter(
                notification -> notification.getWorkspaceId() == null ||
                    Objects.equals(notification.getWorkspaceId(), workspaceId))
            .sorted(Comparator.comparing(Notification::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Override
    public void unassignNotification(long notificationId) {
        notificationWorkspaceRepository.updateWorkspaceId(notificationId, null);
    }
}
