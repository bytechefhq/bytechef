/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.domain;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AuditEventFilter(
    @Nullable String principal, @Nullable String eventType, @Nullable LocalDateTime fromDate,
    @Nullable LocalDateTime toDate, @Nullable String dataSearch) {

    public static AuditEventFilter empty() {
        return new AuditEventFilter(null, null, null, null, null);
    }
}
