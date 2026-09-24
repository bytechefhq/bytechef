/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AuditEventServiceTest {

    private final PersistenceAuditEventRepository persistenceAuditEventRepository =
        mock(PersistenceAuditEventRepository.class);

    private final AuditEventService auditEventService = new AuditEventService(persistenceAuditEventRepository);

    @Test
    void testFetchAuditEventsDelegatesToRepository() {
        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setId(42L);

        Page<PersistentAuditEvent> page = new PageImpl<>(List.of(persistentAuditEvent));

        LocalDateTime fromDate = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime toDate = LocalDateTime.of(2026, 9, 21, 23, 59);
        Pageable pageable = PageRequest.of(2, 25);

        when(persistenceAuditEventRepository.findAllFiltered(
            new AuditEventFilter("alice", "PERMISSION_CHECK", fromDate, toDate, "ProjectFacade"), pageable))
                .thenReturn(page);

        Page<PersistentAuditEvent> result = auditEventService.fetchAuditEvents(
            new AuditEventFilter("alice", "PERMISSION_CHECK", fromDate, toDate, "ProjectFacade"), pageable);

        verify(persistenceAuditEventRepository).findAllFiltered(
            new AuditEventFilter("alice", "PERMISSION_CHECK", fromDate, toDate, "ProjectFacade"), pageable);

        assertThat(result.getContent()).hasSize(1);

        assertThat(result.getContent()
            .getFirst()
            .getId()).isEqualTo(42L);
    }

    @Test
    void testFetchEventTypesDelegatesToRepository() {
        when(persistenceAuditEventRepository.findDistinctEventTypes())
            .thenReturn(List.of("CONNECTION_CREATED", "PERMISSION_CHECK"));

        List<String> types = auditEventService.fetchEventTypes();

        assertThat(types).containsExactly("CONNECTION_CREATED", "PERMISSION_CHECK");
    }
}
