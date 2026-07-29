/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.workspace.service;

import com.bytechef.ee.platform.notification.workspace.domain.WorkspaceNotification;
import com.bytechef.ee.platform.notification.workspace.repository.WorkspaceNotificationRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.service.NotificationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final WorkspaceNotificationRepository workspaceNotificationRepository;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceNotificationServiceImpl(
        NotificationService notificationService, WorkspaceNotificationRepository workspaceNotificationRepository) {

        this.notificationService = notificationService;
        this.workspaceNotificationRepository = workspaceNotificationRepository;
    }

    @Override
    public void assignNotificationToWorkspace(long notificationId, long workspaceId) {
        Optional<WorkspaceNotification> existingWorkspaceNotification =
            workspaceNotificationRepository.findByNotificationId(notificationId);

        if (existingWorkspaceNotification.isPresent()) {
            WorkspaceNotification workspaceNotification = existingWorkspaceNotification.get();

            if (workspaceNotification.getWorkspaceId() == workspaceId) {
                return;
            }

            workspaceNotificationRepository.delete(workspaceNotification);
        }

        workspaceNotificationRepository.save(new WorkspaceNotification(notificationId, workspaceId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> fetchWorkspaceIdByNotificationId(long notificationId) {
        return workspaceNotificationRepository.findByNotificationId(notificationId)
            .map(WorkspaceNotification::getWorkspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotifications(long workspaceId) {
        Set<Long> workspaceNotificationIds = workspaceNotificationRepository.findAllByWorkspaceId(workspaceId)
            .stream()
            .map(WorkspaceNotification::getNotificationId)
            .collect(Collectors.toSet());

        Set<Long> allAssignedNotificationIds = workspaceNotificationRepository.findAll()
            .stream()
            .map(WorkspaceNotification::getNotificationId)
            .collect(Collectors.toSet());

        return notificationService.getNotifications()
            .stream()
            .filter(
                notification -> workspaceNotificationIds.contains(notification.getId()) ||
                    !allAssignedNotificationIds.contains(notification.getId()))
            .sorted(Comparator.comparing(Notification::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Override
    public void unassignNotification(long notificationId) {
        workspaceNotificationRepository.deleteByNotificationId(notificationId);
    }
}
