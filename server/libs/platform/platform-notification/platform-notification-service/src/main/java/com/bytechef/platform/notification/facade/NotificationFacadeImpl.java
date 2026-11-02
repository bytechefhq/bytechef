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

package com.bytechef.platform.notification.facade;

import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.dto.NotificationDTO;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationEventService;
import com.bytechef.platform.notification.service.NotificationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class NotificationFacadeImpl implements NotificationFacade {

    private final NotificationEventService notificationEventService;
    private final NotificationSenderRegistry notificationSenderRegistry;
    private final NotificationService notificationService;

    @SuppressFBWarnings("EI")
    public NotificationFacadeImpl(
        NotificationEventService notificationEventService, NotificationSenderRegistry notificationSenderRegistry,
        NotificationService notificationService) {

        this.notificationEventService = notificationEventService;
        this.notificationSenderRegistry = notificationSenderRegistry;
        this.notificationService = notificationService;
    }

    @Override
    public List<NotificationDTO> getNotifications() {
        return notificationService.getNotifications()
            .stream()
            .map(notification -> new NotificationDTO(
                notification, notificationEventService.getNotificationEvents(notification.getNotificationEventIds())))
            .toList();
    }

    @Override
    public NotificationDTO createNotification(Notification notification) {
        validateNotification(notification);

        notification = notificationService.create(notification);

        return new NotificationDTO(
            notification,
            notificationEventService.getNotificationEvents(notification.getNotificationEventIds()));
    }

    @Override
    public NotificationDTO updateNotification(Notification notification) {
        validateNotification(notification);

        notification = notificationService.update(notification);

        return new NotificationDTO(
            notification,
            notificationEventService.getNotificationEvents(notification.getNotificationEventIds()));
    }

    private static boolean isHttpUrl(String value) {
        try {
            URI uri = new URI(value.strip());

            String scheme = uri.getScheme();

            return uri.getHost() != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (URISyntaxException uriSyntaxException) {
            return false;
        }
    }

    private void validateNotification(Notification notification) {
        Notification.Type type = notification.getType();

        if (!notificationSenderRegistry.hasNotificationSender(type)) {
            throw new IllegalArgumentException(
                "Notification type %s is not available in this edition".formatted(type));
        }

        if (type == Notification.Type.WEBHOOK) {
            validateWebhookUrl(notification.getSettings());
        }
    }

    private static void validateWebhookUrl(Map<String, Object> settings) {
        Object webhookUrl = settings == null ? null : settings.get("webhook");

        if (!(webhookUrl instanceof String webhookUrlString) || webhookUrlString.isBlank()) {
            throw new IllegalArgumentException("Webhook URL is required");
        }

        if (!isHttpUrl(webhookUrlString)) {
            throw new IllegalArgumentException("Webhook URL must be an http or https URL");
        }
    }
}
