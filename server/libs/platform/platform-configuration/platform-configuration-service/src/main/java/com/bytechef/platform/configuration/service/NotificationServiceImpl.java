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

package com.bytechef.platform.configuration.service;

import com.bytechef.platform.configuration.domain.notification.Event;
import com.bytechef.platform.configuration.domain.notification.Notification;
import com.bytechef.platform.configuration.repository.NotificationRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Matija Petanjek
 */
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private NotificationRepository notificationRepository;

    public NotificationServiceImpl(
        NotificationRepository notificationRepository) {

        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Notification> findAll() {
        return notificationRepository.findAll();
    }

    @Override
    public List<Notification> fetchNotifications(Event.Type eventType) {
        return notificationRepository.findAllByEventType(eventType.toString());
    }

    @Override
    public Notification create(
        String name, Notification.Type notificationType, Map<String, String> settings, List<Long> eventIds) {
        Notification notification = new Notification();

        notification.setName(name);
        notification.setType(notificationType);
        notification.setSettings(settings);
        notification.setEventIds(eventIds);

        return notificationRepository.save(notification);
    }
}
