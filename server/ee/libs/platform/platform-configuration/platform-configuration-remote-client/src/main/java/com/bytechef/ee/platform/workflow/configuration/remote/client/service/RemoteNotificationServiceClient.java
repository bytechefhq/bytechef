/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.configuration.remote.client.service;

import com.bytechef.platform.configuration.domain.Event;
import com.bytechef.platform.configuration.domain.Notification;
import com.bytechef.platform.configuration.service.NotificationService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteNotificationServiceClient implements NotificationService {

    @Override
    public List<Notification> getNotifications() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Notification> getNotifications(Event.Type eventType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Notification create(String name, Notification.Type type, Map<String, String> settings, List<Long> eventIds) {
        throw new UnsupportedOperationException();
    }
}
