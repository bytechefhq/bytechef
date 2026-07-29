/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.workspace.repository;

import com.bytechef.platform.notification.domain.Notification;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * The workspace dimension of the CE {@code notification} table. Deliberately a bare {@link Repository} rather than a
 * CRUD repository: notification CRUD belongs to the CE {@code NotificationRepository}, and this EE type only reads and
 * writes the {@code workspace_id} scoping column.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface NotificationWorkspaceRepository extends Repository<Notification, Long> {

    /**
     * The notification's workspace, or empty when it is global or does not exist. Both answer empty on purpose: that is
     * exactly what the superseded {@code workspace_notification} lookup returned when no membership row existed.
     */
    @Query("SELECT workspace_id FROM notification WHERE id = :notificationId")
    Optional<Long> findWorkspaceIdByNotificationId(@Param("notificationId") long notificationId);

    /**
     * Points the notification at {@code workspaceId}, or makes it global again when that is null. A targeted UPDATE
     * rather than a load-mutate-save so it neither bumps the optimistic-locking version nor races a concurrent edit of
     * the notification's own fields — workspace scoping is an orthogonal dimension to the notification's content.
     */
    @Modifying
    @Query("UPDATE notification SET workspace_id = :workspaceId WHERE id = :notificationId")
    void updateWorkspaceId(@Param("notificationId") long notificationId, @Param("workspaceId") Long workspaceId);
}
