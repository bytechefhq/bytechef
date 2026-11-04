/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.client.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.notification.remote.client.config.NotificationRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.notification.remote.client.config.NotificationRemoteClientIntTestConfiguration.RecordingJavaMailSender;
import com.bytechef.ee.platform.notification.remote.client.config.NotificationRemoteClientIntTestConfiguration.RecordingWebhookDeliveryService;
import com.bytechef.ee.platform.notification.remote.grpc.NotificationProtoMapper;
import com.bytechef.ee.platform.notification.remote.grpc.NotificationRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.handler.EmailNotificationSender;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = NotificationRemoteClientIntTestConfiguration.class)
class RemoteWebhookNotificationSenderIntTest {

    @Autowired
    private EmailNotificationSender emailNotificationSender;

    @Autowired
    private NotificationSenderRegistry notificationSenderRegistry;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecordingJavaMailSender recordingJavaMailSender;

    @Autowired
    private RecordingWebhookDeliveryService recordingWebhookDeliveryService;

    private ManagedChannel managedChannel;
    private Server server;

    @BeforeEach
    void beforeEach() {
        managedChannel = null;
        server = null;
    }

    @AfterEach
    void afterEach() {
        if (managedChannel != null) {
            managedChannel.shutdownNow();
        }

        if (server != null) {
            server.shutdownNow();
        }

        recordingJavaMailSender.clear();
        recordingWebhookDeliveryService.clear();
    }

    @Test
    void testSendDeliversTheCallersPayloadThroughTheServingAppsWebhookSender() throws IOException {
        RemoteWebhookNotificationSender remoteWebhookNotificationSender = startServer(notificationSenderRegistry);

        assertThatCode(
            () -> remoteWebhookNotificationSender.send(
                notification(), notificationHandlerContext -> "{\"jobId\":42}",
                new NotificationHandlerContext.Builder().build()))
                    .doesNotThrowAnyException();

        assertThat(recordingWebhookDeliveryService.getRecordedWebhookDeliveries())
            .singleElement()
            .satisfies(recordedWebhookDelivery -> {
                assertThat(recordedWebhookDelivery.url()).isEqualTo("https://hooks.example.com/jobs");
                assertThat(recordedWebhookDelivery.payloadJson()).isEqualTo("{\"jobId\":42}");
                assertThat(recordedWebhookDelivery.secret()).isEqualTo("hook-secret");
            });
        assertThat(recordingJavaMailSender.getSentMimeMessages()).isEmpty();
    }

    @Test
    void testSendFailsWhenTheServingAppHasNoWebhookSender() throws IOException {
        RemoteWebhookNotificationSender remoteWebhookNotificationSender = startServer(
            new NotificationSenderRegistry(List.of(emailNotificationSender)));

        assertThatThrownBy(
            () -> remoteWebhookNotificationSender.send(
                notification(), null, new NotificationHandlerContext.Builder().build()))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessageContaining("No notification sender for notification type WEBHOOK")
                    .extracting(throwable -> ((StatusRuntimeException) throwable).getStatus()
                        .getCode())
                    .isEqualTo(Status.Code.INTERNAL);

        assertThat(recordingJavaMailSender.getSentMimeMessages()).isEmpty();
        assertThat(recordingWebhookDeliveryService.getRecordedWebhookDeliveries()).isEmpty();
    }

    private RemoteWebhookNotificationSender startServer(NotificationSenderRegistry senderRegistry)
        throws IOException {

        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new NotificationRemoteServiceGrpcServer(
                        notificationService, senderRegistry, new NotificationProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        return new RemoteWebhookNotificationSender(new FixedGrpcChannels(managedChannel), objectMapper);
    }

    private static Notification notification() {
        Notification notification = new Notification();

        notification.setId(6L);
        notification.setName("Failed jobs hook");
        notification.setNotificationEventIds(List.of(4L));
        notification.setSettings(Map.of(
            "email", "ops@example.com", "webhook", "https://hooks.example.com/jobs", "webhookSecret", "hook-secret"));
        notification.setType(Notification.Type.WEBHOOK);

        return notification;
    }
}
