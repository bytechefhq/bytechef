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

package com.bytechef.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.notification.config.PlatformNotificationIntTestConfiguration;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Matija Petanjek
 */
@SpringBootTest(
    classes = PlatformNotificationIntTestConfiguration.class,
    properties = {
        "bytechef.webhook-delivery.concurrency-limit=1", "bytechef.webhook-delivery.max-pending-deliveries=1"
    })
public class NotificationServiceIntTest {

    @MockitoBean
    private MailService mailService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    public void testSecretSettingsAreStoredEncrypted() {
        Notification notification = new Notification();

        notification.setName("Secret Notification");
        notification.setType(Notification.Type.WEBHOOK);
        notification.setSettings(Map.of("webhook", "https://example.com/hook"));
        notification.setSecretSettings(Map.of("webhookSecret", "whsec_abc"));
        notification.setNotificationEventIds(List.of(1L));

        notification = notificationService.create(notification);

        Notification reloaded = notificationRepository.findById(notification.getId())
            .orElseThrow();

        assertThat(reloaded.getSecretSettings()).containsEntry("webhookSecret", "whsec_abc");
        assertThat(reloaded.getSetting("webhookSecret")).isEqualTo("whsec_abc");

        String rawSecretSettings = jdbcTemplate.queryForObject(
            "SELECT secret_settings FROM notification WHERE id = ?", String.class, notification.getId());

        assertThat(rawSecretSettings).isNotNull()
            .doesNotContain("whsec_abc");
    }

    @Test
    public void testLegacyRowWithoutSecretSettingsLoads() {
        jdbcTemplate.update(
            "INSERT INTO notification (name, type, settings, created_by, created_date, last_modified_by, " +
                "last_modified_date, version, secret_settings) VALUES (?, ?, ?, ?, now(), ?, now(), 0, NULL)",
            "Legacy", Notification.Type.SLACK.ordinal(), "{\"slackWebhookUrl\":\"https://hooks.slack.com/x\"}",
            "system", "system");

        Notification legacy = notificationRepository.findAll()
            .iterator()
            .next();

        assertThat(legacy.getSecretSettings()).isEmpty();
        assertThat(legacy.getSetting("slackWebhookUrl")).isEqualTo("https://hooks.slack.com/x");
        assertThat(legacy.getSetting("missing")).isNull();
    }
}
