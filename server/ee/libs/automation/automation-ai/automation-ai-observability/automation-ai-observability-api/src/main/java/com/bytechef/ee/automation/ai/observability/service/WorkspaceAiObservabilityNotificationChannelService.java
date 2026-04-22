/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityNotificationChannel;
import java.util.List;

/**
 * Workspace-scoped operations on {@link AiObservabilityNotificationChannel}.
 *
 * @version ee
 */
public interface WorkspaceAiObservabilityNotificationChannelService {

    AiObservabilityNotificationChannel createInWorkspace(
        AiObservabilityNotificationChannel notificationChannel, long workspaceId);

    void delete(long id);

    List<AiObservabilityNotificationChannel> getNotificationChannelsByWorkspace(Long workspaceId);

    Long getWorkspaceId(long notificationChannelId);
}
