/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AuditPersistenceException extends RuntimeException {

    public AuditPersistenceException(String eventType, Throwable cause) {
        super("The audit event %s could not be recorded, so the operation was rolled back".formatted(eventType), cause);
    }
}
