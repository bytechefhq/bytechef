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

import com.bytechef.platform.configuration.domain.NotificationEvent;
import com.bytechef.platform.configuration.repository.NotificationEventRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * @author Matija Petanjek
 */
@Service
public class NotificationEventServiceImpl implements NotificationEventService {

    private final NotificationEventRepository notificationEventRepository;

    public NotificationEventServiceImpl(NotificationEventRepository notificationEventRepository) {
        this.notificationEventRepository = notificationEventRepository;
    }

    @Override
    public List<NotificationEvent> getNotificationEvents() {
        return notificationEventRepository.findAll();
    }

    @Override
    public List<NotificationEvent> getNotificationEvents(List<Long> notificationEventIds) {
        return notificationEventRepository.findByIdIn(notificationEventIds);
    }
}
