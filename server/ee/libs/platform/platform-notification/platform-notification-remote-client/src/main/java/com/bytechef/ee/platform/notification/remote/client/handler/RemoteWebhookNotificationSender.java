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
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.WebhookNotificationHandler;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteWebhookNotificationSender extends AbstractRemoteNotificationSender<WebhookNotificationHandler> {

    public RemoteWebhookNotificationSender(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        super(grpcChannels, objectMapper);
    }

    @Override
    public Notification.Type getType() {
        return Notification.Type.WEBHOOK;
    }

    @Override
    public void send(
        Notification notification, @Nullable WebhookNotificationHandler webhookNotificationHandler,
        NotificationHandlerContext notificationHandlerContext) {

        SendNotificationRequest.Builder sendNotificationRequestBuilder = SendNotificationRequest.newBuilder();

        if (webhookNotificationHandler != null) {
            sendNotificationRequestBuilder
                .setContent(webhookNotificationHandler.getPayload(notificationHandlerContext));
        }

        send(notification, sendNotificationRequestBuilder);
    }
}
