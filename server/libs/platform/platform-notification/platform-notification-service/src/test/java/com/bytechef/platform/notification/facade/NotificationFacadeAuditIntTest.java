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
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.notification.audit.NotificationAuditEvents;
import com.bytechef.platform.notification.config.PlatformNotificationIntTestConfiguration;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.dto.NotificationDTO;
import com.bytechef.platform.notification.repository.NotificationRepository;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        PlatformNotificationIntTestConfiguration.class, NotificationFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.audit.enabled=true", "bytechef.webhook-delivery.concurrency-limit=10",
        "bytechef.webhook-delivery.max-pending-deliveries=1000"
    })
class NotificationFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final String WEBHOOK_URL = "https://example.com/hook?token=confidential";

    @MockitoBean
    private MailService mailService;

    @Autowired
    private NotificationFacade notificationFacade;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @MockitoBean
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
            ADMIN_LOGIN, "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        notificationRepository.deleteAll();

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateRecordsTheNotificationWithoutItsSettings() {
        NotificationDTO notificationDTO = notificationFacade.createNotification(
            createNotification(Map.of("webhook", WEBHOOK_URL)));

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(NotificationAuditEvents.NOTIFICATION_CREATED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("notificationId", String.valueOf(notificationDTO.id()))
            .containsEntry("name", "My Notification")
            .containsEntry("type", "WEBHOOK")
            .doesNotContainValue(WEBHOOK_URL);
    }

    @Test
    void testRejectedCreateRecordsAnError() {
        assertThatThrownBy(() -> notificationFacade.createNotification(createNotification(Map.of())))
            .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(NotificationAuditEvents.NOTIFICATION_CREATED);

        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .doesNotContainKey("notificationId");
    }

    @Test
    void testUpdateAndDeleteRecordTheNotification() {
        NotificationDTO notificationDTO = notificationFacade.createNotification(
            createNotification(Map.of("webhook", WEBHOOK_URL)));

        Notification notification = createNotification(Map.of("webhook", WEBHOOK_URL));

        notification.setId(notificationDTO.id());
        notification.setName("Renamed Notification");
        notification.setVersion(notificationDTO.version());

        notificationFacade.updateNotification(notification);

        assertThat(findSingleEvent(NotificationAuditEvents.NOTIFICATION_UPDATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("notificationId", String.valueOf(notificationDTO.id()))
            .containsEntry("name", "Renamed Notification")
            .containsEntry("type", "WEBHOOK")
            .doesNotContainValue(WEBHOOK_URL);

        notificationFacade.deleteNotification(notificationDTO.id());

        assertThat(findSingleEvent(NotificationAuditEvents.NOTIFICATION_DELETED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("notificationId", String.valueOf(notificationDTO.id()));
    }

    private static Notification createNotification(Map<String, ?> settings) {
        Notification notification = new Notification();

        notification.setName("My Notification");
        notification.setNotificationEventIds(List.of(1L));
        notification.setSettings(settings);
        notification.setType(Notification.Type.WEBHOOK);

        return notification;
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> pageContent = page.getContent();

        List<PersistentAuditEvent> persistentAuditEvents = pageContent.stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties.class)
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class AuditConfiguration {
    }
}
