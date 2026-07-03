/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.audit;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes AI Hub audit events through Spring Boot's actuator audit bus. Mirrors {@code ConnectionAuditPublisher} —
 * the Spring Boot listener already wired in EE picks the {@link AuditApplicationEvent} up and persists it via
 * {@code CustomAuditEventRepository} into {@code persistent_audit_event}.
 *
 * <p>
 * Failures absorb silently into {@code bytechef_ai_hub_audit_failed} + a warn log; emission must never break the
 * just-succeeded business transaction. Unauthenticated callers (Quartz scheduled-fire thread) get the {@code "SYSTEM"}
 * principal fallback so the row still records who would have been credited if a user had initiated the action.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AiHubAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(AiHubAuditPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final @Nullable Counter auditFailureCounter;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public AiHubAuditPublisher(
        ApplicationEventPublisher applicationEventPublisher,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.applicationEventPublisher = applicationEventPublisher;

        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        this.auditFailureCounter = meterRegistry == null ? null : Counter.builder("bytechef_ai_hub_audit_failed")
            .description(
                "AI Hub audit events that failed to publish. Non-zero values indicate a gap in the audit trail.")
            .register(meterRegistry);
    }

    public void publish(AiHubAuditEvent event, @Nullable Map<String, Object> data) {
        try {
            String principal;

            try {
                principal = SecurityUtils.fetchCurrentUserLogin()
                    .orElse("SYSTEM");
            } catch (RuntimeException securityException) {
                log.warn(
                    "Could not resolve principal for audit event {}, using SYSTEM",
                    event, securityException);

                principal = "SYSTEM";
            }

            Map<String, Object> dataCopy = new HashMap<>();

            if (data != null) {
                dataCopy.putAll(data);
            }

            AuditEvent auditEvent = new AuditEvent(principal, event.name(), dataCopy);

            applicationEventPublisher.publishEvent(new AuditApplicationEvent(auditEvent));
        } catch (Exception exception) {
            // Catch Exception (not Throwable): Error subtypes (OOM, StackOverflowError) must propagate per the JVM
            // contract. Audit emission must never throw out to the caller — the business transaction has already
            // committed by the time publishers run (afterCommit path) or is irrelevant to the audit attempt (Quartz
            // path). Drift is observable via bytechef_ai_hub_audit_failed.
            if (auditFailureCounter != null) {
                auditFailureCounter.increment();
            }

            log.error(
                "Failed to publish AI Hub audit event {} (data={})",
                event, data, exception);
        }
    }
}
