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
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AuditEventFacade {

    Page<PersistentAuditEvent> fetchAuditEvents(AuditEventFilter auditEventFilter, Pageable pageable);

    List<String> fetchEventTypes();
}
