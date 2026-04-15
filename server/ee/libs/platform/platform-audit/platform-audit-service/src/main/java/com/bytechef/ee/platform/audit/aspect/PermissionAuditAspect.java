/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Aspect that audits permission checks on methods annotated with
 * {@link org.springframework.security.access.prepost.PreAuthorize}. Each invocation produces a
 * {@link PersistentAuditEvent} recording the principal, target method, and whether access was ALLOWED or DENIED.
 *
 * <p>
 * Ordering: this aspect runs at {@link Ordered#HIGHEST_PRECEDENCE} so that it wraps Spring Security's
 * {@code AuthorizationManagerBeforeMethodInterceptor} (which runs near {@code LOWEST_PRECEDENCE - 1}). Without the
 * explicit order, the security interceptor would throw {@link AccessDeniedException} <i>before</i> this aspect's
 * {@code proceed()}, leaving DENIED events unrecorded.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Aspect
@Component
@ConditionalOnEEVersion
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PermissionAuditAspect {

    private static final String PERMISSION_CHECK = "PERMISSION_CHECK";

    // Distinct from the Spring Security "anonymousUser" principal — "anonymousUser" means an anonymous authentication
    // token is present (legitimately hitting a permit-all endpoint); this literal means NO SecurityContext existed at
    // all (e.g., async executor without DelegatingSecurityContextExecutor, or a guarded method reached before the
    // security filter ran). Operators grepping audit rows need to tell the two apart — the first is routine, the
    // second is an authentication-flow bug.
    private static final String MISSING_CONTEXT_PRINCIPAL = "__NO_SECURITY_CONTEXT__";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AuditEventService auditEventService;
    private final @Nullable Counter auditFailureCounter;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public PermissionAuditAspect(
        AuditEventService auditEventService, ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.auditEventService = auditEventService;

        // Lightweight EE apps (e.g., runtime-job-app) may start without actuator — ObjectProvider lets us resolve the
        // registry if present, fall back to null otherwise. When null, audit failures are logged but not counted, so
        // operators running the reduced image still surface the outage in application logs.
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        this.auditFailureCounter = meterRegistry == null ? null : Counter.builder("bytechef_permission_audit_failure")
            .description(
                "Number of permission audit events that failed to persist. Non-zero values indicate a gap in the "
                    + "audit trail \u2014 permission checks still ran, but no PersistentAuditEvent was recorded.")
            .register(meterRegistry);
    }

    @Around("@annotation(org.springframework.security.access.prepost.PreAuthorize)")
    public Object auditPermissionCheck(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        // Capture the principal at entry — by the time the finally block runs, the SecurityContext can have been
        // cleared (e.g., by a downstream filter). Caching here also avoids re-throwing inside the catch block.
        String principal = SecurityUtils.fetchCurrentUserLogin()
            .orElse(MISSING_CONTEXT_PRINCIPAL);

        String result = "ALLOWED";
        Class<? extends Throwable> errorClass = null;

        try {
            return proceedingJoinPoint.proceed();
        } catch (AccessDeniedException accessDeniedException) {
            result = "DENIED";

            throw accessDeniedException;
        } catch (Throwable throwable) {
            result = "ERROR";
            errorClass = throwable.getClass();

            throw throwable;
        } finally {
            saveAuditEvent(proceedingJoinPoint, principal, result, errorClass);
        }
    }

    private void saveAuditEvent(
        ProceedingJoinPoint proceedingJoinPoint, String principal, String result,
        @Nullable Class<? extends Throwable> errorClass) {

        try {
            MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();

            String targetMethod =
                methodSignature.getDeclaringTypeName() + "." + methodSignature.getMethod()
                    .getName();

            // Record errorClass alongside result so operators grepping for ERROR audit rows have a forensic trail
            // back to what actually went wrong. PersistentAuditEvent.setData takes Map<String, String>, so the
            // errorClass value is the FQN string.
            Map<String, String> data = new HashMap<>(3);

            data.put("method", targetMethod);
            data.put("result", result);

            if (errorClass != null) {
                data.put("errorClass", errorClass.getName());
            }

            PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

            persistentAuditEvent.setEventDate(LocalDateTime.now());
            persistentAuditEvent.setEventType(PERMISSION_CHECK);
            persistentAuditEvent.setPrincipal(principal);
            persistentAuditEvent.setData(data);

            auditEventService.save(persistentAuditEvent);
        } catch (Exception exception) {
            // Audit-write failure must not fail the operation, but it does compromise the audit trail. Counter ticks
            // first so a sustained DB outage on the audit table pages operators via metrics dashboards; the loud log
            // line remains as a forensic trail for investigators pulling application logs.
            if (auditFailureCounter != null) {
                auditFailureCounter.increment();
            }

            logger.error(
                "AUDIT PERSISTENCE FAILURE: Failed to save permission audit event for {} \u2014 "
                    + "audit trail is incomplete. Result was '{}', principal was '{}'",
                proceedingJoinPoint.getSignature()
                    .toShortString(),
                result,
                principal,
                exception);
        }
    }
}
