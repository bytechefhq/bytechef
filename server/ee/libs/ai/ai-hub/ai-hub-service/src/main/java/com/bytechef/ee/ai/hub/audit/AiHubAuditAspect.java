/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.audit;

import com.bytechef.ee.platform.audit.SpelAuditEngine;
import com.bytechef.ee.platform.audit.SpelAuditEngine.AuditDataEntry;
import com.bytechef.ee.platform.audit.SpelAuditEngine.AuditSpec;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
 * Aspect that intercepts {@link AuditAiHub}-annotated methods, evaluates the data SpEL, and publishes an
 * {@link AiHubAuditEvent} via {@link AiHubAuditPublisher}. Mirrors {@code ConnectionAuditAspect}: all shared machinery
 * (boot-time SpEL validation, {@code afterCommit} publish, {@code strictAudit} rethrow, {@code establishCorrelation}
 * ThreadLocal scope) lives in {@link SpelAuditEngine}. This aspect keeps only its concrete-annotation pointcuts and
 * translates {@link AuditAiHub} into the engine's neutral {@link AuditSpec}.
 *
 * <p>
 * Unlike {@code @AuditConnection}, {@code @AuditAiHub} has no subject-id slot, so the spec's connection-id expression
 * is {@code null} and the publish callback ignores the (always-null) resolved id.
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
public class AiHubAuditAspect {

    private static final String AUDIT_FAILURE_COUNTER = "bytechef_ai_hub_audit_failed";
    private static final String VALIDATION_SKIPPED_COUNTER = "bytechef_ai_hub_audit_validation_skipped";
    private static final String VALIDATION_SKIPPED_DESCRIPTION =
        "Beans whose @AuditAiHub SpEL could not be validated at boot because the bean failed to "
            + "resolve during context refresh";

    private final AiHubAuditPublisher publisher;
    private final SpelAuditEngine spelAuditEngine;

    @SuppressFBWarnings("EI")
    public AiHubAuditAspect(
        ApplicationContext applicationContext, AiHubAuditPublisher publisher,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.publisher = publisher;
        this.spelAuditEngine = new SpelAuditEngine(applicationContext, meterRegistryProvider);
    }

    /**
     * Boot-time validation of every {@code @AuditAiHub} SpEL expression in the context. Delegates to
     * {@link SpelAuditEngine#validate}; the {@code @EventListener} stays here because it lives on the bean.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void validateAuditAnnotations() {
        spelAuditEngine.validate(
            AuditAiHub.class, AiHubAuditAspect::expressionsOf, "AiHubAuditAspect",
            VALIDATION_SKIPPED_COUNTER, VALIDATION_SKIPPED_DESCRIPTION);
    }

    private static List<String> expressionsOf(Method method) {
        AuditAiHub annotation = method.getAnnotation(AuditAiHub.class);

        List<String> expressions = new ArrayList<>();

        for (AuditAiHub.AuditData auditData : annotation.data()) {
            expressions.add(auditData.value());
        }

        return expressions;
    }

    @Around("@annotation(auditAiHub)")
    public Object establishCorrelation(ProceedingJoinPoint joinPoint, AuditAiHub auditAiHub) throws Throwable {
        return spelAuditEngine.runWithCorrelation(joinPoint, auditAiHub.establishCorrelation());
    }

    @AfterReturning(pointcut = "@annotation(auditAiHub)", returning = "result")
    public void audit(JoinPoint joinPoint, AuditAiHub auditAiHub, Object result) {
        List<AuditDataEntry> dataEntries = new ArrayList<>();

        for (AuditAiHub.AuditData entry : auditAiHub.data()) {
            dataEntries.add(new AuditDataEntry(entry.key(), entry.value()));
        }

        AiHubAuditEvent event = auditAiHub.event();

        AuditSpec spec = new AuditSpec(
            dataEntries, null, event.name(), event.isStrictAudit(), AUDIT_FAILURE_COUNTER,
            (connectionId, data) -> publisher.publish(event, data));

        spelAuditEngine.audit(joinPoint, result, spec);
    }
}
