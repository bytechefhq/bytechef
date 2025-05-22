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

package com.bytechef.platform.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.configuration.domain.Notification;
import com.bytechef.platform.configuration.domain.NotificationEvent;
import com.bytechef.platform.configuration.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Matija Petanjek
 */
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void delete(long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Override
    public List<Notification> getNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    public List<Notification> getNotifications(NotificationEvent.Type eventType) {
        return notificationRepository.findAllByEventType(eventType.ordinal());
    }

    @Override
    public Notification create(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Override
    public Notification update(Notification notification) {
        Notification curNotification = OptionalUtils.get(notificationRepository.findById(notification.getId()));

        curNotification.setName(notification.getName());
        curNotification.setNotificationEventIds(notification.getNotificationEventIds());
        curNotification.setSettings(notification.getSettings());
        curNotification.setType(notification.getType());
        curNotification.setVersion(notification.getVersion());

        return notificationRepository.save(curNotification);
    }
}
