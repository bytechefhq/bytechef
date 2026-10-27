/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.grpc;

import com.bytechef.ee.platform.notification.remote.grpc.proto.RemoteNotification;
import com.bytechef.ee.platform.notification.remote.grpc.proto.RemoteNotifications;
import com.bytechef.ee.remote.grpc.ProtoJsonMapper;
import com.bytechef.platform.notification.domain.Notification;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class NotificationProtoMapper {

    private final ProtoJsonMapper protoJsonMapper;

    public NotificationProtoMapper(ObjectMapper objectMapper) {
        this.protoJsonMapper = new ProtoJsonMapper(objectMapper);
    }

    public Notification toNotification(RemoteNotification remoteNotification) {
        return protoJsonMapper.toObject(remoteNotification, Notification.class);
    }

    public List<Notification> toNotifications(RemoteNotifications remoteNotifications) {
        return remoteNotifications.getNotificationsList()
            .stream()
            .map(this::toNotification)
            .toList();
    }

    public RemoteNotification toProto(Notification notification) {
        return protoJsonMapper.toProto(notification, RemoteNotification.newBuilder());
    }

    public RemoteNotifications toProto(List<Notification> notifications) {
        RemoteNotifications.Builder builder = RemoteNotifications.newBuilder();

        for (Notification notification : notifications) {
            builder.addNotifications(toProto(notification));
        }

        return builder.build();
    }
}
