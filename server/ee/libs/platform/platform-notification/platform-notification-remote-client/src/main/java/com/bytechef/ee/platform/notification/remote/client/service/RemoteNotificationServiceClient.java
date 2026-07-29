/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.client.service;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.service.NotificationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteNotificationServiceClient implements NotificationService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String NOTIFICATION_SERVICE = "/remote/notification-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteNotificationServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public Notification create(Notification notification) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long notificationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Notification getNotification(long notificationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Notification> getNotifications() {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(NOTIFICATION_SERVICE + "/get-notifications")
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<Notification> getNotifications(NotificationEvent.Type eventType) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(NOTIFICATION_SERVICE + "/get-notifications-by-event-type/{eventType}")
                .build(eventType),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public Notification update(Notification notification) {
        throw new UnsupportedOperationException();
    }
}
