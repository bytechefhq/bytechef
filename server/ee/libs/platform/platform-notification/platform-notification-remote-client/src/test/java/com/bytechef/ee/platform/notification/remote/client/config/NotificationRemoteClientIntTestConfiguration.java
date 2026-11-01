/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.client.config;

import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.notification.delivery.SlackNotificationClient;
import com.bytechef.platform.notification.delivery.WebhookDeliveryRequest;
import com.bytechef.platform.notification.delivery.WebhookNotificationClient;
import com.bytechef.platform.notification.handler.EmailNotificationSender;
import com.bytechef.platform.notification.handler.NotificationSender;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.handler.SlackNotificationSender;
import com.bytechef.platform.notification.handler.WebhookNotificationSender;
import com.bytechef.platform.notification.repository.NotificationRepository;
import com.bytechef.platform.notification.service.NotificationService;
import com.bytechef.platform.notification.service.NotificationServiceImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@Import({
    LiquibaseConfiguration.class, NotificationRemoteClientIntTestConfiguration.NotificationJdbcConfiguration.class,
    PostgreSQLContainerConfiguration.class
})
@SpringBootConfiguration
public class NotificationRemoteClientIntTestConfiguration {

    public static final String MAIL_FROM = "noreply@bytechef.io";

    @Bean
    ApplicationProperties applicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Mail mail = new ApplicationProperties.Mail();

        mail.setFrom(MAIL_FROM);

        applicationProperties.setMail(mail);

        return applicationProperties;
    }

    @Bean
    EmailNotificationSender emailNotificationSender(MailService mailService) {
        return new EmailNotificationSender(mailService);
    }

    @Bean
    MailService mailService(
        ApplicationProperties applicationProperties, RecordingJavaMailSender recordingJavaMailSender) {

        return new MailService(
            recordingJavaMailSender, applicationProperties, new StaticMessageSource(), new SpringTemplateEngine());
    }

    @Bean
    NotificationSenderRegistry notificationSenderRegistry(List<NotificationSender<?>> notificationSenders) {
        return new NotificationSenderRegistry(notificationSenders);
    }

    @Bean
    NotificationService notificationService(NotificationRepository notificationRepository) {
        return new NotificationServiceImpl(notificationRepository);
    }

    @Bean
    RecordingJavaMailSender recordingJavaMailSender() {
        return new RecordingJavaMailSender();
    }

    @Bean
    RecordingWebhookNotificationClient recordingWebhookNotificationClient() {
        return new RecordingWebhookNotificationClient();
    }

    @Bean
    SlackNotificationSender slackNotificationSender(
        RecordingWebhookNotificationClient recordingWebhookNotificationClient) {

        return new SlackNotificationSender(new SlackNotificationClient(recordingWebhookNotificationClient));
    }

    @Bean
    WebhookNotificationSender webhookNotificationSender(
        RecordingWebhookNotificationClient recordingWebhookNotificationClient) {

        return new WebhookNotificationSender(recordingWebhookNotificationClient);
    }

    public static class RecordingJavaMailSender extends JavaMailSenderImpl {

        private final List<MimeMessage> sentMimeMessages = new CopyOnWriteArrayList<>();

        public List<MimeMessage> getSentMimeMessages() {
            return List.copyOf(sentMimeMessages);
        }

        public void clear() {
            sentMimeMessages.clear();
        }

        @Override
        protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) {
            sentMimeMessages.addAll(List.of(mimeMessages));
        }
    }

    public static class RecordingWebhookNotificationClient extends WebhookNotificationClient {

        private final List<WebhookDeliveryRequest> webhookDeliveryRequests = new CopyOnWriteArrayList<>();

        public List<WebhookDeliveryRequest> getWebhookDeliveryRequests() {
            return List.copyOf(webhookDeliveryRequests);
        }

        public void clear() {
            webhookDeliveryRequests.clear();
        }

        @Override
        public void deliver(WebhookDeliveryRequest webhookDeliveryRequest, WebhookRetry webhookRetry) {
            webhookDeliveryRequests.add(webhookDeliveryRequest);
        }
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    static class NotificationJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        NotificationJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(new MapWrapperToStringConverter(objectMapper),
                new StringToMapWrapperConverter(objectMapper));
        }
    }
}
