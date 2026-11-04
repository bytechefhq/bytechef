/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.client.handler;

import com.bytechef.ee.platform.notification.remote.grpc.proto.SendNotificationRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.handler.EmailNotificationHandler;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteEmailNotificationSender extends AbstractRemoteNotificationSender<EmailNotificationHandler> {

    public RemoteEmailNotificationSender(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        super(grpcChannels, objectMapper);
    }

    @Override
    public Notification.Type getType() {
        return Notification.Type.EMAIL;
    }

    @Override
    public CompletableFuture<Void> send(
        Notification notification, EmailNotificationHandler emailNotificationHandler,
        NotificationHandlerContext notificationHandlerContext) {

        SendNotificationRequest.Builder sendNotificationRequestBuilder = SendNotificationRequest.newBuilder()
            .setContent(emailNotificationHandler.getContent(notificationHandlerContext))
            .setHtml(emailNotificationHandler.isHtml())
            .setSubject(emailNotificationHandler.getSubject(notificationHandlerContext));

        return send(notification, sendNotificationRequestBuilder);
    }
}
