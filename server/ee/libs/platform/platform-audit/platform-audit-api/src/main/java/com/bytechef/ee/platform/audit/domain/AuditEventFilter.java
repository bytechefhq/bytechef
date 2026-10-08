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
 * Optional criteria for reading audit events; a {@code null} component does not filter. Dates are inclusive and
 * {@code dataSearch} is a case-insensitive substring match on data values.
 *
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
