/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.service;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertEvent;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertEventStatus;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRule;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRuleChannel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.notification.delivery.WebhookDelivery;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.delivery.WebhookRetry;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.service.NotificationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @version ee
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings({
    "EI", "EI2"
})
public class AiObservabilityNotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AiObservabilityNotificationDispatcher.class);

    private final ObjectProvider<MailService> mailServiceObjectProvider;
    private final NotificationService notificationService;
    private final WebhookDeliveryService webhookDeliveryService;

    AiObservabilityNotificationDispatcher(
        ObjectProvider<MailService> mailServiceObjectProvider, NotificationService notificationService,
        WebhookDeliveryService webhookDeliveryService) {

        this.mailServiceObjectProvider = mailServiceObjectProvider;
        this.notificationService = notificationService;
        this.webhookDeliveryService = webhookDeliveryService;
    }

    public void dispatch(AiObservabilityAlertRule alertRule, AiObservabilityAlertEvent alertEvent) {
        for (AiObservabilityAlertRuleChannel ruleChannel : alertRule.getChannels()) {
            try {
                Notification notification = notificationService.getNotification(ruleChannel.notificationId());

                switch (notification.getType()) {
                    case EMAIL -> sendEmail(notification, alertRule, alertEvent);
                    case WEBHOOK -> sendWebhook(notification, alertRule, alertEvent);
                    case SLACK -> sendSlack(notification, alertRule, alertEvent);

                    default -> log.warn(
                        "Unsupported notification type {} for AI observability alert delivery",
                        notification.getType());
                }
            } catch (RuntimeException exception) {
                log.error(
                    "Failed to dispatch AI observability alert for rule {} to notification {}",
                    alertRule.getId(), ruleChannel.notificationId(), exception);
            }
        }
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private void sendEmail(
        Notification notification, AiObservabilityAlertRule alertRule, AiObservabilityAlertEvent alertEvent) {

        String email = (String) notification.getSetting("email");

        if (email == null || email.isBlank()) {
            log.warn("Notification {} has no email address configured; skipping alert delivery", notification.getId());

            return;
        }

        MailService mailService = mailServiceObjectProvider.getIfAvailable();

        if (mailService == null) {
            log.warn(
                "No mail service is available in this deployment; skipping email alert delivery for notification {}",
                notification.getId());

            return;
        }

        boolean resolved = alertEvent.getStatus() == AiObservabilityAlertEventStatus.RESOLVED;
        String subjectPrefix = resolved ? "[ByteChef Alert RESOLVED]" : "[ByteChef Alert]";

        mailService.sendEmail(
            email, String.format("%s %s", subjectPrefix, alertRule.getName()),
            String.format(
                "Alert: %s%nStatus: %s%n%nMessage: %s%nTriggered value: %s%nThreshold: %s%nMetric: %s%n",
                alertRule.getName(),
                alertEvent.getStatus()
                    .name(),
                alertEvent.getMessage(),
                alertEvent.getTriggeredValue(),
                alertRule.getThreshold(),
                alertRule.getMetric()
                    .name()),
            false, false);
    }

    private void sendWebhook(
        Notification notification, AiObservabilityAlertRule alertRule, AiObservabilityAlertEvent alertEvent) {

        String url = (String) notification.getSetting("webhook");

        if (url == null || url.isBlank()) {
            log.warn("Notification {} has no webhook url configured; skipping alert delivery", notification.getId());

            return;
        }

        String payload = JsonUtils.write(
            Map.of(
                "alertRuleId", alertRule.getId(),
                "alertRuleName", alertRule.getName(),
                "metric", alertRule.getMetric()
                    .name(),
                "threshold", alertRule.getThreshold(),
                "triggeredValue", alertEvent.getTriggeredValue(),
                "message", alertEvent.getMessage(),
                "status", alertEvent.getStatus()
                    .name()));

        WebhookDelivery webhookDelivery = WebhookDelivery.builder("ai-observability", url)
            .description("AI observability alert rule " + alertRule.getId() + " '" + alertRule.getName() + "'")
            .eventType("ai-observability.alert")
            .secret((String) notification.getSetting("webhookSecret"))
            .validateUrl(true)
            .payload(String.valueOf(alertRule.getId()), payload)
            .retry(WebhookRetry.none())
            .build();

        webhookDeliveryService.submit(webhookDelivery);
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private void sendSlack(
        Notification notification, AiObservabilityAlertRule alertRule, AiObservabilityAlertEvent alertEvent) {

        String slackWebhookUrl = (String) notification.getSetting("slackWebhookUrl");

        if (slackWebhookUrl == null || slackWebhookUrl.isBlank()) {
            log.warn(
                "Notification {} has no Slack webhook url configured; skipping alert delivery", notification.getId());

            return;
        }

        boolean resolved = alertEvent.getStatus() == AiObservabilityAlertEventStatus.RESOLVED;
        String icon = resolved ? ":white_check_mark:" : ":rotating_light:";
        String statusLabel = resolved ? "RESOLVED" : "Alert";

        String text = String.format(
            "%s *%s: %s*\n%s\nTriggered value: %s", icon, statusLabel, alertRule.getName(), alertEvent.getMessage(),
            alertEvent.getTriggeredValue());

        WebhookDelivery webhookDelivery = WebhookDelivery.builder("slack", slackWebhookUrl.strip())
            .description("AI observability alert rule " + alertRule.getId() + " '" + alertRule.getName() + "'")
            .eventType("slack.message")
            .validateUrl(true)
            .payload(String.valueOf(alertRule.getId()), JsonUtils.write(Map.of("text", text)))
            .retry(WebhookRetry.none())
            .build();

        webhookDeliveryService.submit(webhookDelivery);
    }
}
