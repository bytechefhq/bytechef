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

import com.bytechef.platform.notification.delivery.SlackNotificationClient;
import com.bytechef.platform.notification.domain.Notification;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Delivers SLACK-channel notifications through the shared {@link SlackNotificationClient}. Settings:
 * {@code slackWebhookUrl} (Slack incoming-webhook URL, required).
 *
 * @author Ivica Cardic
 */
@Component
public class SlackNotificationSender implements NotificationSender<SlackNotificationHandler> {

    private static final Logger log = LoggerFactory.getLogger(SlackNotificationSender.class);

    private final SlackNotificationClient slackNotificationClient;

    public SlackNotificationSender(SlackNotificationClient slackNotificationClient) {
        this.slackNotificationClient = slackNotificationClient;
    }

    @Override
    public Notification.Type getType() {
        return Notification.Type.SLACK;
    }

    @Async
    @Override
    public void send(
        Notification notification, SlackNotificationHandler slackNotificationHandler,
        NotificationHandlerContext notificationHandlerContext) {

        Map<String, Object> settings = notification.getSettings();

        String slackWebhookUrl = (String) settings.get("slackWebhookUrl");

        if (slackWebhookUrl == null || slackWebhookUrl.isBlank()) {
            log.warn(
                "Notification {} has no Slack webhook URL configured; skipping delivery", notification.getId());

            return;
        }

        try {
            slackNotificationClient.send(
                slackWebhookUrl, slackNotificationHandler.getText(notificationHandlerContext));
        } catch (RuntimeException exception) {
            log.error("Failed to deliver Slack notification {}", notification.getId(), exception);
        }
    }
}
