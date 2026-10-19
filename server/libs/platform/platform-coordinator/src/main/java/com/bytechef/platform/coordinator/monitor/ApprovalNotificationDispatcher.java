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

package com.bytechef.platform.coordinator.monitor;

import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationHandler;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSender;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
final class ApprovalNotificationDispatcher {

    enum Result {

        DELIVERED, FAILED, NONE_CONFIGURED
    }

    private static final Logger log = LoggerFactory.getLogger(ApprovalNotificationDispatcher.class);

    private final NotificationHandlerRegistry notificationHandlerRegistry;
    private final NotificationSenderRegistry notificationSenderRegistry;
    private final NotificationService notificationService;

    ApprovalNotificationDispatcher(
        NotificationHandlerRegistry notificationHandlerRegistry, NotificationSenderRegistry notificationSenderRegistry,
        NotificationService notificationService) {

        this.notificationHandlerRegistry = notificationHandlerRegistry;
        this.notificationSenderRegistry = notificationSenderRegistry;
        this.notificationService = notificationService;
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    Result dispatch(NotificationEvent.Type eventType, NotificationHandlerContext notificationHandlerContext) {
        List<Notification> notifications = notificationService.getNotifications(eventType);

        if (notifications.isEmpty()) {
            return Result.NONE_CONFIGURED;
        }

        int failedCount = 0;

        for (Notification notification : notifications) {
            NotificationSender notificationSender = notificationSenderRegistry.getNotificationSender(
                notification.getType());

            NotificationHandler notificationHandler = notificationHandlerRegistry.getNotificationHandler(
                eventType, notification.getType());

            if (notificationSender == null || notificationHandler == null) {
                failedCount++;

                log.warn(
                    "No {} found for notification {} of type {} and event type {}; counting it as failed",
                    notificationSender == null ? "sender" : "handler", notification.getId(), notification.getType(),
                    eventType);

                continue;
            }

            try {
                notificationSender.send(notification, notificationHandler, notificationHandlerContext);
            } catch (RuntimeException runtimeException) {
                failedCount++;

                log.warn(
                    "Could not send notification {} for event type {} of job {}", notification.getId(), eventType,
                    notificationHandlerContext.getJobId(), runtimeException);
            }
        }

        return failedCount < notifications.size() ? Result.DELIVERED : Result.FAILED;
    }
}
