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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfiguration;
import com.bytechef.platform.configuration.domain.Notification;
import com.bytechef.platform.configuration.domain.NotificationEvent;
import com.bytechef.platform.configuration.repository.NotificationRepository;
import com.bytechef.platform.mail.MailService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Matija Petanjek
 */
@SpringBootTest(classes = PlatformConfigurationIntTestConfiguration.class)
public class NotificationServiceIntTest {

    @MockitoBean
    private MailService mailService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockitoBean
    private WorkflowService workflowService;

    @AfterEach
    public void afterEach() {
        notificationRepository.deleteAll();
    }

    @Test
    public void testCreateNotification() {
        Notification notification = new Notification();

        notification.setName("My Notification");
        notification.setType(Notification.Type.EMAIL);
        notification.setSettings(Map.of("to", "john@email.com"));
        notification.setNotificationEventIds(List.of(1L));

        notification = notificationService.create(notification);

        assertThat(notification)
            .hasFieldOrPropertyWithValue("name", "My Notification")
            .hasFieldOrPropertyWithValue("type", Notification.Type.EMAIL)
            .hasFieldOrPropertyWithValue("settings", Map.of("to", "john@email.com"));
        assertThat(notification.getNotificationEventIds()).containsExactlyInAnyOrder(1L);

        List<Notification> notifications = notificationService.getNotifications(NotificationEvent.Type.JOB_CANCELLED);

        assertThat(notifications).hasSize(1)
            .contains(notification);
    }
}
