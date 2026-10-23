/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import org.aspectj.lang.annotation.Pointcut;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AuditPointcuts {

    private AuditPointcuts() {
    }

    @Pointcut("@annotation(com.bytechef.platform.audit.Audited)")
    static void auditedMethod() {
    }

    @Pointcut("@annotation(org.springframework.security.access.prepost.PreAuthorize)")
    static void preAuthorizedMethod() {
    }
}
