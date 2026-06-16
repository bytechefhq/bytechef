/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.facade;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Facade for read-only access to persistent audit events. Hosts the {@code ADMIN} authorization guard so it applies to
 * every caller of the facade rather than only the GraphQL entry point, and keeps it off the shared
 * {@code AuditEventService} which the audit-write path (e.g. the permission audit aspect) relies on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AuditEventFacade {

    Page<PersistentAuditEvent> fetchAuditEvents(
        String principal, String eventType, LocalDateTime fromDate, LocalDateTime toDate, String dataSearch,
        Pageable pageable);

    List<String> fetchEventTypes();
}
