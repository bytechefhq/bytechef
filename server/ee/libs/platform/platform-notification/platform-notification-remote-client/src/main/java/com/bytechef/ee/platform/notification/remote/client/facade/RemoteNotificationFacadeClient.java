/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.client.facade;

import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.dto.NotificationDTO;
import com.bytechef.platform.notification.facade.NotificationFacade;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteNotificationFacadeClient implements NotificationFacade {

    @Override
    public List<NotificationDTO> getNotifications() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotificationDTO createNotification(Notification notification) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotificationDTO updateNotification(Notification notification) {
        throw new UnsupportedOperationException();
    }
}
