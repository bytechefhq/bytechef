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

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.notification.delivery.WebhookDelivery;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.delivery.WebhookRetry;
import com.bytechef.platform.notification.domain.Notification;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class WebhookNotificationSender implements NotificationSender<WebhookNotificationHandler> {

    private final WebhookDeliveryService webhookDeliveryService;

    @SuppressFBWarnings("EI2")
    public WebhookNotificationSender(WebhookDeliveryService webhookDeliveryService) {
        this.webhookDeliveryService = webhookDeliveryService;
    }

    @Override
    public Notification.Type getType() {
        return Notification.Type.WEBHOOK;
    }

    @Override
    public CompletableFuture<Void> send(
        Notification notification, WebhookNotificationHandler webhookNotificationHandler,
        NotificationHandlerContext notificationHandlerContext) {

        Map<String, Object> settings = notification.getSettings();

        String url = (String) settings.get("webhook");

        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                "Notification " + notification.getId() + " has no webhook URL configured");
        }

        WebhookDelivery webhookDelivery = WebhookDelivery.builder("notification", url)
            .description("notification " + notification.getId() + " '" + notification.getName() + "'")
            .eventType("job.status")
            .secret((String) settings.get("webhookSecret"))
            .validateUrl(true)
            .payload(
                String.valueOf(notificationHandlerContext.getJobId()),
                webhookNotificationHandler.getPayload(notificationHandlerContext))
            .retry(WebhookRetry.none())
            .deliveryId(notificationHandlerContext.getDeliveryId())
            .timestamp(notificationHandlerContext.getTimestamp())
            .build();

        return webhookDeliveryService.submit(webhookDelivery)
            .thenAccept(NotificationDeliveryOutcomes::requireAnySent);
    }
}
