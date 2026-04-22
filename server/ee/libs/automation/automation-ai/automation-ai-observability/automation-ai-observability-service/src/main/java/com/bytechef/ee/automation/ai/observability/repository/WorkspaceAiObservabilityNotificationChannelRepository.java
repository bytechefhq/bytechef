/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.repository;

import com.bytechef.ee.automation.ai.observability.domain.WorkspaceAiObservabilityNotificationChannel;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityNotificationChannel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @version ee
 */
public interface WorkspaceAiObservabilityNotificationChannelRepository
    extends ListCrudRepository<WorkspaceAiObservabilityNotificationChannel, Long> {

    Optional<WorkspaceAiObservabilityNotificationChannel> findByAiObservabilityNotificationChannelId(
        long aiObservabilityNotificationChannelId);

    @Query("""
        SELECT ai_observability_notification_channel.*
        FROM ai_observability_notification_channel
        JOIN workspace_ai_observability_notification_channel
            ON workspace_ai_observability_notification_channel.ai_observability_notification_channel_id
                 = ai_observability_notification_channel.id
        WHERE workspace_ai_observability_notification_channel.workspace_id = :workspaceId
        """)
    List<AiObservabilityNotificationChannel> findAllChannelsByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
