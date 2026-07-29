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
import com.bytechef.platform.notification.delivery.SlackNotificationClient;
import com.bytechef.platform.notification.delivery.WebhookDeliveryRequest;
import com.bytechef.platform.notification.delivery.WebhookNotificationClient;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.service.NotificationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Delivers AI-observability alerts through the central platform-notification registry: each alert rule references
 * {@code Notification} rows (post channel migration), and the per-channel mechanics reuse the central transports —
 * {@code MailService} (the single email path), {@code WebhookNotificationClient} (SSRF-validated, signed when
 * {@code webhookSecret} is configured), and {@code SlackNotificationClient}. Per-target failures are logged and never
 * abort the fan-out to the remaining targets.
 *
 * @version ee
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class AiObservabilityNotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AiObservabilityNotificationDispatcher.class);

    private final MailService mailService;
    private final NotificationService notificationService;
    private final SlackNotificationClient slackNotificationClient;
    private final WebhookNotificationClient webhookNotificationClient;

    AiObservabilityNotificationDispatcher(
        MailService mailService, NotificationService notificationService,
        SlackNotificationClient slackNotificationClient, WebhookNotificationClient webhookNotificationClient) {

        this.mailService = mailService;
        this.notificationService = notificationService;
        this.slackNotificationClient = slackNotificationClient;
        this.webhookNotificationClient = webhookNotificationClient;
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

        Map<String, Object> settings = notification.getSettings();

        String email = (String) settings.get("email");

        if (email == null || email.isBlank()) {
            log.warn("Notification {} has no email address configured; skipping alert delivery", notification.getId());

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

        Map<String, Object> settings = notification.getSettings();

        String url = (String) settings.get("webhook");

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

        webhookNotificationClient.deliver(
            new WebhookDeliveryRequest(
                url, "ai-observability.alert", payload, Map.of(), (String) settings.get("webhookSecret")));
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private void sendSlack(
        Notification notification, AiObservabilityAlertRule alertRule, AiObservabilityAlertEvent alertEvent) {

        Map<String, Object> settings = notification.getSettings();

        String slackWebhookUrl = (String) settings.get("slackWebhookUrl");

        if (slackWebhookUrl == null || slackWebhookUrl.isBlank()) {
            log.warn(
                "Notification {} has no Slack webhook url configured; skipping alert delivery", notification.getId());

            return;
        }

        boolean resolved = alertEvent.getStatus() == AiObservabilityAlertEventStatus.RESOLVED;
        String icon = resolved ? ":white_check_mark:" : ":rotating_light:";
        String statusLabel = resolved ? "RESOLVED" : "Alert";

        slackNotificationClient.send(
            slackWebhookUrl,
            String.format(
                "%s *%s: %s*\n%s\nTriggered value: %s",
                icon, statusLabel, alertRule.getName(), alertEvent.getMessage(),
                alertEvent.getTriggeredValue()));
    }
}
