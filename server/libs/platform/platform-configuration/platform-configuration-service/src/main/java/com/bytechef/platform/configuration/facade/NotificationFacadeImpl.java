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

package com.bytechef.platform.configuration.facade;

import com.bytechef.platform.configuration.dto.NotificationDTO;
import com.bytechef.platform.configuration.service.NotificationEventService;
import com.bytechef.platform.configuration.service.NotificationService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class NotificationFacadeImpl implements NotificationFacade {

    private final NotificationEventService notificationEventService;
    private final NotificationService notificationService;

    public NotificationFacadeImpl(
        NotificationEventService notificationEventService, NotificationService notificationService) {

        this.notificationEventService = notificationEventService;
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
}
