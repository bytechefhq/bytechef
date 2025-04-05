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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.config.NotificationIntTestConfiguration;
import com.bytechef.platform.configuration.domain.Event;
import com.bytechef.platform.configuration.domain.Notification;
import com.bytechef.platform.configuration.repository.EventRepository;
import com.bytechef.platform.configuration.repository.NotificationRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Matija Petanjek
 */
@SpringBootTest(classes = NotificationIntTestConfiguration.class)
public class NotificationServiceIntTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @AfterEach
    public void afterEach() {
        notificationRepository.deleteAll();
    }

    @Test
    public void testCreateNotification() {
        Notification notification = notificationService.create(
            "My Notification", Notification.Type.EMAIL, Map.of("to", "john@email.com"), List.of(1L));

        assertThat(notification)
            .hasFieldOrPropertyWithValue("name", "My Notification")
            .hasFieldOrPropertyWithValue("type", Notification.Type.EMAIL)
            .hasFieldOrPropertyWithValue("settings", Map.of("to", "john@email.com"));
        assertThat(notification.getEventIds()).containsExactlyInAnyOrder(1L);

        List<Notification> notifications = notificationService.getNotifications(Event.Type.JOB_CANCELLED);

        assertThat(notifications).hasSize(1)
            .contains(notification);
    }

}
