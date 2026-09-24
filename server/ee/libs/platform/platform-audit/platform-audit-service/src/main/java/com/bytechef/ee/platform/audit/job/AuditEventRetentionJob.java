/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.job;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/**
 * Deletes audit events older than {@code bytechef.audit.retention-days} to reclaim space and limit growth of
 * {@code persistent_audit_event}. Scheduled by {@code bytechef.audit.retention-cron}, evaluated in the JVM default time
 * zone. A retention of zero or fewer days disables the purge rather than deleting the whole audit trail.
 *
 * <p>
 * The audit tables live in each tenant's schema, and a scheduler thread carries no tenant, so with multi-tenancy
 * enabled the purge runs once per tenant. A failure in one tenant is logged and does not stop the others.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AuditEventRetentionJob implements SchedulingConfigurer {

    static final String PURGED_METRIC = "bytechef_audit_retention_purged";
    static final String FAILURE_METRIC = "bytechef_audit_retention_failure";

    private static final Logger log = LoggerFactory.getLogger(AuditEventRetentionJob.class);

    private final Counter failureCounter;
    private final PersistenceAuditEventRepository persistenceAuditEventRepository;
    private final Counter purgedCounter;
    private final String retentionCron;
    private final long retentionDays;
    private final TenantService tenantService;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public AuditEventRetentionJob(
        ApplicationProperties applicationProperties, ObjectProvider<MeterRegistry> meterRegistryProvider,
        PersistenceAuditEventRepository persistenceAuditEventRepository, TenantService tenantService) {

        ApplicationProperties.Audit audit = applicationProperties.getAudit();
        MeterRegistry meterRegistry = Objects.requireNonNullElseGet(
            meterRegistryProvider.getIfAvailable(), SimpleMeterRegistry::new);

        this.failureCounter = Counter.builder(FAILURE_METRIC)
            .description("Number of audit retention purges that failed, per tenant")
            .register(meterRegistry);
        this.purgedCounter = Counter.builder(PURGED_METRIC)
            .description("Number of audit events deleted by the retention job")
            .register(meterRegistry);

        this.persistenceAuditEventRepository = persistenceAuditEventRepository;
        this.retentionCron = audit.getRetentionCron();
        this.retentionDays = audit.getRetentionDays();
        this.tenantService = tenantService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        scheduledTaskRegistrar.addCronTask(this::purgeExpiredEvents, retentionCron);
    }

    public void purgeExpiredEvents() {
        if (retentionDays <= 0) {
            log.warn("Audit retention is disabled: bytechef.audit.retention-days is {}", retentionDays);

            return;
        }

        LocalDateTime cutoff = LocalDateTime.now()
            .minusDays(retentionDays);

        if (!tenantService.isMultiTenantEnabled()) {
            purgeTenantEvents(cutoff, null);

            return;
        }

        for (String tenantId : tenantService.getTenantIds()) {
            purgeTenantEvents(cutoff, tenantId);
        }
    }

    private void purgeTenantEvents(LocalDateTime cutoff, @Nullable String tenantId) {
        try {
            if (tenantId == null) {
                purgeEventsBefore(cutoff);
            } else {
                TenantContext.runWithTenantId(tenantId, () -> purgeEventsBefore(cutoff));
            }
        } catch (RuntimeException | LinkageError | StackOverflowError throwable) {
            failureCounter.increment();

            log.error("Audit retention failed for tenant {}", tenantId, throwable);
        }
    }

    private void purgeEventsBefore(LocalDateTime cutoff) {
        int deleted = persistenceAuditEventRepository.deleteByEventDateBefore(cutoff);

        purgedCounter.increment(deleted);

        if (deleted > 0) {
            log.info(
                "Audit retention: deleted {} event(s) older than {} in tenant {}", deleted, cutoff,
                TenantContext.getCurrentTenantId());
        } else {
            log.debug("Audit retention: no events older than {} in tenant {}", cutoff,
                TenantContext.getCurrentTenantId());
        }
    }
}
