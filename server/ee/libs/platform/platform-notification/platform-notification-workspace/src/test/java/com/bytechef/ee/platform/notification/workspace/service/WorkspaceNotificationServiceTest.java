/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.notification.workspace.repository.NotificationWorkspaceRepository;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.service.NotificationService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationWorkspaceRepository notificationWorkspaceRepository;

    private WorkspaceNotificationService workspaceNotificationService;

    @BeforeEach
    void beforeEach() {
        workspaceNotificationService = new WorkspaceNotificationServiceImpl(
            notificationService, notificationWorkspaceRepository);
    }

    @Test
    void testGetNotificationsReturnsOwnAndGlobalNotificationsInNameOrder() {
        Notification globalNotification = createNotification(1L, "zeta", null);
        Notification ownNotification = createNotification(2L, "Alpha", 10L);
        Notification otherWorkspaceNotification = createNotification(3L, "beta", 20L);

        when(notificationService.getNotifications())
            .thenReturn(List.of(globalNotification, ownNotification, otherWorkspaceNotification));

        List<Notification> notifications = workspaceNotificationService.getNotifications(10L);

        assertThat(notifications).containsExactly(ownNotification, globalNotification);
    }

    @Test
    void testAssignNotificationToWorkspace() {
        workspaceNotificationService.assignNotificationToWorkspace(5L, 10L);

        verify(notificationWorkspaceRepository).updateWorkspaceId(5L, 10L);
    }

    @Test
    void testUnassignNotificationClearsWorkspace() {
        workspaceNotificationService.unassignNotification(5L);

        verify(notificationWorkspaceRepository).updateWorkspaceId(5L, null);
    }

    @Test
    void testFetchWorkspaceIdByNotificationId() {
        when(notificationWorkspaceRepository.findWorkspaceIdByNotificationId(5L)).thenReturn(Optional.of(10L));

        assertThat(workspaceNotificationService.fetchWorkspaceIdByNotificationId(5L)).contains(10L);
    }

    private static Notification createNotification(long id, String name, Long workspaceId) {
        Notification notification = new Notification();

        notification.setId(id);
        notification.setName(name);
        notification.setWorkspaceId(workspaceId);

        return notification;
    }
}
