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
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@Component
public class WebhookNotificationSender implements NotificationSender<WebhookNotificationHandler> {

    private final WebhookNotificationClient webhookNotificationClient;

    public WebhookNotificationSender(WebhookNotificationClient webhookNotificationClient) {
        this.webhookNotificationClient = webhookNotificationClient;
    }

    @Override
    public Notification.Type getType() {
        return Notification.Type.WEBHOOK;
    }

    @Override
    public void send(
        Notification notification, WebhookNotificationHandler webhookNotificationHandler,
        NotificationHandlerContext notificationHandlerContext) {

        Map<String, Object> settings = notification.getSettings();

        String url = (String) settings.get("webhook");

        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                "Notification " + notification.getId() + " has no webhook URL configured");
        }

        String secret = (String) settings.get("webhookSecret");

        webhookNotificationClient.deliver(
            new WebhookDeliveryRequest(
                url, "job.status", webhookNotificationHandler.getPayload(notificationHandlerContext), Map.of(),
                secret));
    }
}
