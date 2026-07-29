/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.dispatcher;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertEvent;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRule;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.notification.delivery.SlackNotificationClient;
import com.bytechef.platform.notification.delivery.WebhookDeliveryRequest;
import com.bytechef.platform.notification.delivery.WebhookNotificationClient;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.service.NotificationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Delivers a fired alert to every {@code Notification} row referenced by the rule — the platform-notification registry
 * is THE central point for channels, so alert rules reference its rows instead of defining channel entities of their
 * own. Per-channel mechanics reuse the central transports: {@code MailService} (EMAIL — the single email path),
 * {@code WebhookNotificationClient} (WEBHOOK — SSRF-validated, signed when {@code webhookSecret} is configured), and
 * {@code SlackNotificationClient} (SLACK). {@code @Async} + per-target error logging so a slow SMTP/webhook endpoint
 * never blocks the coordinator's event fan-out.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
public class WorkflowAlertDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WorkflowAlertDispatcher.class);

    private final MailService mailService;
    private final NotificationService notificationService;
    private final SlackNotificationClient slackNotificationClient;
    private final WebhookNotificationClient webhookNotificationClient;

    public WorkflowAlertDispatcher(
        MailService mailService, NotificationService notificationService,
        SlackNotificationClient slackNotificationClient, WebhookNotificationClient webhookNotificationClient) {

        this.mailService = mailService;
        this.notificationService = notificationService;
        this.slackNotificationClient = slackNotificationClient;
        this.webhookNotificationClient = webhookNotificationClient;
    }

    @Async
    public void dispatch(WorkflowAlertRule workflowAlertRule, WorkflowAlertEvent workflowAlertEvent) {
        for (Long notificationId : workflowAlertRule.getNotificationIds()) {
            try {
                Notification notification = notificationService.getNotification(notificationId);

                switch (notification.getType()) {
                    case EMAIL -> sendEmail(notification, workflowAlertRule, workflowAlertEvent);
                    case WEBHOOK -> sendWebhook(notification, workflowAlertRule, workflowAlertEvent);
                    case SLACK -> sendSlack(notification, workflowAlertRule, workflowAlertEvent);

                    default -> log.warn(
                        "Unsupported notification type {} for alert delivery", notification.getType());
                }
            } catch (RuntimeException exception) {
                log.error(
                    "Failed to deliver workflow alert '{}' (rule {}) to notification {}",
                    workflowAlertRule.getName(), workflowAlertRule.getId(), notificationId, exception);
            }
        }
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private void sendEmail(
        Notification notification, WorkflowAlertRule workflowAlertRule, WorkflowAlertEvent workflowAlertEvent) {

        Map<String, Object> settings = notification.getSettings();

        String email = (String) settings.get("email");

        if (email == null || email.isBlank()) {
            log.warn("Notification {} has no email address configured; skipping alert delivery", notification.getId());

            return;
        }

        mailService.sendEmail(
            email, "[ByteChef Alert] " + workflowAlertRule.getName(),
            """
                Alert: %s
                Rule type: %s
                Threshold: %s
                Triggered value: %s

                %s
                """.formatted(
                workflowAlertRule.getName(),
                workflowAlertRule.getRuleType(), workflowAlertRule.getThreshold(),
                workflowAlertEvent.getTriggeredValue(), workflowAlertEvent.getMessage()),
            false, false);
    }

    private void sendWebhook(
        Notification notification, WorkflowAlertRule workflowAlertRule, WorkflowAlertEvent workflowAlertEvent) {

        Map<String, Object> settings = notification.getSettings();

        String url = (String) settings.get("webhook");

        if (url == null || url.isBlank()) {
            log.warn("Notification {} has no webhook url configured; skipping alert delivery", notification.getId());

            return;
        }

        Map<String, Object> payload = new HashMap<>();

        payload.put("alertRuleId", workflowAlertRule.getId());
        payload.put("alertRuleName", workflowAlertRule.getName());
        payload.put(
            "ruleType", workflowAlertRule.getRuleType()
                .name());
        payload.put("threshold", workflowAlertRule.getThreshold());
        payload.put("triggeredValue", workflowAlertEvent.getTriggeredValue());
        payload.put("message", workflowAlertEvent.getMessage());
        payload.put("jobId", workflowAlertEvent.getJobId());

        webhookNotificationClient.deliver(
            new WebhookDeliveryRequest(
                url, "workflow.alert", JsonUtils.write(payload), Map.of(), (String) settings.get("webhookSecret")));
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private void sendSlack(
        Notification notification, WorkflowAlertRule workflowAlertRule, WorkflowAlertEvent workflowAlertEvent) {

        Map<String, Object> settings = notification.getSettings();

        String slackWebhookUrl = (String) settings.get("slackWebhookUrl");

        if (slackWebhookUrl == null || slackWebhookUrl.isBlank()) {
            log.warn(
                "Notification {} has no Slack webhook url configured; skipping alert delivery", notification.getId());

            return;
        }

        slackNotificationClient.send(
            slackWebhookUrl,
            ":rotating_light: *Alert: %s*\n%s\nTriggered value: %s".formatted(
                workflowAlertRule.getName(), workflowAlertEvent.getMessage(),
                workflowAlertEvent.getTriggeredValue()));
    }
}
