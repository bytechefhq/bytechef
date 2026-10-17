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

package com.bytechef.platform.coordinator.config;

import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationSender;
import com.bytechef.platform.notification.handler.WebhookNotificationHandler;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Ivica Cardic
 */
public class RecordingWebhookNotificationSender implements NotificationSender<WebhookNotificationHandler> {

    public static final String FAILING_NOTIFICATION_NAME = "failing";

    private final List<String> attemptedNotificationNames = new CopyOnWriteArrayList<>();
    private final List<NotificationHandlerContext> deliveredNotificationHandlerContexts =
        new CopyOnWriteArrayList<>();

    @Override
    public Notification.Type getType() {
        return Notification.Type.WEBHOOK;
    }

    @Override
    public void send(
        Notification notification, WebhookNotificationHandler notificationHandler,
        NotificationHandlerContext notificationHandlerContext) {

        String name = notification.getName();

        attemptedNotificationNames.add(name);

        if (FAILING_NOTIFICATION_NAME.equals(name)) {
            throw new IllegalStateException("webhook unreachable");
        }

        notificationHandler.getPayload(notificationHandlerContext);

        deliveredNotificationHandlerContexts.add(notificationHandlerContext);
    }

    public List<String> getAttemptedNotificationNames() {
        return List.copyOf(attemptedNotificationNames);
    }

    public List<NotificationHandlerContext> getDeliveredNotificationHandlerContexts() {
        return List.copyOf(deliveredNotificationHandlerContexts);
    }

    public void reset() {
        attemptedNotificationNames.clear();
        deliveredNotificationHandlerContexts.clear();
    }
}
