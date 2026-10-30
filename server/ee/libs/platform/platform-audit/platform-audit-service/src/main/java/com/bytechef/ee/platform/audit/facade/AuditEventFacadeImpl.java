/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.facade;

import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class AuditEventFacadeImpl implements AuditEventFacade {

    private final AuditEventService auditEventService;

    @SuppressFBWarnings("EI")
    AuditEventFacadeImpl(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Page<PersistentAuditEvent> fetchAuditEvents(AuditEventFilter auditEventFilter, Pageable pageable) {

        return auditEventService.fetchAuditEvents(auditEventFilter, pageable);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<String> fetchEventTypes() {
        return auditEventService.fetchEventTypes();
    }
}
