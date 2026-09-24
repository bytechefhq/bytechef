/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    public void testFindAllFilteredByPrincipal() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("bob", "PERMISSION_CHECK", LocalDateTime.now());

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter("alice", null, null, null, null), PageRequest.of(0, 25));

        assertThat(page.getContent())
            .hasSize(1)
            .allSatisfy(event -> assertThat(event.getPrincipal()).isEqualTo("alice"));
    }

    @Test
    public void testFindAllFilteredByEventType() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("alice", "CONNECTION_CREATED", LocalDateTime.now());

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, "CONNECTION_CREATED", null, null, null), PageRequest.of(0, 25));

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
            .findAllFiltered(new AuditEventFilter(null, null, now.minusDays(5), now, null), PageRequest.of(0, 25));

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
    public void testFindAllFilteredByDataSearchIsCaseInsensitive() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("method", "com.bytechef.ProjectFacade.delete"));
        save("alice", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("method", "com.bytechef.UserFacade.create"));

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, null, "projectfacade"), PageRequest.of(0, 25));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent())
            .hasSize(1)
            .allSatisfy(event -> assertThat(event.getData()).containsEntry(
                "method", "com.bytechef.ProjectFacade.delete"));
    }

    @Test
    public void testFindAllFilteredByDataSearchTreatsWildcardsLiterally() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("method", "com.bytechef.ProjectFacade.delete"));
        save("alice", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("note", "100% done_now"));

        Page<PersistentAuditEvent> percentPage = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, null, "%"), PageRequest.of(0, 25));

        assertThat(percentPage.getContent())
            .hasSize(1)
            .allSatisfy(event -> assertThat(event.getData()).containsKey("note"));

        Page<PersistentAuditEvent> underscorePage = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, null, "done_now"), PageRequest.of(0, 25));

        assertThat(underscorePage.getContent()).hasSize(1);

        Page<PersistentAuditEvent> noMatchPage = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, null, "d_ne"), PageRequest.of(0, 25));

        assertThat(noMatchPage.getContent()).isEmpty();
    }

    @Test
    public void testDeleteByEventDateBeforeRemovesDataRows() {
        LocalDateTime now = LocalDateTime.now();

        save("alice", "PERMISSION_CHECK", now.minusDays(400), Map.of("method", "expired", "result", "ALLOWED"));
        save("alice", "PERMISSION_CHECK", now.minusDays(10), Map.of("method", "kept", "result", "ALLOWED"));

        persistenceAuditEventRepository.deleteByEventDateBefore(now.minusDays(365));

        Page<PersistentAuditEvent> expiredPage = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, null, "expired"), PageRequest.of(0, 25));

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
                index % 2 == 0 ? "alice" : "bob", index % 3 == 0 ? "OTHER_EVENT" : "TARGET_EVENT",
                base.minusMinutes(index),
                Map.of(
                    "method", "method" + index,
                    "target", index % 6 == 2 ? "hay" : "needle-target",
                    "note", index % 4 == 0 ? "needle-note" : "plain"));
        }

        List<String> methods = new ArrayList<>();

        for (int pageNumber = 0; pageNumber < 3; pageNumber++) {
            Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
                new AuditEventFilter("alice", "TARGET_EVENT", base.minusMinutes(35), base, "needle"),
                PageRequest.of(pageNumber, 4));

            assertThat(page.getTotalElements()).isEqualTo(9);
            assertThat(page.getContent()).hasSize(pageNumber < 2 ? 4 : 1);
            assertThat(page.getContent())
                .allSatisfy(event -> assertThat(event.getData()).hasSize(3));

            page.getContent()
                .forEach(event -> methods.add(event.getData()
                    .get("method")));
        }

        assertThat(methods).containsExactly(
            "method4", "method8", "method10", "method16", "method20", "method22", "method28", "method32", "method34");
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
    public void testFindAllFilteredByDataSearchTreatsBackslashLiterally() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("path", "C:\\temp\\file"));
        save("alice", "PERMISSION_CHECK", LocalDateTime.now(), Map.of("path", "C:/temp/file"));

        Page<PersistentAuditEvent> backslashPage = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, null, "C:\\te"), PageRequest.of(0, 25));

        assertThat(backslashPage.getContent())
            .singleElement()
            .satisfies(event -> assertThat(event.getData()).containsEntry("path", "C:\\temp\\file"));

        Page<PersistentAuditEvent> trailingBackslashPage = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, null, "temp\\"), PageRequest.of(0, 25));

        assertThat(trailingBackslashPage.getContent()).hasSize(1);
    }

    private void save(String principal, String eventType, LocalDateTime eventDate) {
        save(principal, eventType, eventDate, Map.of("k", "v"));
    }

    private void save(String principal, String eventType, LocalDateTime eventDate, Map<String, String> data) {
        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setPrincipal(principal);
        persistentAuditEvent.setEventType(eventType);
        persistentAuditEvent.setEventDate(eventDate);
        persistentAuditEvent.setData(data);

        persistenceAuditEventRepository.save(persistentAuditEvent);
    }
}
