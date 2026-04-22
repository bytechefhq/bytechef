/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannel;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityNotificationChannelService {

    AiObservabilityNotificationChannel create(AiObservabilityNotificationChannel notificationChannel);

    void delete(long id);

    AiObservabilityNotificationChannel getNotificationChannel(long id);

    List<AiObservabilityNotificationChannel> getNotificationChannelsByWorkspace(Long workspaceId);

    boolean test(long id);

    AiObservabilityNotificationChannel update(AiObservabilityNotificationChannel notificationChannel);
}
