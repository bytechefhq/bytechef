/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.configuration.remote.client.service;

import com.bytechef.platform.configuration.domain.Notification;
import com.bytechef.platform.configuration.domain.NotificationEvent;
import com.bytechef.platform.configuration.service.NotificationService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteNotificationServiceClient implements NotificationService {

    @Override
    public Notification create(Notification notification) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long notificationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Notification> getNotifications() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Notification> getNotifications(NotificationEvent.Type eventType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Notification update(Notification notification) {
        throw new UnsupportedOperationException();
    }
}
