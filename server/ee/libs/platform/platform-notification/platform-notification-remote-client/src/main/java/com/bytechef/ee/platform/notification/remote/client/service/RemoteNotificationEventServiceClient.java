/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.client.service;

import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.service.NotificationEventService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteNotificationEventServiceClient implements NotificationEventService {

    @Override
    public List<NotificationEvent> getNotificationEvents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NotificationEvent> getNotificationEvents(List<Long> notificationEventIds) {
        throw new UnsupportedOperationException();
    }
}
