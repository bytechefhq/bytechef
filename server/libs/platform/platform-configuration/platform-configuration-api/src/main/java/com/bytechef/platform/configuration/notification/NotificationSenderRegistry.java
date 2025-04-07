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

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.configuration.domain.Notification;
import java.util.List;
import java.util.Map;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class NotificationSenderRegistry<T extends NotificationHandler> {
    private final Map<Notification.Type, NotificationSender<T>> notificationSenderMap;

    public NotificationSenderRegistry(List<NotificationSender<T>> notificationSenders) {
        this.notificationSenderMap = MapUtils.toMap(
            notificationSenders, NotificationSender::getType, notificationSender -> notificationSender);
    }

    @NonNull
    public NotificationSender<T> getNotificationSender(Notification.Type type) {
        return notificationSenderMap.get(type);
    }
}
