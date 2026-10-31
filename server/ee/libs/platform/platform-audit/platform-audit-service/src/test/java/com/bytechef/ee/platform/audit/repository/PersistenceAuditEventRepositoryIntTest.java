/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class PersistenceAuditEventRepositoryIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @AfterEach
    public void afterEach() {
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    public void testUpgradeCreatesTheEventDatePrincipalAndEventTypeIndexes() {
        Map<String, String> indexDefinitions = new HashMap<>();

        jdbcTemplate.query(
            "SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'persistent_audit_event'",
            resultSet -> {
                indexDefinitions.put(resultSet.getString("indexname"), resultSet.getString("indexdef"));
            });

        assertThat(indexDefinitions)
            .hasEntrySatisfying(
                "idx_persistent_audit_event_event_date_id",
                indexDefinition -> assertThat(indexDefinition).contains("(event_date DESC, id DESC)"))
            .hasEntrySatisfying(
                "idx_persistent_audit_event_principal_event_date",
                indexDefinition -> assertThat(indexDefinition).contains("(principal, event_date DESC)"))
            .hasEntrySatisfying(
                "idx_persistent_audit_event_event_type_event_date",
                indexDefinition -> assertThat(indexDefinition).contains("(event_type, event_date DESC)"))
            .hasEntrySatisfying(
                "idx_persistent_audit_event_platform_event_date",
                indexDefinition -> assertThat(indexDefinition).contains("(platform, event_date DESC)"));
    }

    @Test
    public void testPlatformIsStoredAndReadBack() {
        LocalDateTime now = LocalDateTime.now();

        save("alice", "PERMISSION_CHECK", now, PlatformType.EMBEDDED);
        save("bob", "PERMISSION_CHECK", now.minusMinutes(1), (PlatformType) null);

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        assertThat(page.getContent())
            .extracting(PersistentAuditEvent::getPrincipal, PersistentAuditEvent::getPlatform)
            .containsExactly(
                tuple("alice", PlatformType.EMBEDDED),
                tuple("bob", null));
    }

    @Test
    public void testFindAllFilteredByPlatformExcludesOtherAndNullPlatforms() {
        LocalDateTime now = LocalDateTime.now();

        save("alice", "PERMISSION_CHECK", now, PlatformType.AUTOMATION);
        save("bob", "PERMISSION_CHECK", now.minusMinutes(1), PlatformType.EMBEDDED);
        save("carol", "PERMISSION_CHECK", now.minusMinutes(2), (PlatformType) null);

        Page<PersistentAuditEvent> automationPage = persistenceAuditEventRepository.findAllFiltered(
            new AuditEventFilter(null, null, null, null, true, PlatformType.AUTOMATION), PageRequest.of(0, 25));

        assertThat(automationPage.getTotalElements()).isEqualTo(1);
        assertThat(automationPage.getContent())
            .extracting(PersistentAuditEvent::getPrincipal)
            .containsExactly("alice");

        Page<PersistentAuditEvent> embeddedPage = persistenceAuditEventRepository.findAllFiltered(
            new AuditEventFilter(null, null, null, null, true, PlatformType.EMBEDDED), PageRequest.of(0, 25));

        assertThat(embeddedPage.getTotalElements()).isEqualTo(1);
        assertThat(embeddedPage.getContent())
            .extracting(PersistentAuditEvent::getPrincipal)
            .containsExactly("bob");

        Page<PersistentAuditEvent> unfilteredPage = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        assertThat(unfilteredPage.getTotalElements()).isEqualTo(3);
    }

    @Test
    public void testFindAllFilteredByPlatformCombinesWithTheOtherFilters() {
        LocalDateTime now = LocalDateTime.now();

        save("alice", "WORKSPACE_USER_ADDED", now, PlatformType.AUTOMATION, Map.of("note", "needle"));
        save("alice", "WORKSPACE_USER_ADDED", now.minusMinutes(1), PlatformType.EMBEDDED, Map.of("note", "needle"));
        save("alice", "PERMISSION_CHECK", now.minusMinutes(2), PlatformType.AUTOMATION, Map.of("note", "needle"));
        save("alice", "WORKSPACE_USER_ADDED", now.minusMinutes(3), PlatformType.AUTOMATION, Map.of("note", "hay"));
        save("alice", "WORKSPACE_USER_ADDED", now.minusDays(3), PlatformType.AUTOMATION, Map.of("note", "needle"));
        save("alice", "WORKSPACE_USER_ADDED", now.minusMinutes(4), null, Map.of("note", "needle"));

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            new AuditEventFilter(
                "WORKSPACE_USER_ADDED", now.minusDays(1), now, "needle", true, PlatformType.AUTOMATION),
            PageRequest.of(0, 25));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).singleElement()
            .satisfies(persistentAuditEvent -> {
                assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
                assertThat(persistentAuditEvent.getData()).containsEntry("note", "needle");
            });
    }

    @Test
    public void testUpdateJobCreatedPlatformFillsOnlyThatJobsRecentCreatedEventWithoutPlatform() {
        LocalDateTime now = LocalDateTime.now();

        save("SYSTEM", "JOB_CREATED", now, null, Map.of("jobId", "42"));
        save("SYSTEM", "JOB_CREATED", now, null, Map.of("jobId", "43"));
        save("SYSTEM", "JOB_STARTED", now, null, Map.of("jobId", "42"));
        save("SYSTEM", "JOB_CREATED", now.minusDays(2), null, Map.of("jobId", "42"));
        save("SYSTEM", "JOB_CREATED", now, PlatformType.AUTOMATION, Map.of("jobId", "42"));

        int updated = persistenceAuditEventRepository.updateJobCreatedPlatform(
            42L, PlatformType.EMBEDDED, now.minusDays(1));

        assertThat(updated).isEqualTo(1);

        List<PersistentAuditEvent> embeddedPersistentAuditEvents = persistenceAuditEventRepository.findAll()
            .stream()
            .filter(persistentAuditEvent -> persistentAuditEvent.getPlatform() == PlatformType.EMBEDDED)
            .toList();

        assertThat(embeddedPersistentAuditEvents).singleElement()
            .satisfies(persistentAuditEvent -> {
                assertThat(persistentAuditEvent.getEventType()).isEqualTo("JOB_CREATED");
                assertThat(persistentAuditEvent.getData()).containsEntry("jobId", "42");
                assertThat(persistentAuditEvent.getEventDate()).isAfter(now.minusDays(1));
            });
    }

    @Test
    public void testFindAllFilteredBySearchMatchesPrincipalCaseInsensitively() {
        save("alice@example.com", "PERMISSION_CHECK", LocalDateTime.now());
        save("bob", "PERMISSION_CHECK", LocalDateTime.now());

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, "ALICE@", true, null), PageRequest.of(0, 25));

        assertThat(page.getContent())
            .singleElement()
            .satisfies(event -> assertThat(event.getPrincipal()).isEqualTo("alice@example.com"));

        Page<PersistentAuditEvent> wildcardPage = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, "a_ice", true, null), PageRequest.of(0, 25));

        assertThat(wildcardPage.getContent()).isEmpty();
    }

    @Test
    public void testFindAllFilteredBySearchMatchesPrincipalOrDataValue() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("method", "com.bytechef.ProjectFacade.delete"));
        save("bob", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("userLogin", "alice"));
        save("carol", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("userLogin", "dave"));

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, "alice", true, null), PageRequest.of(0, 25));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
            .extracting(PersistentAuditEvent::getPrincipal)
            .containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    public void testFindAllFilteredBySearchSkipsPrincipalWhenPrincipalSearchIsOff() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("method", "com.bytechef.ProjectFacade.delete"));
        save("bob", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("userLogin", "alice"));

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, "alice", false, null), PageRequest.of(0, 25));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent())
            .singleElement()
            .satisfies(event -> assertThat(event.getPrincipal()).isEqualTo("bob"));
    }

    @Test
    public void testFindAllFilteredByEventType() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("alice", "CONNECTION_CREATED", LocalDateTime.now());

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter("CONNECTION_CREATED", null, null, null, true, null),
                PageRequest.of(0, 25));

        assertThat(page.getContent())
            .hasSize(1)
            .allSatisfy(event -> assertThat(event.getEventType()).isEqualTo("CONNECTION_CREATED"));
    }

    @Test
    public void testFindAllFilteredByDateRange() {
        LocalDateTime now = LocalDateTime.now();

        save("alice", "PERMISSION_CHECK", now.minusDays(10));
        save("alice", "PERMISSION_CHECK", now.minusDays(1));

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, now.minusDays(5), now, null, true, null),
                PageRequest.of(0, 25));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    public void testFindAllFilteredWithNoFiltersReturnsAll() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("bob", "CONNECTION_CREATED", LocalDateTime.now());

        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    public void testFindDistinctEventTypes() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("bob", "CONNECTION_CREATED", LocalDateTime.now());

        List<String> types = persistenceAuditEventRepository.findDistinctEventTypes();

        assertThat(types).containsExactly("CONNECTION_CREATED", "PERMISSION_CHECK");
    }

    @Test
    public void testDeleteByEventDateBefore() {
        LocalDateTime now = LocalDateTime.now();

        save("alice", "PERMISSION_CHECK", now.minusDays(400));
        save("alice", "PERMISSION_CHECK", now.minusDays(10));

        int deleted = persistenceAuditEventRepository.deleteByEventDateBefore(now.minusDays(365));

        assertThat(deleted).isEqualTo(1);

        Page<PersistentAuditEvent> remaining =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        assertThat(remaining.getContent()).hasSize(1);
    }

    @Test
    public void testFindAllFilteredPagesByEventNotByDataRow() {
        LocalDateTime now = LocalDateTime.now();

        for (int index = 0; index < 5; index++) {
            save(
                "alice", "PERMISSION_CHECK", now.minusMinutes(index),
                Map.of("method", "com.bytechef.Facade.method" + index, "result", "ALLOWED", "errorClass", "none"));
        }

        Page<PersistentAuditEvent> firstPage =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getContent())
            .hasSize(2)
            .allSatisfy(event -> assertThat(event.getData()).hasSize(3));
        assertThat(firstPage.getContent())
            .extracting(event -> event.getData()
                .get("method"))
            .containsExactly("com.bytechef.Facade.method0", "com.bytechef.Facade.method1");

        Page<PersistentAuditEvent> lastPage =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(2, 2));

        assertThat(lastPage.getContent())
            .hasSize(1)
            .allSatisfy(event -> assertThat(event.getData()).hasSize(3));
        assertThat(lastPage.getContent())
            .extracting(event -> event.getData()
                .get("method"))
            .containsExactly("com.bytechef.Facade.method4");
    }

    @Test
    public void testFindAllFilteredBySearchIsCaseInsensitive() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("method", "com.bytechef.ProjectFacade.delete"));
        save("alice", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("method", "com.bytechef.UserFacade.create"));

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, "projectfacade", true, null),
                PageRequest.of(0, 25));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent())
            .hasSize(1)
            .allSatisfy(event -> assertThat(event.getData()).containsEntry(
                "method", "com.bytechef.ProjectFacade.delete"));
    }

    @Test
    public void testFindAllFilteredBySearchTreatsWildcardsLiterally() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("method", "com.bytechef.ProjectFacade.delete"));
        save("alice", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("note", "100% done_now"));

        Page<PersistentAuditEvent> percentPage = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, "%", true, null), PageRequest.of(0, 25));

        assertThat(percentPage.getContent())
            .hasSize(1)
            .allSatisfy(event -> assertThat(event.getData()).containsKey("note"));

        Page<PersistentAuditEvent> underscorePage = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, "done_now", true, null), PageRequest.of(0, 25));

        assertThat(underscorePage.getContent()).hasSize(1);

        Page<PersistentAuditEvent> noMatchPage = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, "d_ne", true, null), PageRequest.of(0, 25));

        assertThat(noMatchPage.getContent()).isEmpty();
    }

    @Test
    public void testDeleteByEventDateBeforeRemovesDataRows() {
        LocalDateTime now = LocalDateTime.now();

        save("alice", "PERMISSION_CHECK", now.minusDays(400), Map.of("method", "expired", "result", "ALLOWED"));
        save("alice", "PERMISSION_CHECK", now.minusDays(10), Map.of("method", "kept", "result", "ALLOWED"));

        persistenceAuditEventRepository.deleteByEventDateBefore(now.minusDays(365));

        Page<PersistentAuditEvent> expiredPage = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, "expired", true, null), PageRequest.of(0, 25));

        assertThat(expiredPage.getContent()).isEmpty();

        Integer orphanCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM persistent_audit_event_data d WHERE NOT EXISTS "
                + "(SELECT 1 FROM persistent_audit_event e WHERE e.id = d.persistent_audit_event_id)",
            Integer.class);

        assertThat(orphanCount).isZero();
    }

    @Test
    public void testFindAllFilteredCombinesEveryFilterAndPagesPastTheFirstPage() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0);

        for (int index = 0; index < 40; index++) {
            save(
                index % 5 == 0 ? "needle-owner" : "alice", index % 3 == 0 ? "OTHER_EVENT" : "TARGET_EVENT",
                base.minusMinutes(index),
                Map.of("method", "method" + index, "note", index % 4 == 0 ? "needle-note" : "plain"));
        }

        List<String> methods = new ArrayList<>();

        for (int pageNumber = 0; pageNumber < 3; pageNumber++) {
            Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
                new AuditEventFilter("TARGET_EVENT", base.minusMinutes(35), base, "needle", true, null),
                PageRequest.of(pageNumber, 4));

            assertThat(page.getTotalElements()).isEqualTo(10);
            assertThat(page.getContent()).hasSize(pageNumber < 2 ? 4 : 2);
            assertThat(page.getContent())
                .allSatisfy(event -> assertThat(event.getData()).hasSize(2));

            page.getContent()
                .forEach(event -> methods.add(event.getData()
                    .get("method")));
        }

        assertThat(methods).containsExactly(
            "method4", "method5", "method8", "method10", "method16", "method20", "method25", "method28", "method32",
            "method35");
    }

    @Test
    public void testFindAllFilteredOrdersEventsSharingADateById() {
        LocalDateTime eventDate = LocalDateTime.of(2026, 1, 1, 12, 0);

        for (int index = 0; index < 5; index++) {
            save("alice", "PERMISSION_CHECK", eventDate, Map.of("method", "method" + index));
        }

        List<Long> ids = new ArrayList<>();

        for (int pageNumber = 0; pageNumber < 3; pageNumber++) {
            Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(),
                PageRequest.of(pageNumber, 2));

            page.getContent()
                .forEach(event -> ids.add(event.getId()));
        }

        assertThat(ids)
            .hasSize(5)
            .doesNotHaveDuplicates()
            .isSortedAccordingTo((first, second) -> Long.compare(second, first));
    }

    @Test
    public void testFindAllFilteredBySearchTreatsBackslashLiterally() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("path", "C:\\temp\\file"));
        save("alice", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("path", "C:/temp/file"));

        Page<PersistentAuditEvent> backslashPage = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, "C:\\te", true, null), PageRequest.of(0, 25));

        assertThat(backslashPage.getContent())
            .singleElement()
            .satisfies(event -> assertThat(event.getData()).containsEntry("path", "C:\\temp\\file"));

        Page<PersistentAuditEvent> trailingBackslashPage = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, "temp\\", true, null), PageRequest.of(0, 25));

        assertThat(trailingBackslashPage.getContent()).hasSize(1);
    }

    private void save(String principal, String eventType, LocalDateTime eventDate) {
        save(principal, eventType, eventDate, Map.of("k", "v"));
    }

    private void save(String principal, String eventType, LocalDateTime eventDate, Map<String, String> data) {
        save(principal, eventType, eventDate, null, data);
    }

    private void save(String principal, String eventType, LocalDateTime eventDate, PlatformType platform) {
        save(principal, eventType, eventDate, platform, Map.of("k", "v"));
    }

    private void save(
        String principal, String eventType, LocalDateTime eventDate, PlatformType platform, Map<String, String> data) {

        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setPlatform(platform);
        persistentAuditEvent.setPrincipal(principal);
        persistentAuditEvent.setEventType(eventType);
        persistentAuditEvent.setEventDate(eventDate);
        persistentAuditEvent.setData(data);

        persistenceAuditEventRepository.save(persistentAuditEvent);
    }
}
