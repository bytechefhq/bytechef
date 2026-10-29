/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.notification.remote.client.config.NotificationRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.notification.remote.grpc.NotificationProtoMapper;
import com.bytechef.ee.platform.notification.remote.grpc.NotificationRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.repository.NotificationRepository;
import com.bytechef.platform.notification.service.NotificationService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = NotificationRemoteClientIntTestConfiguration.class)
class RemoteNotificationServiceClientIntTest {

    private static final long JOB_CANCELLED_EVENT_ID = 1L;
    private static final long JOB_FAILED_EVENT_ID = 4L;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationSenderRegistry notificationSenderRegistry;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private ManagedChannel managedChannel;
    private RemoteNotificationServiceClient remoteNotificationServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                new NotificationRemoteServiceGrpcServer(
                    notificationService, notificationSenderRegistry, new NotificationProtoMapper(objectMapper)))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteNotificationServiceClient = new RemoteNotificationServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        notificationRepository.deleteAll();
    }

    @Test
    void testGetNotificationsReturnsEveryStoredNotification() {
        Notification failedJobsNotification = notificationService.create(
            notification("Failed jobs", Notification.Type.EMAIL, List.of(JOB_FAILED_EVENT_ID)));
        Notification cancelledJobsNotification = notificationService.create(
            notification("Cancelled jobs", Notification.Type.WEBHOOK, List.of(JOB_CANCELLED_EVENT_ID)));

        List<Notification> notifications = remoteNotificationServiceClient.getNotifications();

        assertThat(notifications)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(failedJobsNotification, cancelledJobsNotification);

        Notification readFailedJobsNotification = notifications.stream()
            .filter(notification -> "Failed jobs".equals(notification.getName()))
            .findFirst()
            .orElseThrow();

        assertThat(readFailedJobsNotification.getSettings())
            .isEqualTo(Map.of("email", "ops@example.com", "retries", 2));
        assertThat(readFailedJobsNotification.getNotificationEventIds()).containsExactly(JOB_FAILED_EVENT_ID);
        assertThat(readFailedJobsNotification.getCreatedBy()).isEqualTo("system");
        assertThat(readFailedJobsNotification.getCreatedDate()).isNotNull();
    }

    @Test
    void testGetNotificationsOfAnEventTypeReturnsOnlyTheSubscribedNotifications() {
        Notification failedJobsNotification = notificationService.create(
            notification("Failed jobs", Notification.Type.EMAIL, List.of(JOB_FAILED_EVENT_ID)));

        notificationService.create(
            notification("Cancelled jobs", Notification.Type.WEBHOOK, List.of(JOB_CANCELLED_EVENT_ID)));

        assertThat(remoteNotificationServiceClient.getNotifications(NotificationEvent.Type.JOB_FAILED))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(failedJobsNotification);
        assertThat(remoteNotificationServiceClient.getNotifications(NotificationEvent.Type.JOB_COMPLETED))
            .isEmpty();
    }

    @Test
    void testGetNotificationsWithoutStoredNotificationsIsEmpty() {
        assertThat(remoteNotificationServiceClient.getNotifications()).isEmpty();
    }

    private static Notification notification(String name, Notification.Type type, List<Long> notificationEventIds) {
        Notification notification = new Notification();

        notification.setName(name);
        notification.setNotificationEventIds(notificationEventIds);
        notification.setSettings(Map.of("email", "ops@example.com", "retries", 2));
        notification.setType(type);

        return notification;
    }
}
