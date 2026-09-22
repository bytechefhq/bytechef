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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.Audited;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Covers the around-advice behavior of {@link AuditAspect}: outcome recording, buffering and flushing around
 * transactions, read-call suppression, {@code @Audited} mapper handling and principal resolution, and that the original
 * exception is re-thrown so Spring Security's filter chain can convert it to the correct HTTP status. Ordering
 * ({@code @Order(HIGHEST_PRECEDENCE)}) is pinned via annotation reflection since order itself is only observable in a
 * real Spring integration.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AuditAspectTest {

    private AuditEventService auditEventService;
    private AuditAspect aspect;
    private AuditMapperResolver auditMapperResolver;
    private ProceedingJoinPoint joinPoint;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() throws Throwable {
        auditEventService = mock(AuditEventService.class);
        meterRegistry = new SimpleMeterRegistry();

        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(meterRegistry);

        auditMapperResolver = mock(AuditMapperResolver.class);

        aspect = new AuditAspect(auditEventService, auditMapperResolver, provider);
        joinPoint = mock(ProceedingJoinPoint.class);

        MethodSignature signature = mock(MethodSignature.class);

        when(signature.getDeclaringTypeName()).thenReturn("TestService");
        when(signature.getMethod()).thenReturn(getClass().getDeclaredMethod("dummyTargetMethod"));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[0]);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
    }

    @Test
    void testAllowedPathRecordsAllowedResult() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.audit(joinPoint);

        assertThat(result).isEqualTo("ok");

        PersistentAuditEvent captured = captureSavedEvent();

        assertThat(captured.getData()).containsEntry("result", "ALLOWED");
        assertThat(captured.getEventType()).isEqualTo("PERMISSION_CHECK");
    }

    @Test
    void testAccessDeniedRecordsDeniedAndPropagates() throws Throwable {
        AccessDeniedException expected = new AccessDeniedException("denied");

        when(joinPoint.proceed()).thenThrow(expected);

        assertThatThrownBy(() -> aspect.audit(joinPoint))
            .isSameAs(expected);

        PersistentAuditEvent captured = captureSavedEvent();

        assertThat(captured.getData()).containsEntry("result", "DENIED");
    }

    @Test
    void testUnexpectedExceptionRecordsErrorAndPropagates() throws Throwable {
        RuntimeException unexpected = new RuntimeException("boom");

        when(joinPoint.proceed()).thenThrow(unexpected);

        assertThatThrownBy(() -> aspect.audit(joinPoint))
            .isSameAs(unexpected);

        PersistentAuditEvent captured = captureSavedEvent();

        assertThat(captured.getData()).containsEntry("result", "ERROR");
    }

    @Test
    void testAuditPersistenceFailureDoesNotMaskOriginalOutcome() throws Throwable {
        // If AuditEventService.saveAll throws (e.g., DB down), the original outcome (here: ALLOWED + return value) must
        // still propagate. Otherwise an audit-backend outage would DoS every @PreAuthorize-protected endpoint.
        when(joinPoint.proceed()).thenReturn("ok");
        org.mockito.Mockito.doThrow(new IllegalStateException("audit backend down"))
            .when(auditEventService)
            .saveAll(any());

        Object result = aspect.audit(joinPoint);

        assertThat(result).isEqualTo("ok");
        verify(auditEventService, times(1)).saveAll(any());
    }

    @Test
    void testAuditPersistenceFailureIncrementsCounter() throws Throwable {
        // Operators rely on the bytechef_audit_failure counter to page when the audit trail has a gap.
        when(joinPoint.proceed()).thenReturn("ok");
        org.mockito.Mockito.doThrow(new IllegalStateException("audit backend down"))
            .when(auditEventService)
            .saveAll(any());

        aspect.audit(joinPoint);

        assertThat(failureCount("persist")).isEqualTo(1.0);
        assertThat(failureCount()).isEqualTo(1.0);
    }

    @Test
    void testAuditSucceedsDoesNotIncrementFailureCounter() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint);

        assertThat(failureCount()).isEqualTo(0.0);
    }

    @Test
    void testAspectIsOrderedHighestPrecedence() {
        // Ordering is what makes the DENIED path recordable — without HIGHEST_PRECEDENCE, Spring Security's
        // authorization advisor runs first and throws before this aspect's proceed().
        org.springframework.core.annotation.Order order =
            AuditAspect.class.getAnnotation(org.springframework.core.annotation.Order.class);

        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void testNestedCallInsideTransactionIsBufferedUntilTheTransactionIsGone() throws Throwable {
        ProceedingJoinPoint innerJoinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = joinPoint.getSignature();

        when(innerJoinPoint.getSignature()).thenReturn(signature);
        when(innerJoinPoint.proceed()).thenAnswer(invocation -> {
            verify(auditEventService, never()).saveAll(any());

            return "inner";
        });
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            TransactionSynchronizationManager.setActualTransactionActive(true);

            try {
                return aspect.audit(innerJoinPoint);
            } finally {
                verify(auditEventService, never()).saveAll(any());

                TransactionSynchronizationManager.setActualTransactionActive(false);
            }
        });

        assertThat(aspect.audit(joinPoint)).isEqualTo("inner");

        List<PersistentAuditEvent> savedEvents = captureSavedEvents();

        assertThat(savedEvents).hasSize(2);
    }

    @Test
    void testNestedSuccessIsRecordedAsRolledBackWhenTheEnclosingTransactionRollsBack() throws Throwable {
        MethodSignature innerSignature = mock(MethodSignature.class);
        ProceedingJoinPoint innerJoinPoint = mock(ProceedingJoinPoint.class);

        when(innerSignature.getDeclaringTypeName()).thenReturn("InnerService");
        when(innerSignature.getMethod()).thenReturn(getClass().getDeclaredMethod("createSomething"));
        when(innerSignature.getParameterNames()).thenReturn(new String[0]);
        when(innerJoinPoint.getSignature()).thenReturn(innerSignature);
        when(innerJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(innerJoinPoint.proceed()).thenReturn("inner");
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(true);

            aspect.audit(innerJoinPoint);

            for (TransactionSynchronization transactionSynchronization : TransactionSynchronizationManager
                .getSynchronizations()) {

                transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }

            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);

            throw new IllegalStateException("outer failed");
        });

        assertThatThrownBy(() -> aspect.audit(joinPoint)).hasMessage("outer failed");

        assertThat(captureSavedEvents())
            .extracting(persistentAuditEvent -> persistentAuditEvent.getData()
                .get("result"))
            .containsExactlyInAnyOrder("ROLLED_BACK", "ERROR");
    }

    @Test
    void testReadOnlyCallerTransactionIsNotWrittenAndCountsAsLost() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);

        assertThat(aspect.audit(joinPoint)).isEqualTo("ok");

        verify(auditEventService, never()).saveAll(any());

        assertThat(failureCount("read_only")).isEqualTo(1.0);
    }

    @Test
    void testWritableCallerTransactionJoinsItAndCountsRollbackAsLost() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        aspect.audit(joinPoint);

        verify(auditEventService, times(1)).saveAll(any());

        assertThat(failureCount()).isEqualTo(0.0);

        for (TransactionSynchronization transactionSynchronization : TransactionSynchronizationManager
            .getSynchronizations()) {

            transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        assertThat(failureCount("rollback")).isEqualTo(1.0);
    }

    @Test
    void testAuditEventConstructionFailureDoesNotMaskTheBusinessResult() throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        when(signature.getDeclaringTypeName()).thenThrow(new RuntimeException("boom building the audit event"));
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.audit(joinPoint);

        assertThat(result).isEqualTo("ok");
        assertThat(failureCount()).isEqualTo(1.0);
        assertThat(AuditFrame.current()).isNull();
    }

    @Test
    void testOuterEventConstructionFailureStillWritesTheBufferedInnerEvent() throws Throwable {
        MethodSignature outerSignature = (MethodSignature) joinPoint.getSignature();
        MethodSignature innerSignature = mock(MethodSignature.class);
        ProceedingJoinPoint innerJoinPoint = mock(ProceedingJoinPoint.class);

        when(innerSignature.getDeclaringTypeName()).thenReturn("InnerService");
        when(innerSignature.getMethod()).thenReturn(getClass().getDeclaredMethod("createSomething"));
        when(innerSignature.getParameterNames()).thenReturn(new String[0]);
        when(innerJoinPoint.getSignature()).thenReturn(innerSignature);
        when(innerJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(innerJoinPoint.proceed()).thenReturn("inner");
        when(outerSignature.getDeclaringTypeName()).thenThrow(new RuntimeException("boom building the audit event"));
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            TransactionSynchronizationManager.setActualTransactionActive(true);

            try {
                return aspect.audit(innerJoinPoint);
            } finally {
                TransactionSynchronizationManager.setActualTransactionActive(false);
            }
        });

        assertThat(aspect.audit(joinPoint)).isEqualTo("inner");

        assertThat(captureSavedEvent().getData()).containsEntry("method", "InnerService.createSomething");
        assertThat(failureCount("build")).isEqualTo(1.0);
    }

    @AfterEach
    void afterEach() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }

        TransactionSynchronizationManager.setActualTransactionActive(false);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
    }

    private double failureCount() {
        return meterRegistry.find("bytechef_audit_failure")
            .counters()
            .stream()
            .mapToDouble(Counter::count)
            .sum();
    }

    private double failureCount(String reason) {
        Counter counter = meterRegistry.find("bytechef_audit_failure")
            .tag("reason", reason)
            .counter();

        assertThat(counter).isNotNull();

        return counter.count();
    }

    private PersistentAuditEvent captureSavedEvent() {
        List<PersistentAuditEvent> savedEvents = captureSavedEvents();

        assertThat(savedEvents).hasSize(1);

        return savedEvents.getFirst();
    }

    @SuppressWarnings("unchecked")
    private List<PersistentAuditEvent> captureSavedEvents() {
        ArgumentCaptor<List<PersistentAuditEvent>> captor = ArgumentCaptor.forClass(List.class);

        verify(auditEventService).saveAll(captor.capture());

        return captor.getValue();
    }

    @Test
    void testSuccessfulReadIsNotRecorded() throws Throwable {
        useTargetMethod("getSomething");

        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.audit(joinPoint)).isEqualTo("ok");

        verify(auditEventService, never()).saveAll(any());
    }

    @Test
    void testSuccessfulMutationIsRecorded() throws Throwable {
        useTargetMethod("createSomething");

        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint);

        assertThat(captureSavedEvents()).hasSize(1);
    }

    @Test
    void testDeniedReadIsRecorded() throws Throwable {
        useTargetMethod("getSomething");

        when(joinPoint.proceed()).thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> aspect.audit(joinPoint))
            .isInstanceOf(AccessDeniedException.class);

        List<PersistentAuditEvent> savedEvents = captureSavedEvents();

        assertThat(savedEvents).hasSize(1);
        assertThat(savedEvents.getFirst()
            .getData()).containsEntry("result", "DENIED");
    }

    @Test
    void testFailedReadIsRecorded() throws Throwable {
        useTargetMethod("getSomething");

        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> aspect.audit(joinPoint))
            .isInstanceOf(IllegalStateException.class);

        assertThat(captureSavedEvents()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "get", "getSomething", "isAdmin", "hasPermission", "listProjects", "existsById", "countEvents",
        "fetchEventTypes",
        "findAll", "loadWorkflow", "readFile", "searchProjects"
    })
    void testReadStyleMethodNameIsRecognized(String methodName) {
        assertThat(AuditAspect.isReadMethod(methodName)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "getaway", "listen", "listenForEvents", "isolate", "issueToken", "hashPassword", "readmit", "counter",
        "createSomething", "doSomething"
    })
    void testMutationStyleMethodNameIsNotRecognizedAsARead(String methodName) {
        assertThat(AuditAspect.isReadMethod(methodName)).isFalse();
    }

    @Test
    void testUnrecognizedMethodNameIsRecorded() throws Throwable {
        useTargetMethod("doSomething");

        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint);

        assertThat(captureSavedEvents()).hasSize(1);
    }

    private void useTargetMethod(String name) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

        when(methodSignature.getMethod()).thenReturn(getClass().getDeclaredMethod(name));
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void getSomething() {
        // Referenced reflectively to give the mocked MethodSignature a read-style method name.
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void createSomething() {
        // Referenced reflectively to give the mocked MethodSignature a mutation-style method name.
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void doSomething() {
        // Referenced reflectively to give the mocked MethodSignature an unrecognized method name.
    }

    @SuppressWarnings("unused")
    private void dummyTargetMethod() {
        // Referenced reflectively to produce a Method instance for the mocked MethodSignature.
    }

    @Test
    void testAuditedMethodWritesOneDomainEventWithMapperData() throws Throwable {
        useAuditedTargetMethod(42L);

        when(auditMapperResolver.resolve(TestAuditMapper.class)).thenReturn(new TestAuditMapper());
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.audit(joinPoint)).isEqualTo("ok");

        PersistentAuditEvent captured = captureSavedEvent();

        assertThat(captured.getEventType()).isEqualTo("TEST_EVENT");
        assertThat(captured.getData())
            .containsEntry("userId", "42")
            .containsEntry("resultValue", "ok")
            .containsEntry("result", "SUCCESS")
            .containsKey("method")
            .doesNotContainKey("mapperError");
    }

    @Test
    void testAuditedMethodDeniedRecordsDeniedWithoutResult() throws Throwable {
        useAuditedTargetMethod(42L);

        AccessDeniedException accessDeniedException = new AccessDeniedException("denied");

        when(auditMapperResolver.resolve(TestAuditMapper.class)).thenReturn(new TestAuditMapper());
        when(joinPoint.proceed()).thenThrow(accessDeniedException);

        assertThatThrownBy(() -> aspect.audit(joinPoint)).isSameAs(accessDeniedException);

        PersistentAuditEvent captured = captureSavedEvent();

        assertThat(captured.getEventType()).isEqualTo("TEST_EVENT");
        assertThat(captured.getData())
            .containsEntry("result", "DENIED")
            .containsEntry("userId", "42")
            .doesNotContainKey("resultValue");
    }

    @Test
    void testAuditedMethodErrorRecordsErrorClass() throws Throwable {
        useAuditedTargetMethod(42L);

        when(auditMapperResolver.resolve(TestAuditMapper.class)).thenReturn(new TestAuditMapper());
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> aspect.audit(joinPoint)).isInstanceOf(IllegalStateException.class);

        assertThat(captureSavedEvent().getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalStateException.class.getName());
    }

    @Test
    void testThrowingMapperStillWritesTheRowAndCountsTheFailure() throws Throwable {
        useAuditedTargetMethod(42L);

        when(auditMapperResolver.resolve(TestAuditMapper.class)).thenReturn(auditInvocation -> {
            throw new IllegalArgumentException("bad mapper");
        });
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.audit(joinPoint)).isEqualTo("ok");

        assertThat(captureSavedEvent().getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mapperError", IllegalArgumentException.class.getName());
        assertThat(failureCount()).isEqualTo(1.0);
    }

    @Test
    void testMapperLinkageErrorStillWritesTheRowAndKeepsTheBusinessResult() throws Throwable {
        useAuditedTargetMethod(42L);

        when(auditMapperResolver.resolve(TestAuditMapper.class)).thenReturn(auditInvocation -> {
            throw new NoClassDefFoundError("com/example/Missing");
        });
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.audit(joinPoint)).isEqualTo("ok");

        assertThat(captureSavedEvent().getData())
            .containsEntry("mapperError", NoClassDefFoundError.class.getName())
            .containsEntry("result", "SUCCESS");
        assertThat(failureCount("mapper")).isEqualTo(1.0);
    }

    @Test
    void testStackOverflowWhileWritingDoesNotReplaceTheBusinessException() throws Throwable {
        IllegalStateException businessException = new IllegalStateException("business failure");

        when(joinPoint.proceed()).thenThrow(businessException);
        org.mockito.Mockito.doThrow(new StackOverflowError())
            .when(auditEventService)
            .saveAll(any());

        assertThatThrownBy(() -> aspect.audit(joinPoint)).isSameAs(businessException);
    }

    @Test
    void testNullMapperDataIsRecordedAsMapperError() throws Throwable {
        useAuditedTargetMethod(42L);

        when(auditMapperResolver.resolve(TestAuditMapper.class)).thenReturn(auditInvocation -> null);
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint);

        assertThat(captureSavedEvent().getData()).containsEntry("mapperError", "NULL_DATA");
        assertThat(failureCount()).isEqualTo(1.0);
    }

    @Test
    void testMapperCannotOverwriteReservedKeysAndLongKeysAndValuesAreTruncated() throws Throwable {
        useAuditedTargetMethod(42L);

        String longKey = "k".repeat(300);
        String longValue = "v".repeat(299) + "END";

        when(auditMapperResolver.resolve(TestAuditMapper.class))
            .thenReturn(auditInvocation -> Map.of(
                "result", "FORGED", "method", "com.evil.Forged.method", "errorClass", "com.evil.Forged", "mapperError",
                "FORGED_ERROR", longKey, longValue));
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint);

        Map<String, String> data = captureSavedEvent().getData();

        assertThat(data).containsEntry("result", "SUCCESS");
        assertThat(data.get("method")).doesNotContain("com.evil");
        assertThat(data).containsEntry("k".repeat(256), "v".repeat(253) + "END");
        assertThat(data).doesNotContainKey("errorClass");
        assertThat(data).doesNotContainKey("mapperError");
    }

    @Test
    void testPreAuthorizeOnlyMethodStillWritesPermissionCheck() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint);

        PersistentAuditEvent captured = captureSavedEvent();

        assertThat(captured.getEventType()).isEqualTo("PERMISSION_CHECK");
        assertThat(captured.getData()).containsEntry("result", "ALLOWED");
        verify(auditMapperResolver, never()).resolve(any());
    }

    @Test
    void testCapturedValueFromTheFrameReachesTheMapper() throws Throwable {
        useAuditedTargetMethod(42L);

        when(auditMapperResolver.resolve(TestAuditMapper.class)).thenReturn(new TestAuditMapper());
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            AuditFrame auditFrame = AuditFrame.current();

            assertThat(auditFrame).isNotNull();

            auditFrame.setCaptured("VIEWER");

            return "ok";
        });

        aspect.audit(joinPoint);

        assertThat(captureSavedEvent().getData()).containsEntry("captured", "VIEWER");
        assertThat(AuditFrame.current()).isNull();
    }

    @Test
    void testShouldRecordFalseWritesNothingAndStillCleansUpTheFrameAndBuffer() throws Throwable {
        useAuditedTargetMethod(42L);

        when(auditMapperResolver.resolve(TestAuditMapper.class)).thenReturn(new AuditMapper() {

            @Override
            public Map<String, String> map(AuditInvocation auditInvocation) {
                throw new AssertionError("map must not run when shouldRecord returns false");
            }

            @Override
            public boolean shouldRecord(AuditInvocation auditInvocation) {
                return false;
            }
        });
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.audit(joinPoint)).isEqualTo("ok");

        verify(auditEventService, never()).saveAll(any());
        assertThat(AuditFrame.current()).isNull();
    }

    @Test
    void testThrowingShouldRecordStillWritesARowWithMapperError() throws Throwable {
        useAuditedTargetMethod(42L);

        when(auditMapperResolver.resolve(TestAuditMapper.class)).thenReturn(new AuditMapper() {

            @Override
            public Map<String, String> map(AuditInvocation auditInvocation) {
                throw new AssertionError("map must not run when shouldRecord throws");
            }

            @Override
            public boolean shouldRecord(AuditInvocation auditInvocation) {
                throw new IllegalStateException("bad shouldRecord");
            }
        });
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.audit(joinPoint)).isEqualTo("ok");

        assertThat(captureSavedEvent().getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mapperError", IllegalStateException.class.getName());
        assertThat(failureCount()).isEqualTo(1.0);
    }

    @Test
    void testAuditedMethodWithNoSecurityContextRecordsSystemPrincipal() throws Throwable {
        useAuditedTargetMethod(42L);

        when(auditMapperResolver.resolve(TestAuditMapper.class)).thenReturn(new TestAuditMapper());
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint);

        assertThat(captureSavedEvent().getPrincipal()).isEqualTo("SYSTEM");
    }

    @Test
    void testPreAuthorizeOnlyMethodWithNoSecurityContextRecordsMissingContextPrincipal() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint);

        assertThat(captureSavedEvent().getPrincipal()).isEqualTo("__NO_SECURITY_CONTEXT__");
    }

    @Test
    void testTokenPrincipalWithoutUserDetailsIsRecordedByItsName() throws Throwable {
        java.security.Principal servicePrincipal = () -> "service-account";

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(servicePrincipal, null, List.of()));

        try {
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.audit(joinPoint);

            assertThat(captureSavedEvent().getPrincipal()).isEqualTo("service-account");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void testOverLongPrincipalIsTruncatedToFitItsColumn() throws Throwable {
        String principal = "p".repeat(299) + "END";

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        try {
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.audit(joinPoint);

            assertThat(captureSavedEvent().getPrincipal()).isEqualTo("p".repeat(253) + "END");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void useAuditedTargetMethod(long userId) throws NoSuchMethodException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        when(signature.getMethod()).thenReturn(getClass().getDeclaredMethod("auditedTargetMethod", long.class));
        when(signature.getParameterNames()).thenReturn(new String[] {
            "userId"
        });
        when(joinPoint.getArgs()).thenReturn(new Object[] {
            userId
        });
    }

    @Audited(event = "TEST_EVENT", mapper = TestAuditMapper.class)
    @SuppressWarnings("unused")
    private void auditedTargetMethod(long userId) {
        // Referenced reflectively to produce an @Audited Method instance for the mocked MethodSignature.
    }

    static class TestAuditMapper implements AuditMapper {

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            Map<String, String> data = new HashMap<>();

            data.put("userId", String.valueOf(auditInvocation.argument("userId")));

            if (auditInvocation.result() != null) {
                data.put("resultValue", String.valueOf(auditInvocation.result()));
            }

            if (auditInvocation.captured() != null) {
                data.put("captured", String.valueOf(auditInvocation.captured()));
            }

            return data;
        }
    }
}
