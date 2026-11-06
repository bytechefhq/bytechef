/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertCondition;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertEvent;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertMetric;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRule;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRuleChannel;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.notification.delivery.WebhookDelivery;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.delivery.WebhookPayload;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.service.NotificationService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class AiObservabilityNotificationDispatcherTest {

    @Mock
    private MailService mailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private WebhookDeliveryService webhookDeliveryService;

    private AiObservabilityNotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new AiObservabilityNotificationDispatcher(
            mailServiceObjectProvider(mailService), notificationService, webhookDeliveryService);
    }

    @Test
    void testDispatchDeliversEmailThroughMailService() {
        Notification notification = notification(Notification.Type.EMAIL, Map.of("email", "ops@example.com"));

        when(notificationService.getNotification(10L)).thenReturn(notification);

        dispatcher.dispatch(rule(10L), event());

        verify(mailService).sendEmail(
            eq("ops@example.com"), contains("[ByteChef Alert]"), anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    void testDispatchDeliversSlackThroughCentralClient() {
        Notification notification = notification(
            Notification.Type.SLACK, Map.of("slackWebhookUrl", "https://hooks.slack.com/services/T/C/X"));

        when(notificationService.getNotification(11L)).thenReturn(notification);

        dispatcher.dispatch(rule(11L), event());

        ArgumentCaptor<WebhookDelivery> webhookDeliveryArgumentCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(webhookDeliveryService).submit(webhookDeliveryArgumentCaptor.capture());

        WebhookDelivery webhookDelivery = webhookDeliveryArgumentCaptor.getValue();

        assertThat(webhookDelivery.url()).isEqualTo("https://hooks.slack.com/services/T/C/X");

        WebhookPayload webhookPayload = webhookDelivery.payloads()
            .getFirst();

        assertThat(webhookPayload.json()).contains("breach");
    }

    @Test
    void testBlankSettingsAreSkippedWithoutThrowing() {
        Notification notification = notification(Notification.Type.EMAIL, Map.of());

        when(notificationService.getNotification(12L)).thenReturn(notification);

        dispatcher.dispatch(rule(12L), event());

        verify(mailService, never()).sendEmail(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    void testBrokenTargetDoesNotAbortFanOut() {
        Notification slackNotification = notification(
            Notification.Type.SLACK, Map.of("slackWebhookUrl", "https://hooks.slack.com/services/T/C/X"));

        when(notificationService.getNotification(13L)).thenThrow(new IllegalArgumentException("missing"));
        when(notificationService.getNotification(14L)).thenReturn(slackNotification);

        AiObservabilityAlertRule rule = rule(13L);

        rule.setChannels(
            Set.of(new AiObservabilityAlertRuleChannel(13L), new AiObservabilityAlertRuleChannel(14L)));

        dispatcher.dispatch(rule, event());

        verify(webhookDeliveryService).submit(any(WebhookDelivery.class));
    }

    @Test
    void testEmailIsSkippedWhenNoMailServiceIsAvailable() {
        Notification notification = notification(Notification.Type.EMAIL, Map.of("email", "ops@example.com"));

        when(notificationService.getNotification(15L)).thenReturn(notification);

        AiObservabilityNotificationDispatcher mailLessDispatcher = new AiObservabilityNotificationDispatcher(
            mailServiceObjectProvider(null), notificationService, webhookDeliveryService);

        mailLessDispatcher.dispatch(rule(15L), event());

        verify(mailService, never()).sendEmail(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean());
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MailService> mailServiceObjectProvider(MailService mailService) {
        ObjectProvider<MailService> objectProvider = mock(ObjectProvider.class);

        lenient()
            .when(objectProvider.getIfAvailable())
            .thenReturn(mailService);

        return objectProvider;
    }

    private static AiObservabilityAlertRule rule(long notificationId) {
        AiObservabilityAlertRule rule = new AiObservabilityAlertRule(
            "rule", AiObservabilityAlertMetric.ERROR_RATE, AiObservabilityAlertCondition.GREATER_THAN,
            BigDecimal.ONE, 5, 0);

        rule.setChannels(Set.of(new AiObservabilityAlertRuleChannel(notificationId)));

        return rule;
    }

    private static AiObservabilityAlertEvent event() {
        return new AiObservabilityAlertEvent(1L, BigDecimal.TWO, "breach");
    }

    private static Notification notification(Notification.Type type, Map<String, Object> settings) {
        Notification notification = new Notification();

        notification.setName("target");
        notification.setType(type);
        notification.setSettings(settings);

        return notification;
    }
}
