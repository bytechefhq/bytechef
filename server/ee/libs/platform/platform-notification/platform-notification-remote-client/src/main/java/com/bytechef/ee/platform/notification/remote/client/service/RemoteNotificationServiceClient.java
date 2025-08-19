/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.client.service;

import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.service.NotificationService;
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
