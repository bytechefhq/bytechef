/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;

/**
 * Covers the around-advice behavior of {@link PermissionAuditAspect}: ALLOWED/DENIED/ERROR outcomes are recorded, and
 * the original exception is re-thrown so Spring Security's filter chain can convert it to the correct HTTP status.
 * Ordering ({@code @Order(HIGHEST_PRECEDENCE)}) is pinned via annotation reflection since order itself is only
 * observable in a real Spring integration.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class PermissionAuditAspectTest {

    private AuditEventService auditEventService;
    private PermissionAuditAspect aspect;
    private ProceedingJoinPoint joinPoint;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() throws Throwable {
        auditEventService = mock(AuditEventService.class);
        meterRegistry = new SimpleMeterRegistry();

        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(meterRegistry);

        aspect = new PermissionAuditAspect(auditEventService, provider);
        joinPoint = mock(ProceedingJoinPoint.class);

        // Signature returned by ProceedingJoinPoint.getSignature(). Used for both the normal flow and the
        // audit-persistence catch block (where toShortString is invoked on failure).
        MethodSignature signature = mock(MethodSignature.class);

        when(signature.getDeclaringTypeName()).thenReturn("TestService");
        when(signature.getMethod()).thenReturn(getClass().getDeclaredMethod("dummyTargetMethod"));
        when(joinPoint.getSignature()).thenReturn(signature);
    }

    @Test
    void testAllowedPathRecordsAllowedResult() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.auditPermissionCheck(joinPoint);

        assertThat(result).isEqualTo("ok");

        PersistentAuditEvent captured = captureSavedEvent();

        assertThat(captured.getData()).containsEntry("result", "ALLOWED");
        assertThat(captured.getEventType()).isEqualTo("PERMISSION_CHECK");
    }

    @Test
    void testAccessDeniedRecordsDeniedAndPropagates() throws Throwable {
        AccessDeniedException expected = new AccessDeniedException("denied");

        when(joinPoint.proceed()).thenThrow(expected);

        assertThatThrownBy(() -> aspect.auditPermissionCheck(joinPoint))
            .isSameAs(expected);

        PersistentAuditEvent captured = captureSavedEvent();

        assertThat(captured.getData()).containsEntry("result", "DENIED");
    }

    @Test
    void testUnexpectedExceptionRecordsErrorAndPropagates() throws Throwable {
        RuntimeException unexpected = new RuntimeException("boom");

        when(joinPoint.proceed()).thenThrow(unexpected);

        assertThatThrownBy(() -> aspect.auditPermissionCheck(joinPoint))
            .isSameAs(unexpected);

        PersistentAuditEvent captured = captureSavedEvent();

        assertThat(captured.getData()).containsEntry("result", "ERROR");
    }

    @Test
    void testAuditPersistenceFailureDoesNotMaskOriginalOutcome() throws Throwable {
        // If AuditEventService.save throws (e.g., DB down), the original outcome (here: ALLOWED + return value) must
        // still propagate. Otherwise an audit-backend outage would DoS every @PreAuthorize-protected endpoint.
        when(joinPoint.proceed()).thenReturn("ok");
        org.mockito.Mockito.doThrow(new IllegalStateException("audit backend down"))
            .when(auditEventService)
            .save(any());

        Object result = aspect.auditPermissionCheck(joinPoint);

        assertThat(result).isEqualTo("ok");
        verify(auditEventService, times(1)).save(any());
    }

    @Test
    void testAuditPersistenceFailureIncrementsCounter() throws Throwable {
        // Operators rely on the bytechef_permission_audit_failure counter to page when the audit trail has a gap.
        // Log-only alerting used to miss sustained outages that happened outside business hours.
        when(joinPoint.proceed()).thenReturn("ok");
        org.mockito.Mockito.doThrow(new IllegalStateException("audit backend down"))
            .when(auditEventService)
            .save(any());

        aspect.auditPermissionCheck(joinPoint);

        Counter counter = meterRegistry.find("bytechef_permission_audit_failure")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testAuditSucceedsDoesNotIncrementFailureCounter() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.auditPermissionCheck(joinPoint);

        Counter counter = meterRegistry.find("bytechef_permission_audit_failure")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(0.0);
    }

    @Test
    void testAspectIsOrderedHighestPrecedence() {
        // Ordering is what makes the DENIED path recordable — without HIGHEST_PRECEDENCE, Spring Security's
        // authorization advisor runs first and throws before this aspect's proceed().
        org.springframework.core.annotation.Order order =
            PermissionAuditAspect.class.getAnnotation(org.springframework.core.annotation.Order.class);

        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
    }

    private PersistentAuditEvent captureSavedEvent() {
        ArgumentCaptor<PersistentAuditEvent> captor = ArgumentCaptor.forClass(PersistentAuditEvent.class);

        verify(auditEventService).save(captor.capture());

        return captor.getValue();
    }

    @SuppressWarnings("unused")
    private void dummyTargetMethod() {
        // Referenced reflectively to produce a Method instance for the mocked MethodSignature.
    }
}
