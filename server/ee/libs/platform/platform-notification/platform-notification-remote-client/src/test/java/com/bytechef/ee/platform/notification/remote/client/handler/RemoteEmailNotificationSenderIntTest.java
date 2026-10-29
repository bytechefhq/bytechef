/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.client.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.notification.remote.client.config.NotificationRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.notification.remote.client.config.NotificationRemoteClientIntTestConfiguration.RecordingJavaMailSender;
import com.bytechef.ee.platform.notification.remote.grpc.NotificationProtoMapper;
import com.bytechef.ee.platform.notification.remote.grpc.NotificationRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.EmailNotificationHandler;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
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
class RemoteEmailNotificationSenderIntTest {

    @Autowired
    private NotificationSenderRegistry notificationSenderRegistry;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecordingJavaMailSender recordingJavaMailSender;

    private ManagedChannel managedChannel;
    private RemoteEmailNotificationSender remoteEmailNotificationSender;
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

        remoteEmailNotificationSender = new RemoteEmailNotificationSender(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        recordingJavaMailSender.clear();
    }

    @Test
    void testSendMailsTheContentTheCallerRenderedToTheConfiguredAddress() throws IOException, MessagingException {
        NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder()
            .eventType(NotificationEvent.Type.JOB_FAILED)
            .jobId(7L)
            .jobName("Nightly")
            .build();

        remoteEmailNotificationSender.send(
            notification(), new JobFailedEmailNotificationHandler(true), notificationHandlerContext);

        List<MimeMessage> sentMimeMessages = recordingJavaMailSender.getSentMimeMessages();

        assertThat(sentMimeMessages).hasSize(1);

        MimeMessage sentMimeMessage = sentMimeMessages.getFirst();

        assertThat(sentMimeMessage.getSubject()).isEqualTo("Job Nightly failed");
        assertThat(sentMimeMessage.getAllRecipients())
            .extracting(Address::toString)
            .containsExactly("ops@example.com");
        assertThat(sentMimeMessage.getFrom())
            .extracting(Address::toString)
            .containsExactly(NotificationRemoteClientIntTestConfiguration.MAIL_FROM);
        assertThat(sentMimeMessage.getContent()).hasToString("<p>Job 7 failed</p>");
        assertThat(sentMimeMessage.getDataHandler()
            .getContentType()).startsWith("text/html");
    }

    @Test
    void testSendPlainTextContentKeepsItPlain() throws MessagingException {
        remoteEmailNotificationSender.send(
            notification(), new JobFailedEmailNotificationHandler(false),
            new NotificationHandlerContext.Builder()
                .jobId(8L)
                .jobName("Hourly")
                .build());

        List<MimeMessage> sentMimeMessages = recordingJavaMailSender.getSentMimeMessages();

        assertThat(sentMimeMessages).hasSize(1);

        MimeMessage sentMimeMessage = sentMimeMessages.getFirst();

        assertThat(sentMimeMessage.getSubject()).isEqualTo("Job Hourly failed");
        assertThat(sentMimeMessage.getDataHandler()
            .getContentType()).startsWith("text/plain");
    }

    private static Notification notification() {
        Notification notification = new Notification();

        notification.setId(5L);
        notification.setName("Failed jobs");
        notification.setNotificationEventIds(List.of(4L));
        notification.setSettings(Map.of("email", "ops@example.com"));
        notification.setType(Notification.Type.EMAIL);

        return notification;
    }

    private record JobFailedEmailNotificationHandler(boolean html) implements EmailNotificationHandler {

        @Override
        public String getContent(NotificationHandlerContext notificationHandlerContext) {
            return "<p>Job " + notificationHandlerContext.getJobId() + " failed</p>";
        }

        @Override
        public String getSubject(NotificationHandlerContext notificationHandlerContext) {
            return "Job " + notificationHandlerContext.getJobName() + " failed";
        }

        @Override
        public boolean isHtml() {
            return html;
        }
    }
}
