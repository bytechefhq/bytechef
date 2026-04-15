/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.connection.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Unit tests for {@link ConnectionAuditAspect}. Exercises the SpEL evaluation path directly by invoking the advice
 * method with a hand-built {@link JoinPoint}; this lets us verify {@code @beanName.method()} lookups work without
 * spinning up a Spring test context, and that SpEL failures do not propagate through to the audited method.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ConnectionAuditAspectTest {

    @Mock
    private ConnectionAuditPublisher connectionAuditPublisher;

    private ConnectionAuditAspect aspect;
    private StaticApplicationContext applicationContext;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        applicationContext = new StaticApplicationContext();
        applicationContext.refresh();

        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> emptyProvider = mock(ObjectProvider.class);

        when(emptyProvider.getIfAvailable()).thenReturn(null);

        aspect = new ConnectionAuditAspect(applicationContext, connectionAuditPublisher, emptyProvider);
    }

    @AfterEach
    void tearDown() {
        // Tests that activate TransactionSynchronizationManager must clear it so pooled JUnit
        // threads do not leak synchronization state into the next test.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void testAuditPublishesEventWithResolvedIdAndData() throws NoSuchMethodException {
        // connectionId resolves from the method argument via SpEL; the data map receives a
        // literal string value to prove SpEL expression evaluation runs over the annotation attrs.
        AuditConnection auditAnnotation = auditAnnotation("#arg0", "status", "'ACTIVE'");

        Method method = SampleService.class.getMethod("byId", long.class);
        JoinPoint joinPoint = joinPoint(method, new Object[] {
            42L
        });

        aspect.audit(joinPoint, auditAnnotation, 42L);

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(connectionAuditPublisher).publish(eq(ConnectionAuditEvent.CONNECTION_CREATED), eq(42L),
            dataCaptor.capture());

        assertThat(dataCaptor.getValue()).containsEntry("status", "ACTIVE");
    }

    @Test
    void testAuditSpelFailureIsLoggedNotPropagated() throws NoSuchMethodException {
        // The audited method returned successfully; a broken SpEL expression must not rethrow.
        // Before the try/catch split, a SpEL exception would propagate out of @AfterReturning and
        // corrupt whatever transactional state the caller was finalizing.
        AuditConnection auditAnnotation = auditAnnotation("invalid SpEL!!!");

        Method method = SampleService.class.getMethod("byId", long.class);
        JoinPoint joinPoint = joinPoint(method, new Object[] {
            42L
        });

        aspect.audit(joinPoint, auditAnnotation, 42L);

        verify(connectionAuditPublisher, never()).publish(any(), anyLong(), any());
    }

    @Test
    void testAuditPublishFailureIsCaughtSoAuditDoesNotBreakAuditedMethod() throws NoSuchMethodException {
        // A broken publisher must not escape the aspect — the audited mutation has already committed.
        AuditConnection auditAnnotation = auditAnnotation("#arg0");

        Method method = SampleService.class.getMethod("byId", long.class);
        JoinPoint joinPoint = joinPoint(method, new Object[] {
            42L
        });

        doThrow(new RuntimeException("listener blew up"))
            .when(connectionAuditPublisher)
            .publish(any(), anyLong(), any());

        // Should not throw.
        aspect.audit(joinPoint, auditAnnotation, 42L);
    }

    @Test
    void testAuditSpelFailureThrowsForStrictAuditEvents() throws NoSuchMethodException {
        // CONNECTION_DELETED / DEMOTED / REASSIGNED / REVOKED are privilege-narrowing compliance events:
        // a missing audit trail is a compliance-grade regression, so an SpEL-evaluation failure must
        // propagate (rolling back the surrounding @Transactional boundary) rather than being absorbed
        // into the audit-failure metric. CONNECTION_CREATED remains best-effort (covered by
        // testAuditSpelFailureIsLoggedNotPropagated).
        AuditConnection auditAnnotation =
            strictAuditAnnotation("invalid SpEL!!!", ConnectionAuditEvent.CONNECTION_DELETED);

        Method method = SampleService.class.getMethod("byId", long.class);
        JoinPoint joinPoint = joinPoint(method, new Object[] {
            42L
        });

        assertThatThrownBy(() -> aspect.audit(joinPoint, auditAnnotation, 42L))
            .isInstanceOf(AuditCaptureFailedException.class)
            .hasMessageContaining("CONNECTION_DELETED");

        verify(connectionAuditPublisher, never()).publish(any(), anyLong(), any());
    }

    @Test
    void testAuditErrorPropagatesPerJvmContract() throws NoSuchMethodException {
        // Error subtypes (OOM, StackOverflowError, AssertionError, VirtualMachineError) must NOT be
        // absorbed by the aspect — the JVM contract is that Error signals unrecoverable conditions and
        // must propagate. Swallowing them here would let the advised method return success while the
        // JVM is in an unrecoverable state. The aspect catches Exception (not Throwable) so only
        // RuntimeException-family failures are absorbed into the audit-failure metric.
        AuditConnection auditAnnotation = auditAnnotation("#arg0");

        Method method = SampleService.class.getMethod("byId", long.class);
        JoinPoint joinPoint = joinPoint(method, new Object[] {
            42L
        });

        doThrow(new AssertionError("listener AssertionError"))
            .when(connectionAuditPublisher)
            .publish(any(), anyLong(), any());

        assertThatThrownBy(() -> aspect.audit(joinPoint, auditAnnotation, 42L))
            .isInstanceOf(AssertionError.class)
            .hasMessage("listener AssertionError");
    }

    @Test
    void testAuditAutoAttachesCorrelationIdFromThreadLocal() throws NoSuchMethodException {
        // When an umbrella method has opened a correlation scope (e.g. setConnectionProjects),
        // every nested audited mutation must inherit the correlation ID in its event data so
        // downstream consumers can reassemble the parent/child audit relationship. The aspect
        // consults AuditCorrelation.current() and injects the ID under "correlationId".
        AuditConnection auditAnnotation = auditAnnotation("#arg0");

        Method method = SampleService.class.getMethod("byId", long.class);
        JoinPoint joinPoint = joinPoint(method, new Object[] {
            42L
        });

        String expectedCorrelationIdValue = "11111111-2222-3333-4444-555555555555";
        AuditCorrelation.CorrelationId previous = AuditCorrelation.push(
            new AuditCorrelation.CorrelationId(expectedCorrelationIdValue));

        try {
            aspect.audit(joinPoint, auditAnnotation, 42L);
        } finally {
            AuditCorrelation.pop(previous);
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(connectionAuditPublisher).publish(
            eq(ConnectionAuditEvent.CONNECTION_CREATED), eq(42L), dataCaptor.capture());

        assertThat(dataCaptor.getValue()).containsEntry("correlationId", expectedCorrelationIdValue);
    }

    @Test
    void testAuditDoesNotOverrideExplicitCorrelationIdFromAnnotation() throws NoSuchMethodException {
        // The auto-attach is putIfAbsent, so a caller can still override by supplying an explicit
        // @AuditData entry. This test pins that precedence: an annotation-provided correlationId
        // is not clobbered by the ThreadLocal.
        AuditConnection auditAnnotation = auditAnnotation(
            "#arg0", "correlationId", "'annotation-wins'");

        Method method = SampleService.class.getMethod("byId", long.class);
        JoinPoint joinPoint = joinPoint(method, new Object[] {
            42L
        });

        AuditCorrelation.CorrelationId previous = AuditCorrelation.push(
            new AuditCorrelation.CorrelationId("thread-local-loser"));

        try {
            aspect.audit(joinPoint, auditAnnotation, 42L);
        } finally {
            AuditCorrelation.pop(previous);
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(connectionAuditPublisher).publish(
            eq(ConnectionAuditEvent.CONNECTION_CREATED), eq(42L), dataCaptor.capture());

        assertThat(dataCaptor.getValue()).containsEntry("correlationId", "annotation-wins");
    }

    // Builds a minimal AuditConnection stub for the aspect to interpret. keyValuePairs must be
    // key/value pairs (even length); an odd trailing element is ignored to keep callers simple.
    private static AuditConnection auditAnnotation(String connectionIdExpression, String... keyValuePairs) {
        int pairCount = keyValuePairs.length / 2;

        AuditConnection.AuditData[] data = new AuditConnection.AuditData[pairCount];

        for (int pairIndex = 0; pairIndex < pairCount; pairIndex++) {
            String keyValue = keyValuePairs[pairIndex * 2];
            String valueExpression = keyValuePairs[pairIndex * 2 + 1];

            data[pairIndex] = new AuditConnection.AuditData() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return AuditConnection.AuditData.class;
                }

                @Override
                public String key() {
                    return keyValue;
                }

                @Override
                public String value() {
                    return valueExpression;
                }
            };
        }

        return new AuditConnection() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return AuditConnection.class;
            }

            @Override
            public ConnectionAuditEvent event() {
                return ConnectionAuditEvent.CONNECTION_CREATED;
            }

            @Override
            public String connectionId() {
                return connectionIdExpression;
            }

            @Override
            public AuditConnection.AuditData[] data() {
                return data;
            }

            @Override
            public boolean establishCorrelation() {
                return false;
            }
        };
    }

    private static AuditConnection strictAuditAnnotation(String connectionIdExpression, ConnectionAuditEvent event) {
        return new AuditConnection() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return AuditConnection.class;
            }

            @Override
            public ConnectionAuditEvent event() {
                return event;
            }

            @Override
            public String connectionId() {
                return connectionIdExpression;
            }

            @Override
            public AuditConnection.AuditData[] data() {
                return new AuditConnection.AuditData[0];
            }

            @Override
            public boolean establishCorrelation() {
                return false;
            }
        };
    }

    private static JoinPoint joinPoint(Method method, Object[] args) {
        MethodSignature methodSignature = mock(MethodSignature.class);

        when(methodSignature.getMethod()).thenReturn(method);

        JoinPoint joinPoint = mock(JoinPoint.class);

        when(joinPoint.getSignature()).thenReturn((Signature) methodSignature);
        when(joinPoint.getArgs()).thenReturn(args);

        return joinPoint;
    }

    @Test
    void testEstablishCorrelationPropagatesSameIdToUmbrellaAndNestedAudits() throws Throwable {
        // End-to-end contract for setConnectionProjects-style audits: an umbrella method that opens
        // a correlation scope must wrap its own audit AND every nested audit that fires inside
        // proceed() with the same correlation id. Regression guard for `establishCorrelation = true`
        // being accidentally removed — the aspect fires but children no longer inherit the parent id
        // and consumers can't reassemble the batch.
        AuditConnection umbrella = auditAnnotationWithCorrelation(true);
        AuditConnection child = auditAnnotation("#arg0");

        Method method = SampleService.class.getMethod("byId", long.class);
        JoinPoint afterReturningPoint = joinPoint(method, new Object[] {
            42L
        });

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);

        when(pjp.proceed()).thenAnswer(invocation -> {
            // Simulate the full Spring AOP interception order inside the @Around scope: the method
            // body runs (firing nested @AfterReturning audits), then the umbrella's own
            // @AfterReturning fires — all while the correlation ThreadLocal is still pushed.
            aspect.audit(afterReturningPoint, child, "row-1");
            aspect.audit(afterReturningPoint, child, "row-2");
            aspect.audit(afterReturningPoint, umbrella, "umbrella");

            return null;
        });

        aspect.establishCorrelation(pjp, umbrella);

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(connectionAuditPublisher, org.mockito.Mockito.times(3))
            .publish(any(), anyLong(), dataCaptor.capture());

        Map<String, Object> firstEvent = dataCaptor.getAllValues()
            .get(0);

        assertThat(firstEvent.get("correlationId"))
            .asString()
            .isNotBlank();

        Object parentId = firstEvent.get("correlationId");

        for (Map<String, Object> event : dataCaptor.getAllValues()) {
            assertThat(event.get("correlationId")).isEqualTo(parentId);
        }
    }

    private static AuditConnection auditAnnotationWithCorrelation(boolean establishCorrelation) {
        return new AuditConnection() {

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return AuditConnection.class;
            }

            @Override
            public ConnectionAuditEvent event() {
                return ConnectionAuditEvent.CONNECTION_CREATED;
            }

            @Override
            public String connectionId() {
                return "#arg0";
            }

            @Override
            public AuditConnection.AuditData[] data() {
                return new AuditConnection.AuditData[0];
            }

            @Override
            public boolean establishCorrelation() {
                return establishCorrelation;
            }
        };
    }

    @Test
    void testAuditDefersPublishToAfterCommitWhenTransactionIsActive() throws NoSuchMethodException {
        // Regression guard for the @Transactional-rollback-vs-audit-drift bug: when a transaction
        // is active, the aspect MUST register a TransactionSynchronization and publish inside
        // afterCommit — not inline. Publishing inline leaves the audit trail claiming a mutation
        // that a subsequent outer rollback reverts.
        AuditConnection auditAnnotation = auditAnnotation("#arg0");

        Method method = SampleService.class.getMethod("byId", long.class);
        JoinPoint joinPoint = joinPoint(method, new Object[] {
            42L
        });

        TransactionSynchronizationManager.initSynchronization();

        try {
            aspect.audit(joinPoint, auditAnnotation, 42L);

            // Inline publish must NOT have fired — it is registered for afterCommit only.
            verify(connectionAuditPublisher, never()).publish(any(), anyLong(), any());

            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

            TransactionSynchronizationManager.getSynchronizations()
                .get(0)
                .afterCommit();

            verify(connectionAuditPublisher).publish(eq(ConnectionAuditEvent.CONNECTION_CREATED), eq(42L), any());
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void testAuditDoesNotEmitWhenAfterCommitIsNotInvoked() throws NoSuchMethodException {
        // The afterCommit callback is the only emission site when a transaction is active. A
        // rollback path never calls afterCommit — so a simulated rollback (we register the
        // synchronization but never invoke afterCommit) must leave the publisher untouched.
        AuditConnection auditAnnotation = auditAnnotation("#arg0");

        Method method = SampleService.class.getMethod("byId", long.class);
        JoinPoint joinPoint = joinPoint(method, new Object[] {
            42L
        });

        TransactionSynchronizationManager.initSynchronization();

        try {
            aspect.audit(joinPoint, auditAnnotation, 42L);

            // Simulate rollback by invoking afterCompletion with STATUS_ROLLED_BACK instead of afterCommit.
            TransactionSynchronizationManager.getSynchronizations()
                .get(0)
                .afterCompletion(
                    org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(connectionAuditPublisher, never()).publish(any(), anyLong(), any());
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    public static class SampleService {
        public long byId(long arg0) {
            return arg0;
        }
    }
}
