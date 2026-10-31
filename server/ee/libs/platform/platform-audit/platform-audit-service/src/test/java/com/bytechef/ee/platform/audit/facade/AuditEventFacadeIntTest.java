/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    AuditEventFacadeIntTest.MethodSecurityConfiguration.class, PostgreSQLContainerConfiguration.class
})
class AuditEventFacadeIntTest {

    @Autowired
    private AuditEventFacade auditEventFacade;

    @MockitoSpyBean
    private AuditEventService auditEventService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testFetchEventTypesIsDeniedForNonAdmin() {
        authenticate("bob", "ROLE_USER");

        assertThatThrownBy(() -> auditEventFacade.fetchEventTypes())
            .isInstanceOf(AccessDeniedException.class);

        verify(auditEventService, never()).fetchEventTypes();
    }

    @Test
    void testFetchAuditEventsIsDeniedForNonAdmin() {
        authenticate("bob", "ROLE_USER");

        assertThatThrownBy(() -> auditEventFacade.fetchAuditEvents(AuditEventFilter.empty(), PageRequest.of(0, 25)))
            .isInstanceOf(AccessDeniedException.class);

        verify(auditEventService, never()).fetchAuditEvents(any(), any());
    }

    @Test
    void testFetchEventTypesIsAllowedForAdmin() {
        saveEvent("CONNECTION_CREATED");
        saveEvent("PERMISSION_CHECK");

        authenticate("alice", "ROLE_ADMIN");

        assertThat(auditEventFacade.fetchEventTypes()).containsExactlyInAnyOrder(
            "CONNECTION_CREATED", "PERMISSION_CHECK");
    }

    @Test
    void testFetchAuditEventsIsAllowedForAdmin() {
        saveEvent("CONNECTION_CREATED");

        authenticate("alice", "ROLE_ADMIN");

        Page<PersistentAuditEvent> page = auditEventFacade.fetchAuditEvents(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        assertThat(page.getContent())
            .singleElement()
            .satisfies(persistentAuditEvent -> {
                assertThat(persistentAuditEvent.getEventType()).isEqualTo("CONNECTION_CREATED");
                assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("carol");
                assertThat(persistentAuditEvent.getData()).containsEntry("connectionId", "9");
            });
    }

    @Test
    void testReadingAuditEventsDoesNotAppendToTheAuditLog() {
        saveEvent("PERMISSION_CHECK");

        authenticate("alice", "ROLE_ADMIN");

        assertThat(auditEventFacade.fetchEventTypes()).containsExactly("PERMISSION_CHECK");

        assertThat(persistenceAuditEventRepository.count()).isEqualTo(1);
    }

    @Test
    void testDeniedReadOfTheAuditLogIsRecorded() {
        authenticate("bob", "ROLE_USER");

        assertThatThrownBy(() -> auditEventFacade.fetchEventTypes())
            .isInstanceOf(AccessDeniedException.class);

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        assertThat(page.getContent())
            .singleElement()
            .satisfies(persistentAuditEvent -> {
                assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("bob");
                assertThat(persistentAuditEvent.getData())
                    .containsEntry("result", "DENIED")
                    .containsEntry("method", AuditEventFacadeImpl.class.getName() + ".fetchEventTypes");
            });
    }

    private void authenticate(String principal, String authority) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private void saveEvent(String eventType) {
        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setData(Map.of("connectionId", "9"));
        persistentAuditEvent.setEventDate(LocalDateTime.now());
        persistentAuditEvent.setEventType(eventType);
        persistentAuditEvent.setPlatform(PlatformType.AUTOMATION);
        persistentAuditEvent.setPrincipal("carol");

        persistenceAuditEventRepository.save(persistentAuditEvent);
    }

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
}
