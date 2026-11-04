/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.client.config;

import static org.mockito.Mockito.mock;

import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionImpl;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.notification.delivery.WebhookDelivery;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.delivery.WebhookNotificationClient;
import com.bytechef.platform.notification.delivery.WebhookPayload;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
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
    Encryption encryption(EncryptionKey encryptionKey) {
        return new EncryptionImpl(encryptionKey);
    }

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

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
    RecordingWebhookDeliveryService recordingWebhookDeliveryService() {
        return new RecordingWebhookDeliveryService();
    }

    @Bean
    SlackNotificationSender slackNotificationSender(RecordingWebhookDeliveryService recordingWebhookDeliveryService) {
        return new SlackNotificationSender(recordingWebhookDeliveryService);
    }

    @Bean
    WebhookNotificationSender webhookNotificationSender(
        RecordingWebhookDeliveryService recordingWebhookDeliveryService) {

        return new WebhookNotificationSender(recordingWebhookDeliveryService);
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

    public record RecordedWebhookDelivery(String url, String payloadJson, @Nullable String secret) {
    }

    public static class RecordingWebhookDeliveryService extends WebhookDeliveryService {

        private final List<RecordedWebhookDelivery> recordedWebhookDeliveries = new CopyOnWriteArrayList<>();
        private volatile WebhookDeliveryOutcome.Status status = WebhookDeliveryOutcome.Status.SENT;

        @SuppressWarnings("unchecked")
        public RecordingWebhookDeliveryService() {
            super(
                new WebhookNotificationClient(), new SimpleAsyncTaskExecutor(), webhookDeliveryApplicationProperties(),
                mock(ObjectProvider.class));
        }

        private static ApplicationProperties webhookDeliveryApplicationProperties() {
            ApplicationProperties applicationProperties = new ApplicationProperties();

            ApplicationProperties.WebhookDelivery webhookDelivery = applicationProperties.getWebhookDelivery();

            webhookDelivery.setConcurrencyLimit(1);
            webhookDelivery.setMaxPendingDeliveries(1);

            return applicationProperties;
        }

        public List<RecordedWebhookDelivery> getRecordedWebhookDeliveries() {
            return List.copyOf(recordedWebhookDeliveries);
        }

        public void clear() {
            recordedWebhookDeliveries.clear();

            status = WebhookDeliveryOutcome.Status.SENT;
        }

        public void setStatus(WebhookDeliveryOutcome.Status status) {
            this.status = status;
        }

        @Override
        public CompletableFuture<WebhookDeliveryOutcome> submit(WebhookDelivery webhookDelivery) {
            for (WebhookPayload webhookPayload : webhookDelivery.payloads()) {
                recordedWebhookDeliveries
                    .add(new RecordedWebhookDelivery(webhookDelivery.url(), webhookPayload.json(),
                        webhookDelivery.secret()));
            }

            List<WebhookDeliveryOutcome.PayloadResult> payloadResults = webhookDelivery.payloads()
                .stream()
                .map(webhookPayload -> new WebhookDeliveryOutcome.PayloadResult(webhookPayload.label(), status))
                .toList();

            return CompletableFuture.completedFuture(new WebhookDeliveryOutcome(payloadResults, null, null, null));
        }
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    static class NotificationJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        NotificationJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
            this.encryption = encryption;
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
                new MapWrapperToStringConverter(objectMapper), new StringToMapWrapperConverter(objectMapper));
        }
    }
}
