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
 * @version ee
 * @author Ivica Cardic
 */
public interface NotificationWorkspaceRepository extends Repository<Notification, Long> {

    @Query("SELECT workspace_id FROM notification WHERE id = :notificationId")
    Optional<Long> findWorkspaceIdByNotificationId(@Param("notificationId") long notificationId);

    @Modifying
    @Query("UPDATE notification SET workspace_id = :workspaceId WHERE id = :notificationId")
    void updateWorkspaceId(@Param("notificationId") long notificationId, @Param("workspaceId") Long workspaceId);
}
