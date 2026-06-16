/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.repository;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Custom repository fragment for audit event queries that require pagination support.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
interface CustomPersistenceAuditEventRepository {

    int deleteByEventDateBefore(LocalDateTime cutoff);

    Page<PersistentAuditEvent> findAllFiltered(
        String principal, String eventType, LocalDateTime fromDate, LocalDateTime toDate, String dataSearch,
        Pageable pageable);

    List<String> findDistinctEventTypes();
}
