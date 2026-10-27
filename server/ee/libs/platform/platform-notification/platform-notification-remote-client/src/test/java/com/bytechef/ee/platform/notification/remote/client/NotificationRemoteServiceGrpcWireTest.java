/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.notification.remote.client.service.RemoteNotificationServiceClient;
import com.bytechef.ee.platform.notification.remote.grpc.NotificationProtoMapper;
import com.bytechef.ee.platform.notification.remote.grpc.NotificationRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.service.NotificationService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class NotificationRemoteServiceGrpcWireTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();

    private final NotificationService notificationService = mock(NotificationService.class);

    private ManagedChannel managedChannel;
    private RemoteNotificationServiceClient remoteNotificationServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                new NotificationRemoteServiceGrpcServer(notificationService, new NotificationProtoMapper(JSON_MAPPER)))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteNotificationServiceClient = new RemoteNotificationServiceClient(
            new FixedGrpcChannels(managedChannel), JSON_MAPPER);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testGetNotificationsOfAnEventType() {
        Notification notification = notification();

        when(notificationService.getNotifications(NotificationEvent.Type.JOB_FAILED))
            .thenReturn(List.of(notification));

        assertThat(remoteNotificationServiceClient.getNotifications(NotificationEvent.Type.JOB_FAILED))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(notification);
    }

    @Test
    void testGetAllNotifications() {
        Notification notification = notification();

        when(notificationService.getNotifications()).thenReturn(List.of(notification));

        assertThat(remoteNotificationServiceClient.getNotifications())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(notification);
    }

    private static Notification notification() {
        Notification notification = new Notification();

        notification.setCreatedBy("admin@localhost.com");
        notification.setCreatedDate(Instant.parse("2026-10-26T10:00:00Z"));
        notification.setId(5L);
        notification.setLastModifiedBy("user@localhost.com");
        notification.setLastModifiedDate(Instant.parse("2026-10-27T11:30:00Z"));
        notification.setName("Failed jobs");
        notification.setNotificationEventIds(List.of(3L, 4L));
        notification.setSettings(Map.of("email", "ops@example.com", "retries", 2));
        notification.setType(Notification.Type.EMAIL);
        notification.setVersion(2);

        return notification;
    }
}
