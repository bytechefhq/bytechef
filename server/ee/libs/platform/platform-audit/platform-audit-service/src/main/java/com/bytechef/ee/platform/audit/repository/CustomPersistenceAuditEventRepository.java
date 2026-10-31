/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.repository;

import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.platform.constant.PlatformType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
interface CustomPersistenceAuditEventRepository {

    @Transactional
    int deleteByEventDateBefore(LocalDateTime cutoff);

    Page<PersistentAuditEvent> findAllFiltered(AuditEventFilter auditEventFilter, Pageable pageable);

    List<String> findDistinctEventTypes();

    @Transactional
    int updateJobCreatedPlatform(long jobId, PlatformType platform, LocalDateTime since);
}
