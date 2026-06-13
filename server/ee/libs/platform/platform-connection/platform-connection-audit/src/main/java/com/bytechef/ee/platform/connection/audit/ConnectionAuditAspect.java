/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.audit;

import com.bytechef.ee.platform.audit.SpelAuditEngine;
import com.bytechef.ee.platform.audit.SpelAuditEngine.AuditDataEntry;
import com.bytechef.ee.platform.audit.SpelAuditEngine.AuditSpec;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Aspect that intercepts methods annotated with {@link AuditConnection} and publishes a connection audit event after
 * successful method completion. SpEL expressions in the annotation are evaluated against the method parameters and
 * return value.
 *
 * <p>
 * All shared SpEL/audit machinery lives in {@link SpelAuditEngine}; this aspect keeps only its concrete-annotation
 * pointcuts and translates {@link AuditConnection} into the engine's neutral {@link AuditSpec}/{@link AuditDataEntry}
 * shapes plus a publish callback delegating to {@link ConnectionAuditPublisher}.
 *
 * <p>
 * When {@link AuditConnection#establishCorrelation()} is {@code true}, the {@link #establishCorrelation} advice opens a
 * {@code AuditCorrelation} scope for the duration of the method invocation. Any nested audited methods that fire during
 * that scope inherit the same correlation ID in their emitted event data, letting consumers reassemble the parent/child
 * audit relationship without relying on temporal proximity.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Aspect
@Component
@ConditionalOnEEVersion
@SuppressFBWarnings({
    "CT_CONSTRUCTOR_THROW", "SPEL_INJECTION"
})
public class ConnectionAuditAspect {

    private static final String AUDIT_FAILURE_COUNTER = "bytechef_connection_audit_failed";
    private static final String VALIDATION_SKIPPED_COUNTER = "bytechef_connection_audit_validation_skipped";
    private static final String VALIDATION_SKIPPED_DESCRIPTION =
        "Beans whose @AuditConnection SpEL could not be validated at boot because the "
            + "bean failed to resolve during context refresh";

    private final ConnectionAuditPublisher connectionAuditPublisher;
    private final SpelAuditEngine spelAuditEngine;

    @SuppressFBWarnings("EI")
    public ConnectionAuditAspect(
        ApplicationContext applicationContext, ConnectionAuditPublisher connectionAuditPublisher,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.connectionAuditPublisher = connectionAuditPublisher;
        this.spelAuditEngine = new SpelAuditEngine(applicationContext, meterRegistryProvider);
    }

    /**
     * Boot-time validation of every {@code @AuditConnection} SpEL expression in the context. Delegates to
     * {@link SpelAuditEngine#validate}; the {@code @EventListener} stays here because it lives on the bean.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void validateAuditAnnotations() {
        spelAuditEngine.validate(
            AuditConnection.class, ConnectionAuditAspect::expressionsOf, "ConnectionAuditAspect",
            VALIDATION_SKIPPED_COUNTER, VALIDATION_SKIPPED_DESCRIPTION);
    }

    private static List<String> expressionsOf(Method method) {
        AuditConnection annotation = method.getAnnotation(AuditConnection.class);

        List<String> expressions = new ArrayList<>();

        expressions.add(annotation.connectionId());

        for (AuditConnection.AuditData auditData : annotation.data()) {
            expressions.add(auditData.value());
        }

        return expressions;
    }

    /**
     * Opens a {@code AuditCorrelation} scope before the audited method runs when
     * {@link AuditConnection#establishCorrelation()} is set. Delegates the push/proceed/pop to the engine.
     */
    @Around("@annotation(auditConnection)")
    public Object establishCorrelation(ProceedingJoinPoint joinPoint, AuditConnection auditConnection)
        throws Throwable {

        return spelAuditEngine.runWithCorrelation(joinPoint, auditConnection.establishCorrelation());
    }

    /**
     * Captures audit state at method-return time and defers the actual publish to {@code afterCommit} when a
     * transaction is active. Delegates to {@link SpelAuditEngine#audit}; the strict-audit rethrow, afterCommit
     * deferral, and failure-metric handling all live in the engine.
     */
    @AfterReturning(pointcut = "@annotation(auditConnection)", returning = "result")
    public void audit(JoinPoint joinPoint, AuditConnection auditConnection, Object result) {
        List<AuditDataEntry> dataEntries = new ArrayList<>();

        for (AuditConnection.AuditData entry : auditConnection.data()) {
            dataEntries.add(new AuditDataEntry(entry.key(), entry.value()));
        }

        ConnectionAuditEvent event = auditConnection.event();

        AuditSpec spec = new AuditSpec(
            dataEntries, auditConnection.connectionId(), event.name(), event.isStrictAudit(), AUDIT_FAILURE_COUNTER,
            (connectionId, data) -> connectionAuditPublisher.publish(
                event, Objects.requireNonNull(connectionId), data));

        spelAuditEngine.audit(joinPoint, result, spec);
    }
}
