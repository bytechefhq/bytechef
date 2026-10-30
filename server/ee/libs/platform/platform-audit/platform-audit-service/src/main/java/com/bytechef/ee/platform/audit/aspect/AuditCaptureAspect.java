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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.security.authorization.method.AuthorizationInterceptorsOrder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final @Nullable TransactionTemplate nestedTransactionTemplate;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public AuditCaptureAspect(
        AuditMapperResolver auditMapperResolver,
        ObjectProvider<PlatformTransactionManager> transactionManagerProvider) {

        this.auditMapperResolver = auditMapperResolver;

        PlatformTransactionManager transactionManager = transactionManagerProvider.getIfAvailable();

        if (transactionManager == null) {
            this.nestedTransactionTemplate = null;
        } else {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);

            this.nestedTransactionTemplate = transactionTemplate;
        }
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

                auditFrame.setCaptured(captureBehindSavepoint(auditMapper, auditInvocation));
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

    private @Nullable Object captureBehindSavepoint(AuditMapper auditMapper, AuditInvocation auditInvocation) {
        if (nestedTransactionTemplate == null || !TransactionSynchronizationManager.isActualTransactionActive()) {
            return auditMapper.capture(auditInvocation);
        }

        return nestedTransactionTemplate.execute(transactionStatus -> auditMapper.capture(auditInvocation));
    }

    @Override
    public int getOrder() {
        return AuthorizationInterceptorsOrder.JSR250.getOrder() + 1;
    }
}
