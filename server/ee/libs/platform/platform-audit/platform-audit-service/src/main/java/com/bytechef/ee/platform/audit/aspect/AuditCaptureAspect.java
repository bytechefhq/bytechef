/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.audit.Audited;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.security.authorization.method.AuthorizationInterceptorsOrder;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Aspect
@Component
@ConditionalOnEEVersion
public class AuditCaptureAspect implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuditCaptureAspect.class);

    private final AuditMapperResolver auditMapperResolver;

    @SuppressFBWarnings("EI")
    public AuditCaptureAspect(AuditMapperResolver auditMapperResolver) {
        this.auditMapperResolver = auditMapperResolver;
    }

    @Around("com.bytechef.ee.platform.audit.aspect.AuditPointcuts.auditedMethod()")
    public Object capture(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        AuditFrame auditFrame = AuditFrame.current();
        Audited audited = auditFrame == null ? null : auditFrame.getAudited();

        if (audited != null) {
            try {
                AuditMapper auditMapper = auditMapperResolver.resolve(audited.mapper());
                AuditInvocation auditInvocation = AuditJoinPoints.createInvocation(
                    proceedingJoinPoint, audited.event(), null, null, AuditOutcome.SUCCESS, null);

                auditFrame.setCaptured(auditMapper.capture(auditInvocation));
            } catch (Exception | LinkageError | StackOverflowError exception) {
                auditFrame.setCaptureError(
                    exception.getClass()
                        .getName());

                log.error(
                    "AUDIT CAPTURE FAILURE: {} could not capture the before-state of event {}",
                    audited.mapper()
                        .getName(),
                    audited.event(), exception);
            }
        }

        return proceedingJoinPoint.proceed();
    }

    @Override
    public int getOrder() {
        return AuthorizationInterceptorsOrder.JSR250.getOrder() + 1;
    }
}
