/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.grpc;

import com.bytechef.ee.platform.notification.remote.grpc.proto.GetNotificationsRequest;
import com.bytechef.ee.platform.notification.remote.grpc.proto.NotificationRemoteServiceGrpc;
import com.bytechef.ee.platform.notification.remote.grpc.proto.RemoteNotifications;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.service.NotificationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class NotificationRemoteServiceGrpcServer
    extends NotificationRemoteServiceGrpc.NotificationRemoteServiceImplBase {

    private final NotificationProtoMapper notificationProtoMapper;
    private final NotificationService notificationService;

    @SuppressFBWarnings("EI")
    public NotificationRemoteServiceGrpcServer(
        NotificationService notificationService, NotificationProtoMapper notificationProtoMapper) {

        this.notificationProtoMapper = notificationProtoMapper;
        this.notificationService = notificationService;
    }

    @Override
    public void getNotifications(
        GetNotificationsRequest request, StreamObserver<RemoteNotifications> responseObserver) {

        List<Notification> notifications = request.hasEventType()
            ? notificationService.getNotifications(NotificationEvent.Type.valueOf(request.getEventType()))
            : notificationService.getNotifications();

        responseObserver.onNext(notificationProtoMapper.toProto(notifications));
        responseObserver.onCompleted();
    }
}
