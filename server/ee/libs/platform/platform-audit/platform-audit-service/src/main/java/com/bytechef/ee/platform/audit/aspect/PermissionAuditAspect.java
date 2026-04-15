/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.PermissionAuditEventService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.util.SecurityUtils;
import java.time.LocalDateTime;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Aspect that audits permission checks on methods annotated with
 * {@link org.springframework.security.access.prepost.PreAuthorize}. Each invocation produces a
 * {@link PersistentAuditEvent} recording the principal, target method, and whether access was ALLOWED or DENIED.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Aspect
@Component
@ConditionalOnEEVersion
public class PermissionAuditAspect {

    private static final String PERMISSION_CHECK = "PERMISSION_CHECK";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PermissionAuditEventService permissionAuditEventService;

    public PermissionAuditAspect(PermissionAuditEventService permissionAuditEventService) {
        this.permissionAuditEventService = permissionAuditEventService;
    }

    @Around("@annotation(org.springframework.security.access.prepost.PreAuthorize)")
    public Object auditPermissionCheck(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String result = "ALLOWED";

        try {
            return proceedingJoinPoint.proceed();
        } catch (AccessDeniedException accessDeniedException) {
            result = "DENIED";

            throw accessDeniedException;
        } catch (Throwable throwable) {
            result = "ERROR";

            throw throwable;
        } finally {
            saveAuditEvent(proceedingJoinPoint, result);
        }
    }

    private void saveAuditEvent(ProceedingJoinPoint proceedingJoinPoint, String result) {
        try {
            String principal = SecurityUtils.fetchCurrentUserLogin()
                .orElse("anonymoususer");

            MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();

            String targetMethod =
                methodSignature.getDeclaringTypeName() + "." + methodSignature.getMethod()
                    .getName();

            PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

            persistentAuditEvent.setEventDate(LocalDateTime.now());
            persistentAuditEvent.setEventType(PERMISSION_CHECK);
            persistentAuditEvent.setPrincipal(principal);
            persistentAuditEvent.setData(Map.of("method", targetMethod, "result", result));

            permissionAuditEventService.save(persistentAuditEvent);
        } catch (Exception exception) {
            logger.error(
                "AUDIT PERSISTENCE FAILURE: Failed to save permission audit event for {} — "
                    + "audit trail is incomplete. Result was '{}', principal was '{}'",
                proceedingJoinPoint.getSignature()
                    .toShortString(),
                result,
                SecurityUtils.fetchCurrentUserLogin()
                    .orElse("unknown"),
                exception);
        }
    }
}
