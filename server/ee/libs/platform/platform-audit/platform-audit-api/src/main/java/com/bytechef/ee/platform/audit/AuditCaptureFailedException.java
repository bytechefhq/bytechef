/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit;

/**
 * Thrown by {@link SpelAuditEngine} when SpEL evaluation of an audit event marked {@code strictAudit} fails. Propagates
 * out of the aspect so the surrounding {@code @Transactional} boundary rolls back the just-succeeded mutation — a
 * compliance-grade event must not commit without a trail. Non-strict events continue to absorb SpEL failures into the
 * aspect's audit-failure metric.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AuditCaptureFailedException extends RuntimeException {

    public AuditCaptureFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
