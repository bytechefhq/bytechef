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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
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
