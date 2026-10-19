/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.purge;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.tenant.repository.TenantRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import com.bytechef.tenant.domain.TenantDeletion;
import com.bytechef.tenant.service.TenantDeletionService;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.tenant.teardown.TenantTeardownContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Purges tenants whose deletion grace period has elapsed. Deliberately not transactional: a transaction opened in the
 * tenant context would bind its connection to the tenant schema and make contributors that switch to the public tenant
 * reuse that connection. Every step is idempotent, so a purge that crashed midway resumes on the next sweep.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnMultiTenant
@ConditionalOnCoordinator
public class TenantDeletionPurger {

    private static final Logger log = LoggerFactory.getLogger(TenantDeletionPurger.class);

    private final ObjectProvider<Clock> clockProvider;
    private final List<TenantTeardownContributor> tenantTeardownContributors;
    private final TenantDeletionService tenantDeletionService;
    private final TenantRepository tenantRepository;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public TenantDeletionPurger(
        TenantDeletionService tenantDeletionService, TenantRepository tenantRepository, TenantService tenantService,
        List<TenantTeardownContributor> tenantTeardownContributors, ObjectProvider<Clock> clockProvider) {

        this.tenantDeletionService = tenantDeletionService;
        this.tenantRepository = tenantRepository;
        this.tenantService = tenantService;
        this.tenantTeardownContributors = tenantTeardownContributors;
        this.clockProvider = clockProvider;
    }

    @Scheduled(fixedDelayString = "PT15M")
    void purgeDueTenants() {
        Instant now = Instant.now(clockProvider.getIfAvailable(Clock::systemUTC));

        for (TenantDeletion tenantDeletion : tenantDeletionService.getDueForPurge(now)) {
            String tenantId = tenantDeletion.tenantId();

            try {
                purge(tenantId, now);
            } catch (RuntimeException runtimeException) {
                log.error("Purge of tenant {} failed, it stays pending for the next sweep", tenantId, runtimeException);
            }
        }
    }

    private void purge(String tenantId, Instant now) {
        if (!tenantDeletionService.claimForPurge(tenantId, now)) {
            return;
        }

        if (tenantRepository.schemaExists(tenantId)) {
            TenantContext.runWithTenantId(tenantId, () -> SecurityUtils.runAsSystem(() -> {
                rerunDeletionRequested(tenantId);

                for (TenantTeardownContributor tenantTeardownContributor : tenantTeardownContributors) {
                    tenantTeardownContributor.beforeSchemaDrop(tenantId);
                }

                return null;
            }));

            tenantService.deleteTenant(tenantId);
        }

        if (runAfterSchemaDrop(tenantId)) {
            tenantDeletionService.markPurged(tenantId);
        }
    }

    private void rerunDeletionRequested(String tenantId) {
        for (TenantTeardownContributor tenantTeardownContributor : tenantTeardownContributors) {
            try {
                List<String> failures = tenantTeardownContributor.onDeletionRequested(tenantId);

                if (failures != null && !failures.isEmpty()) {
                    log.warn(
                        "Deletion-requested teardown {} reported failures for tenant {} during purge: {}",
                        tenantTeardownContributor.getClass(), tenantId, failures);
                }
            } catch (RuntimeException runtimeException) {
                log.warn(
                    "Deletion-requested teardown {} failed for tenant {} during purge",
                    tenantTeardownContributor.getClass(), tenantId, runtimeException);
            }
        }
    }

    private boolean runAfterSchemaDrop(String tenantId) {
        boolean succeeded = true;

        for (TenantTeardownContributor tenantTeardownContributor : tenantTeardownContributors) {
            try {
                TenantContext.runWithTenantId(
                    TenantContext.DEFAULT_TENANT_ID,
                    () -> SecurityUtils.runAsSystem(() -> {
                        tenantTeardownContributor.afterSchemaDrop(tenantId);

                        return null;
                    }));
            } catch (RuntimeException runtimeException) {
                succeeded = false;

                log.error(
                    "After-drop teardown {} failed for tenant {}", tenantTeardownContributor.getClass(), tenantId,
                    runtimeException);
            }
        }

        return succeeded;
    }
}
