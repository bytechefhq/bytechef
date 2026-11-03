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

package com.bytechef.platform.notification.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.notification.config.PlatformNotificationIntTestConfiguration;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.dto.NotificationDTO;
import com.bytechef.platform.notification.repository.NotificationRepository;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        PlatformNotificationIntTestConfiguration.class, NotificationFacadeIntTest.MethodSecurityConfiguration.class
    },
    properties = {
        "bytechef.webhook-delivery.concurrency-limit=10", "bytechef.webhook-delivery.max-pending-deliveries=1000"
    })
public class NotificationFacadeIntTest {

    @MockitoBean
    private MailService mailService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NotificationFacade notificationFacade;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockitoBean
    private WorkflowService workflowService;

    @BeforeEach
    public void beforeEach() {
        authenticateAs(AuthorityConstants.ADMIN);
    }

    @AfterEach
    public void afterEach() {
        SecurityContextHolder.clearContext();

        notificationRepository.deleteAll();
    }

    @Test
    public void testGetNotificationsRequiresAdmin() {
        authenticateAs(AuthorityConstants.USER);

        assertThatThrownBy(() -> notificationFacade.getNotifications())
            .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    public void testCreateNotificationRequiresAdmin() {
        authenticateAs(AuthorityConstants.USER);

        Notification notification = createNotification(Notification.Type.EMAIL, Map.of("email", "john@email.com"));

        assertThatThrownBy(() -> notificationFacade.createNotification(notification))
            .isInstanceOf(AuthorizationDeniedException.class);

        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    public void testUpdateNotificationRequiresAdmin() {
        Notification notification = createNotification(Notification.Type.EMAIL, Map.of("email", "john@email.com"));

        authenticateAs(AuthorityConstants.USER);

        assertThatThrownBy(() -> notificationFacade.updateNotification(notification))
            .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    public void testDeleteNotificationRequiresAdmin() {
        authenticateAs(AuthorityConstants.USER);

        assertThatThrownBy(() -> notificationFacade.deleteNotification(1L))
            .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    public void testGetNotificationEventsRequiresAdmin() {
        authenticateAs(AuthorityConstants.USER);

        assertThatThrownBy(() -> notificationFacade.getNotificationEvents())
            .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    public void testDeleteNotificationAsAdminDeletes() {
        NotificationDTO notificationDTO = notificationFacade.createNotification(
            createNotification(Notification.Type.EMAIL, Map.of("email", "john@email.com")));

        notificationFacade.deleteNotification(notificationDTO.id());

        assertThat(notificationFacade.getNotifications()).isEmpty();
    }

    @Test
    public void testCreateEmailNotification() {
        NotificationDTO notificationDTO = notificationFacade.createNotification(
            createNotification(Notification.Type.EMAIL, Map.of("email", "john@email.com")));

        assertThat(notificationDTO.type()).isEqualTo(Notification.Type.EMAIL);
        assertThat(notificationRepository.count()).isEqualTo(1);
    }

    @Test
    public void testCreateWebhookNotificationIsRejectedInCommunityEdition() {
        Notification notification = createNotification(
            Notification.Type.WEBHOOK, Map.of("webhook", "https://example.com/hook"));

        assertThatThrownBy(() -> notificationFacade.createNotification(notification))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Notification type WEBHOOK is not available in this edition");

        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    public void testCreateSlackNotification() {
        NotificationDTO notificationDTO = notificationFacade.createNotification(
            createNotification(
                Notification.Type.SLACK, Map.of("slackWebhookUrl", "https://hooks.slack.com/services/T/B/X")));

        assertThat(notificationDTO.type()).isEqualTo(Notification.Type.SLACK);
        assertThat(notificationRepository.count()).isEqualTo(1);
    }

    @Test
    public void testUpdateToWebhookNotificationIsRejectedInCommunityEdition() {
        NotificationDTO notificationDTO = notificationFacade.createNotification(
            createNotification(Notification.Type.EMAIL, Map.of("email", "john@email.com")));

        Notification notification = createNotification(
            Notification.Type.WEBHOOK, Map.of("webhook", "https://example.com/hook"));

        notification.setId(notificationDTO.id());

        assertThatThrownBy(() -> notificationFacade.updateNotification(notification))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Notification type WEBHOOK is not available in this edition");

        Notification storedNotification = notificationRepository.findById(notificationDTO.id())
            .orElseThrow();

        assertThat(storedNotification.getType()).isEqualTo(Notification.Type.EMAIL);
    }

    @Test
    public void testRegenerateSigningSecretRequiresAdmin() {
        authenticateAs(AuthorityConstants.USER);

        assertThatThrownBy(() -> notificationFacade.regenerateSigningSecret(1L))
            .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    public void testLegacySlackRowMovesUrlToSecretSettingsOnUpdate() {
        String slackUrl = "https://hooks.slack.com/services/T/B/LEGACY";

        jdbcTemplate.update(
            "INSERT INTO notification (name, type, settings, created_by, created_date, last_modified_by, " +
                "last_modified_date, version) VALUES ('Legacy', 2, ?, 'system', now(), 'system', now(), 1)",
            "{\"slackWebhookUrl\":\"" + slackUrl + "\"}");

        Long id = jdbcTemplate.queryForObject("SELECT id FROM notification WHERE name = 'Legacy'", Long.class);

        NotificationDTO listedNotificationDTO = notificationFacade.getNotifications()
            .getFirst();

        assertThat(listedNotificationDTO.settings()).containsEntry("slackWebhookUrl", "********");
        assertThat(listedNotificationDTO.settings()
            .toString()).doesNotContain(slackUrl);

        Notification update = createNotification(
            Notification.Type.SLACK, Map.of("slackWebhookUrl", "********"));

        update.setId(id);
        update.setName("Renamed");
        update.setVersion(1);

        notificationFacade.updateNotification(update);

        String rawSettings = jdbcTemplate.queryForObject(
            "SELECT settings FROM notification WHERE id = ?", String.class, id);

        assertThat(rawSettings).doesNotContain(slackUrl);

        Notification storedNotification = notificationRepository.findById(id)
            .orElseThrow();

        assertThat(storedNotification.getSetting("slackWebhookUrl")).isEqualTo(slackUrl);
    }

    @Test
    public void testGetMasksSlackUrl() {
        String slackUrl = "https://hooks.slack.com/services/T/B/X";

        notificationFacade.createNotification(
            createNotification(Notification.Type.SLACK, Map.of("slackWebhookUrl", slackUrl)));

        NotificationDTO notificationDTO = notificationFacade.getNotifications()
            .getFirst();

        assertThat(notificationDTO.settings()).containsEntry("slackWebhookUrl", "********");
        assertThat(notificationDTO.signingSecret()).isNull();
    }

    private static void authenticateAs(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    "test-" + authority, null, List.of(new SimpleGrantedAuthority(authority))));
    }

    private static Notification createNotification(Notification.Type type, Map<String, ?> settings) {
        Notification notification = new Notification();

        notification.setName("My Notification");
        notification.setNotificationEventIds(List.of(1L));
        notification.setSettings(settings);
        notification.setType(type);

        return notification;
    }

    @Nested
    @TestPropertySource(properties = "bytechef.edition=ee")
    class EnterpriseEditionIntTest {

        @Test
        public void testCreateWebhookNotification() {
            NotificationDTO notificationDTO = notificationFacade.createNotification(
                createNotification(Notification.Type.WEBHOOK, Map.of("webhook", "https://example.com/hook")));

            assertThat(notificationDTO.type()).isEqualTo(Notification.Type.WEBHOOK);
            assertThat(notificationRepository.count()).isEqualTo(1);
        }

        @Test
        public void testCreateWebhookReturnsSigningSecretOnce() {
            NotificationDTO createdNotificationDTO = notificationFacade.createNotification(
                createNotification(Notification.Type.WEBHOOK, Map.of("webhook", "https://example.com/hook")));

            assertThat(createdNotificationDTO.signingSecret()).startsWith("whsec_");
            assertThat(createdNotificationDTO.hasSigningSecret()).isTrue();
            assertThat(createdNotificationDTO.settings()).doesNotContainKey("webhookSecret");

            NotificationDTO listedNotificationDTO = notificationFacade.getNotifications()
                .getFirst();

            assertThat(listedNotificationDTO.signingSecret()).isNull();
            assertThat(listedNotificationDTO.hasSigningSecret()).isTrue();
            assertThat(listedNotificationDTO.settings()).doesNotContainKey("webhookSecret");
        }

        @Test
        public void testRegenerateSigningSecretReplacesIt() {
            NotificationDTO createdNotificationDTO = notificationFacade.createNotification(
                createNotification(Notification.Type.WEBHOOK, Map.of("webhook", "https://example.com/hook")));

            String regeneratedSecret = notificationFacade.regenerateSigningSecret(createdNotificationDTO.id());

            assertThat(regeneratedSecret).startsWith("whsec_")
                .isNotEqualTo(createdNotificationDTO.signingSecret());

            Notification storedNotification = notificationRepository.findById(createdNotificationDTO.id())
                .orElseThrow();

            assertThat(storedNotification.getSetting("webhookSecret")).isEqualTo(regeneratedSecret);
        }

        @Test
        public void testRegenerateSigningSecretRejectsNonWebhook() {
            NotificationDTO createdNotificationDTO = notificationFacade.createNotification(
                createNotification(Notification.Type.EMAIL, Map.of("email", "john@email.com")));

            assertThatThrownBy(() -> notificationFacade.regenerateSigningSecret(createdNotificationDTO.id()))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        public void testUpdateWithMaskedValuesKeepsSecrets() {
            NotificationDTO createdNotificationDTO = notificationFacade.createNotification(
                createNotification(
                    Notification.Type.WEBHOOK,
                    Map.of(
                        "webhook", "https://example.com/hook", "webhookHeaders",
                        List.of(Map.of("name", "Authorization", "value", "token-1")))));

            Notification update = createNotification(
                Notification.Type.WEBHOOK,
                Map.of(
                    "webhook", "https://example.com/hook", "webhookHeaders",
                    List.of(Map.of("name", "Authorization", "value", "********"))));

            update.setId(createdNotificationDTO.id());
            update.setName("Renamed");
            update.setVersion(createdNotificationDTO.version());

            NotificationDTO updatedNotificationDTO = notificationFacade.updateNotification(update);

            assertThat(updatedNotificationDTO.name()).isEqualTo("Renamed");
            assertThat(updatedNotificationDTO.signingSecret()).isNull();
            assertThat(updatedNotificationDTO.hasSigningSecret()).isTrue();

            Notification storedNotification = notificationRepository.findById(createdNotificationDTO.id())
                .orElseThrow();

            assertThat(storedNotification.getSetting("webhookSecret"))
                .isEqualTo(createdNotificationDTO.signingSecret());
            assertThat(storedNotification.getSetting("webhookHeaders"))
                .isEqualTo(List.of(Map.of("name", "Authorization", "value", "token-1")));
        }

        @Test
        public void testRejectsTemplateThatIsNotValidJson() {
            Notification notification = createNotification(
                Notification.Type.WEBHOOK,
                Map.of("webhook", "https://example.com/hook", "webhookBodyTemplate", "{\"a\": \"{{jobId}}\""));

            assertThatThrownBy(() -> notificationFacade.createNotification(notification))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid JSON");

            assertThat(notificationRepository.count()).isZero();
        }

        @Test
        public void testAcceptsTemplateWithSummary() {
            NotificationDTO notificationDTO = notificationFacade.createNotification(
                createNotification(
                    Notification.Type.WEBHOOK,
                    Map.of("webhook", "https://example.com/hook", "webhookBodyTemplate", "{\"s\": {{summary}}}")));

            assertThat(notificationDTO.settings()).containsEntry("webhookBodyTemplate", "{\"s\": {{summary}}}");
        }

        @Test
        public void testRejectsReservedHeaderName() {
            assertRejected(List.of(Map.of("name", "X-ByteChef-Event", "value", "v")));
            assertRejected(List.of(Map.of("name", "content-type", "value", "v")));
        }

        @Test
        public void testRejectsMoreThan20Headers() {
            List<Map<String, Object>> headers = new ArrayList<>();

            for (int headerIndex = 0; headerIndex < 21; headerIndex++) {
                headers.add(Map.of("name", "X-Header-" + headerIndex, "value", "v"));
            }

            assertRejected(headers);
        }

        @Test
        public void testRejectsInvalidHeaderName() {
            assertRejected(List.of(Map.of("name", "Bad Header", "value", "v")));
        }

        @Test
        public void testRejectsDuplicateHeaderNamesIgnoringCase() {
            assertRejected(List.of(Map.of("name", "X-Dup", "value", "a"), Map.of("name", "x-dup", "value", "b")));
        }

        private void assertRejected(List<Map<String, Object>> headers) {
            Notification notification = createNotification(
                Notification.Type.WEBHOOK, Map.of("webhook", "https://example.com/hook", "webhookHeaders", headers));

            assertThatThrownBy(() -> notificationFacade.createNotification(notification))
                .isInstanceOf(IllegalArgumentException.class);

            assertThat(notificationRepository.count()).isZero();
        }

        @Test
        public void testCreateWebhookNotificationWithoutUrlIsRejected() {
            Notification notification = createNotification(Notification.Type.WEBHOOK, Map.of());

            assertThatThrownBy(() -> notificationFacade.createNotification(notification))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Webhook URL is required");

            assertThat(notificationRepository.count()).isZero();
        }

        @Test
        public void testCreateWebhookNotificationWithBlankUrlIsRejected() {
            Notification notification = createNotification(Notification.Type.WEBHOOK, Map.of("webhook", "  "));

            assertThatThrownBy(() -> notificationFacade.createNotification(notification))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Webhook URL is required");

            assertThat(notificationRepository.count()).isZero();
        }

        @Test
        public void testCreateWebhookNotificationWithNonHttpUrlIsRejected() {
            Notification notification = createNotification(
                Notification.Type.WEBHOOK, Map.of("webhook", "ftp://example.com/hook"));

            assertThatThrownBy(() -> notificationFacade.createNotification(notification))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Webhook URL must be an http or https URL");

            assertThat(notificationRepository.count()).isZero();
        }

        @Test
        public void testUpdateWebhookNotificationToEmptyUrlIsRejected() {
            NotificationDTO notificationDTO = notificationFacade.createNotification(
                createNotification(Notification.Type.WEBHOOK, Map.of("webhook", "https://example.com/hook")));

            Notification notification = createNotification(Notification.Type.WEBHOOK, Map.of("webhook", ""));

            notification.setId(notificationDTO.id());

            assertThatThrownBy(() -> notificationFacade.updateNotification(notification))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Webhook URL is required");

            Notification storedNotification = notificationRepository.findById(notificationDTO.id())
                .orElseThrow();

            assertThat(storedNotification.getSettings()).containsEntry("webhook", "https://example.com/hook");
        }
    }

    @EnableMethodSecurity
    @TestConfiguration
    static class MethodSecurityConfiguration {
    }
}
