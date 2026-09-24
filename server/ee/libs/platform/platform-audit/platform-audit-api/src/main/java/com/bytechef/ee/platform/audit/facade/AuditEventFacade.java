/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.facade;

import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Facade for read-only access to persistent audit events. Hosts the {@code ADMIN} authorization guard so it applies to
 * every caller of the facade rather than only the GraphQL entry point, and keeps it off the shared
 * {@code AuditEventService}, which {@code AuditAspect} and {@code JobStatusAuditListener} write through.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AuditEventFacade {

    Page<PersistentAuditEvent> fetchAuditEvents(AuditEventFilter auditEventFilter, Pageable pageable);

    List<String> fetchEventTypes();
}
