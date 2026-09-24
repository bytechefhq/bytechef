/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.service;

import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and persists audit events. Writes never borrow a second pooled connection: with no transaction on the thread
 * they open their own, and inside an existing transaction they run on the caller's connection behind a savepoint, so a
 * failed audit insert rolls back to that savepoint and leaves the caller's transaction usable.
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
    public Page<PersistentAuditEvent> fetchAuditEvents(AuditEventFilter auditEventFilter, Pageable pageable) {

        return persistenceAuditEventRepository.findAllFiltered(auditEventFilter, pageable);
    }

    @Transactional(readOnly = true)
    public List<String> fetchEventTypes() {
        return persistenceAuditEventRepository.findDistinctEventTypes();
    }

    @Transactional(propagation = Propagation.NESTED)
    public void saveAll(List<PersistentAuditEvent> persistentAuditEvents) {
        persistenceAuditEventRepository.saveAll(persistentAuditEvents);
    }
}
