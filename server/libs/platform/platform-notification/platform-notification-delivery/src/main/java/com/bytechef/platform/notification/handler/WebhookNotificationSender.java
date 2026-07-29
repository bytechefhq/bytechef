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

import com.bytechef.platform.notification.delivery.WebhookDeliveryRequest;
import com.bytechef.platform.notification.delivery.WebhookNotificationClient;
import com.bytechef.platform.notification.domain.Notification;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Delivers WEBHOOK-channel notifications through the shared {@link WebhookNotificationClient} — the same transport the
 * EE AI-observability alert channels use. Settings: {@code webhook} (destination URL, required) and
 * {@code webhookSecret} (optional HMAC secret; when set, deliveries carry an {@code X-ByteChef-Signature} header).
 *
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@Component
public class WebhookNotificationSender implements NotificationSender<WebhookNotificationHandler> {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotificationSender.class);

    private final WebhookNotificationClient webhookNotificationClient;

    public WebhookNotificationSender(WebhookNotificationClient webhookNotificationClient) {
        this.webhookNotificationClient = webhookNotificationClient;
    }

    @Override
    public Notification.Type getType() {
        return Notification.Type.WEBHOOK;
    }

    @Async
    @Override
    public void send(
        Notification notification, WebhookNotificationHandler webhookNotificationHandler,
        NotificationHandlerContext notificationHandlerContext) {

        Map<String, Object> settings = notification.getSettings();

        String url = (String) settings.get("webhook");

        if (url == null || url.isBlank()) {
            log.warn("Notification {} has no webhook URL configured; skipping delivery", notification.getId());

            return;
        }

        String secret = (String) settings.get("webhookSecret");

        try {
            webhookNotificationClient.deliver(
                new WebhookDeliveryRequest(
                    url, "job.status", webhookNotificationHandler.getPayload(notificationHandlerContext), Map.of(),
                    secret));
        } catch (RuntimeException exception) {
            log.error("Failed to deliver webhook notification {}", notification.getId(), exception);
        }
    }
}
