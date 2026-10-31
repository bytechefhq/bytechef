/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class AuditEventServiceIntTest {

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @AfterEach
    void afterEach() {
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testSaveAllStoresEveryEventWithItsData() {
        LocalDateTime now = getNow();

        auditEventService.saveAll(
            List.of(
                createEvent("alice", "PERMISSION_CHECK", now, PlatformType.AUTOMATION, Map.of("target", "Project")),
                createEvent("bob", "CONNECTION_CREATED", now.minusMinutes(1), null, Map.of("connectionId", "9"))));

        Page<PersistentAuditEvent> page = auditEventService.fetchAuditEvents(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        assertThat(page.getContent())
            .extracting(PersistentAuditEvent::getPrincipal, PersistentAuditEvent::getEventType,
                PersistentAuditEvent::getPlatform, PersistentAuditEvent::getData)
            .containsExactly(
                tuple("alice", "PERMISSION_CHECK", PlatformType.AUTOMATION, Map.of("target", "Project")),
                tuple("bob", "CONNECTION_CREATED", null, Map.of("connectionId", "9")));
    }

    @Test
    void testFetchAuditEventsAppliesTheFilterAndThePage() {
        LocalDateTime now = getNow();

        auditEventService.saveAll(
            List.of(
                createEvent("alice", "PERMISSION_CHECK", now, null, Map.of("target", "ProjectFacade")),
                createEvent("alice", "PERMISSION_CHECK", now.minusMinutes(1), null, Map.of("target", "ProjectFacade")),
                createEvent("alice", "PERMISSION_CHECK", now.minusMinutes(2), null, Map.of("target", "ProjectFacade")),
                createEvent("bob", "PERMISSION_CHECK", now.minusDays(30), null, Map.of("target", "ProjectFacade")),
                createEvent("carol", "CONNECTION_CREATED", now, null, Map.of("target", "ProjectFacade"))));

        AuditEventFilter auditEventFilter = new AuditEventFilter(
            "PERMISSION_CHECK", now.minusDays(1), now.plusMinutes(1), "projectfacade", true, null);

        Page<PersistentAuditEvent> page = auditEventService.fetchAuditEvents(auditEventFilter, PageRequest.of(1, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent())
            .extracting(PersistentAuditEvent::getPrincipal, PersistentAuditEvent::getEventDate)
            .containsExactly(tuple("alice", now.minusMinutes(2)));
    }

    @Test
    void testFetchEventTypesReturnsEachStoredTypeOnce() {
        LocalDateTime now = getNow();

        auditEventService.saveAll(
            List.of(
                createEvent("alice", "PERMISSION_CHECK", now, null, Map.of()),
                createEvent("bob", "PERMISSION_CHECK", now, null, Map.of()),
                createEvent("carol", "CONNECTION_CREATED", now, null, Map.of())));

        assertThat(auditEventService.fetchEventTypes()).containsExactly("CONNECTION_CREATED", "PERMISSION_CHECK");
    }

    @Test
    void testUpdateJobCreatedPlatformCommitsThePlatformOfTheJobsCreatedEvent() {
        LocalDateTime now = getNow();

        auditEventService.saveAll(
            List.of(
                createEvent("SYSTEM", "JOB_CREATED", now, null, Map.of("jobId", "42")),
                createEvent("SYSTEM", "JOB_CREATED", now, null, Map.of("jobId", "43"))));

        auditEventService.updateJobCreatedPlatform(42L, PlatformType.EMBEDDED, now.minusDays(1));

        AuditEventFilter auditEventFilter = new AuditEventFilter(
            "JOB_CREATED", null, null, null, false, PlatformType.EMBEDDED);

        Page<PersistentAuditEvent> page = auditEventService.fetchAuditEvents(auditEventFilter, PageRequest.of(0, 25));

        assertThat(page.getContent())
            .singleElement()
            .satisfies(
                persistentAuditEvent -> assertThat(persistentAuditEvent.getData()).containsEntry("jobId", "42"));
    }

    private static LocalDateTime getNow() {
        LocalDateTime now = LocalDateTime.now();

        return now.truncatedTo(ChronoUnit.MICROS);
    }

    private static PersistentAuditEvent createEvent(
        String principal, String eventType, LocalDateTime eventDate, PlatformType platform, Map<String, String> data) {

        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setData(data);
        persistentAuditEvent.setEventDate(eventDate);
        persistentAuditEvent.setEventType(eventType);
        persistentAuditEvent.setPlatform(platform);
        persistentAuditEvent.setPrincipal(principal);

        return persistentAuditEvent;
    }
}
