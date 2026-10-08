/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.repository;

import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom repository fragment for the audit event queries Spring Data cannot derive: filtered paging, the retention
 * purge and the distinct event types.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
interface CustomPersistenceAuditEventRepository {

    /**
     * Deletes the events older than the cutoff together with their data rows. The data rows have no foreign key to
     * cascade from, so both deletes share one transaction: a failure between them must not leave events stripped of
     * their data.
     */
    @Transactional
    int deleteByEventDateBefore(LocalDateTime cutoff);

    Page<PersistentAuditEvent> findAllFiltered(AuditEventFilter auditEventFilter, Pageable pageable);

    List<String> findDistinctEventTypes();
}
