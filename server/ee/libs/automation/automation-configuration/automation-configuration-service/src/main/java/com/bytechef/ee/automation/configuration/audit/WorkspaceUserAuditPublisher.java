/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

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
 * Publishes workspace-user audit events through Spring Boot's actuator audit bus. Mirrors {@code AiHubAuditPublisher} —
 * the Spring Boot listener already wired in EE picks the {@link AuditApplicationEvent} up and persists it via
 * {@code CustomAuditEventRepository} into {@code persistent_audit_event}.
 *
 * <p>
 * Failures absorb silently into {@code bytechef_workspace_user_audit_failed} + an error log; emission must never break
 * the just-succeeded business transaction. Unauthenticated callers get the {@code "SYSTEM"} principal fallback.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class WorkspaceUserAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceUserAuditPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final @Nullable Counter auditFailureCounter;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public WorkspaceUserAuditPublisher(
        ApplicationEventPublisher applicationEventPublisher,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.applicationEventPublisher = applicationEventPublisher;

        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        this.auditFailureCounter = meterRegistry == null ? null
            : Counter.builder("bytechef_workspace_user_audit_failed")
                .description(
                    "Workspace-user audit events that failed to publish. Non-zero values indicate a gap in the audit "
                        + "trail.")
                .register(meterRegistry);
    }

    public void publish(WorkspaceUserAuditEvent event, @Nullable Map<String, Object> data) {
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
            // committed by the time publishers run. Drift is observable via bytechef_workspace_user_audit_failed.
            if (auditFailureCounter != null) {
                auditFailureCounter.increment();
            }

            log.error(
                "Failed to publish workspace-user audit event {} (data={})",
                event, data, exception);
        }
    }
}
