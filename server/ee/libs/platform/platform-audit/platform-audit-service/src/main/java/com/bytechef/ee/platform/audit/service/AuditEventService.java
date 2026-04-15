/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.service;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists audit events. Uses REQUIRES_NEW so INSERTs succeed even when the caller is inside a read-only transaction
 * (permission aspect around @PreAuthorize methods, connection aspect on facades marked read-only, etc.).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class AuditEventService {

    private final PersistenceAuditEventRepository persistenceAuditEventRepository;

    @SuppressFBWarnings("EI")
    public AuditEventService(PersistenceAuditEventRepository persistenceAuditEventRepository) {
        this.persistenceAuditEventRepository = persistenceAuditEventRepository;
    }

    @Transactional(readOnly = true)
    public Page<PersistentAuditEvent> fetchAuditEvents(
        String principal, String eventType, LocalDateTime fromDate, LocalDateTime toDate, String dataSearch,
        Pageable pageable) {

        return persistenceAuditEventRepository.findAllFiltered(
            principal, eventType, fromDate, toDate, dataSearch, pageable);
    }

    @Transactional(readOnly = true)
    public List<String> fetchEventTypes() {
        return persistenceAuditEventRepository.findDistinctEventTypes();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(PersistentAuditEvent persistentAuditEvent) {
        persistenceAuditEventRepository.save(persistentAuditEvent);
    }
}
