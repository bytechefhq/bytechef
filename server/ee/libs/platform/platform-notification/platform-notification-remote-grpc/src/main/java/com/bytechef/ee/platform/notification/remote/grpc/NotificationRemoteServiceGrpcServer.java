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
import com.bytechef.ee.platform.notification.remote.grpc.proto.SendNotificationRequest;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.EmailNotificationHandler;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationSender;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.handler.WebhookNotificationHandler;
import com.bytechef.platform.notification.service.NotificationService;
import com.google.protobuf.Empty;
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
    private final NotificationSenderRegistry notificationSenderRegistry;
    private final NotificationService notificationService;

    @SuppressFBWarnings("EI")
    public NotificationRemoteServiceGrpcServer(
        NotificationService notificationService, NotificationSenderRegistry notificationSenderRegistry,
        NotificationProtoMapper notificationProtoMapper) {

        this.notificationProtoMapper = notificationProtoMapper;
        this.notificationSenderRegistry = notificationSenderRegistry;
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

    @Override
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public void sendNotification(SendNotificationRequest request, StreamObserver<Empty> responseObserver) {
        Notification notification = notificationProtoMapper.toNotification(request.getNotification());

        NotificationSender notificationSender = notificationSenderRegistry.getNotificationSender(
            notification.getType());

        if (notificationSender == null) {
            throw new IllegalStateException(
                "No notification sender for notification type %s".formatted(notification.getType()));
        }

        notificationSender.send(
            notification,
            new RenderedNotificationHandler(request.getSubject(), request.getContent(), request.getHtml()),
            new NotificationHandlerContext.Builder().build());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    /**
     * The subject and content the calling app rendered with its own notification handler for the event.
     */
    private record RenderedNotificationHandler(String subject, String content, boolean html)
        implements EmailNotificationHandler, WebhookNotificationHandler {

        @Override
        public String getContent(NotificationHandlerContext notificationHandlerContext) {
            return content;
        }

        @Override
        public String getSubject(NotificationHandlerContext notificationHandlerContext) {
            return subject;
        }

        @Override
        public boolean isHtml() {
            return html;
        }
    }
}
