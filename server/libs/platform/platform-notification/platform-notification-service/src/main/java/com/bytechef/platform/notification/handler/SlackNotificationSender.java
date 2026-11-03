/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.notification.handler;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.notification.delivery.WebhookDelivery;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.delivery.WebhookRetry;
import com.bytechef.platform.notification.delivery.WebhookTestResult;
import com.bytechef.platform.notification.domain.Notification;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class SlackNotificationSender implements NotificationSender<SlackNotificationHandler> {

    private final WebhookDeliveryService webhookDeliveryService;

    @SuppressFBWarnings("EI2")
    public SlackNotificationSender(WebhookDeliveryService webhookDeliveryService) {
        this.webhookDeliveryService = webhookDeliveryService;
    }

    @Override
    public Notification.Type getType() {
        return Notification.Type.SLACK;
    }

    @Override
    public CompletableFuture<Void> send(
        Notification notification, SlackNotificationHandler slackNotificationHandler,
        NotificationHandlerContext notificationHandlerContext) {

        String slackWebhookUrl = (String) notification.getSetting("slackWebhookUrl");

        if (slackWebhookUrl == null || slackWebhookUrl.isBlank()) {
            throw new IllegalStateException(
                "Notification " + notification.getId() + " has no Slack webhook URL configured");
        }

        WebhookDelivery webhookDelivery = buildDelivery(
            notification, slackNotificationHandler, notificationHandlerContext, slackWebhookUrl);

        return webhookDeliveryService.submit(webhookDelivery)
            .thenAccept(NotificationDeliveryOutcomes::requireAnySent);
    }

    @Override
    public NotificationTestResult sendTest(
        Notification notification, SlackNotificationHandler slackNotificationHandler,
        NotificationHandlerContext notificationHandlerContext) {

        String slackWebhookUrl = (String) notification.getSetting("slackWebhookUrl");

        if (slackWebhookUrl == null || slackWebhookUrl.isBlank()) {
            return NotificationTestResult.failed("Slack webhook URL is required");
        }

        WebhookTestResult webhookTestResult = webhookDeliveryService.sendTest(
            buildDelivery(notification, slackNotificationHandler, notificationHandlerContext, slackWebhookUrl));

        TestSendRedaction testSendRedaction = TestSendRedaction.of(List.of(slackWebhookUrl));

        return new NotificationTestResult(
            webhookTestResult.success(), webhookTestResult.statusCode(),
            testSendRedaction.redact(webhookTestResult.responseBody()),
            testSendRedaction.redact(webhookTestResult.error()), false);
    }

    private static WebhookDelivery buildDelivery(
        Notification notification, SlackNotificationHandler slackNotificationHandler,
        NotificationHandlerContext notificationHandlerContext, String slackWebhookUrl) {

        String text = slackNotificationHandler.getText(notificationHandlerContext);

        return WebhookDelivery.builder("slack", slackWebhookUrl.strip())
            .description("notification " + notification.getId() + " '" + notification.getName() + "'")
            .eventType("slack.message")
            .validateUrl(true)
            .payload(String.valueOf(notificationHandlerContext.getJobId()), JsonUtils.write(Map.of("text", text)))
            .retry(WebhookRetry.none())
            .build();
    }
}
