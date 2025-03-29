/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.configuration.notification;

import com.bytechef.platform.configuration.domain.notification.Event;
import com.bytechef.platform.configuration.domain.notification.Notification;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Matija Petanjek
 */
@Component
public class NotificationHandlerRegistry {
    private final Map<Event.Type, EmailNotificationHandler> emailNotificationHanlderMap = new HashMap<>();
    private final Map<Event.Type, WebhookNotificationHandler> webhookNotificationHandlerMap = new HashMap<>();

    public NotificationHandlerRegistry(List<NotificationHandler> notificationHandlers) {
        for (NotificationHandler notificationHandler : notificationHandlers) {
            NotificationEvent notificationEvent = notificationHandler.getClass()
                .getAnnotation(NotificationEvent.class);

            Assert.notNull(
                notificationEvent,
                "Notification handler " + notificationHandler.getClass()
                    .getName() +
                    "is missing NotificationEvent annotation");

            if (notificationHandler instanceof EmailNotificationHandler emailNotificationHandler) {
                for (Event.Type eventType : notificationEvent.value()) {
                    emailNotificationHanlderMap.put(eventType, emailNotificationHandler);
                }
            } else if (notificationHandler instanceof WebhookNotificationHandler webhookNotificationHandler) {
                for (Event.Type eventType : notificationEvent.value()) {
                    webhookNotificationHandlerMap.put(eventType, webhookNotificationHandler);
                }
            }
        }
    }

    public NotificationHandler getNotificationHandler(Event.Type eventType, Notification.Type notificationType) {
        if (notificationType == Notification.Type.EMAIL) {
            return emailNotificationHanlderMap.get(eventType);
        }

        return webhookNotificationHandlerMap.get(eventType);
    }

}
