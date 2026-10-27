/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.client.handler;

import com.bytechef.ee.platform.notification.remote.grpc.NotificationProtoMapper;
import com.bytechef.ee.platform.notification.remote.grpc.proto.NotificationRemoteServiceGrpc;
import com.bytechef.ee.platform.notification.remote.grpc.proto.SendNotificationRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.handler.NotificationHandler;
import com.bytechef.platform.notification.handler.NotificationSender;
import tools.jackson.databind.ObjectMapper;

/**
 * Sends a notification with the sender of configuration-app, which owns mail delivery, after the calling app rendered
 * it with its own notification handler.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
abstract class AbstractRemoteNotificationSender<T extends NotificationHandler> implements NotificationSender<T> {

    private static final String SERVICE_ID = "configuration-app";

    private final GrpcChannels grpcChannels;
    private final NotificationProtoMapper notificationProtoMapper;

    AbstractRemoteNotificationSender(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.grpcChannels = grpcChannels;
        this.notificationProtoMapper = new NotificationProtoMapper(objectMapper);
    }

    protected void send(Notification notification, SendNotificationRequest.Builder sendNotificationRequestBuilder) {
        sendNotificationRequestBuilder.setNotification(notificationProtoMapper.toProto(notification));

        NotificationRemoteServiceGrpc.NotificationRemoteServiceBlockingStub stub =
            NotificationRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SERVICE_ID));

        stub.sendNotification(sendNotificationRequestBuilder.build());
    }
}
