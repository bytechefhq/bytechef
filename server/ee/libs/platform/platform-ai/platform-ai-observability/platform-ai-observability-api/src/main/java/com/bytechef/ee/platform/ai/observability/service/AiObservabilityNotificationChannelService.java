/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityNotificationChannel;

/**
 * Workspace-agnostic CRUD for {@link AiObservabilityNotificationChannel}. Workspace association + by-workspace listing
 * live on the automation-side {@code WorkspaceAiObservabilityNotificationChannelService}.
 *
 * @version ee
 */
public interface AiObservabilityNotificationChannelService {

    AiObservabilityNotificationChannel create(AiObservabilityNotificationChannel notificationChannel);

    void delete(long id);

    AiObservabilityNotificationChannel getNotificationChannel(long id);

    boolean test(long id);

    AiObservabilityNotificationChannel update(AiObservabilityNotificationChannel notificationChannel);
}
