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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves audit events in a new transaction so that audit INSERTs succeed even when the calling method is in a read-only
 * transaction.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class PermissionAuditEventService {

    private final PersistenceAuditEventRepository persistenceAuditEventRepository;

    @SuppressFBWarnings("EI")
    public PermissionAuditEventService(PersistenceAuditEventRepository persistenceAuditEventRepository) {
        this.persistenceAuditEventRepository = persistenceAuditEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(PersistentAuditEvent persistentAuditEvent) {
        persistenceAuditEventRepository.save(persistentAuditEvent);
    }
}
