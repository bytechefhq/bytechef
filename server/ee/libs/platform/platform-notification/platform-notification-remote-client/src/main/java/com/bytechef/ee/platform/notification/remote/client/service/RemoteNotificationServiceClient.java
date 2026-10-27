/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.client.service;

import com.bytechef.ee.platform.notification.remote.grpc.NotificationProtoMapper;
import com.bytechef.ee.platform.notification.remote.grpc.proto.GetNotificationsRequest;
import com.bytechef.ee.platform.notification.remote.grpc.proto.NotificationRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.service.NotificationService;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteNotificationServiceClient implements NotificationService {

    private static final String SERVICE_ID = "configuration-app";

    private final GrpcChannels grpcChannels;
    private final NotificationProtoMapper notificationProtoMapper;

    public RemoteNotificationServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.grpcChannels = grpcChannels;
        this.notificationProtoMapper = new NotificationProtoMapper(objectMapper);
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
        return notificationProtoMapper.toNotifications(
            getStub().getNotifications(GetNotificationsRequest.getDefaultInstance()));
    }

    @Override
    public List<Notification> getNotifications(NotificationEvent.Type eventType) {
        return notificationProtoMapper.toNotifications(
            getStub().getNotifications(
                GetNotificationsRequest.newBuilder()
                    .setEventType(eventType.name())
                    .build()));
    }

    @Override
    public Notification update(Notification notification) {
        throw new UnsupportedOperationException();
    }

    private NotificationRemoteServiceGrpc.NotificationRemoteServiceBlockingStub getStub() {
        return NotificationRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SERVICE_ID));
    }
}
