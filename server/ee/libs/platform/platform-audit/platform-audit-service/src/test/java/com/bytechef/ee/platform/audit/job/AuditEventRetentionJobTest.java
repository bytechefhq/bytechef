/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AuditEventRetentionJobTest {

    private final PersistenceAuditEventRepository persistenceAuditEventRepository =
        mock(PersistenceAuditEventRepository.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final TenantService tenantService = mock(TenantService.class);

    @Test
    void testPurgeExpiredEventsPassesCutoffToRepository() {
        when(persistenceAuditEventRepository.deleteByEventDateBefore(any())).thenReturn(3);

        LocalDateTime before = LocalDateTime.now()
            .minusDays(30);

        createJob(30L, "0 0 2 * * *").purgeExpiredEvents();

        ArgumentCaptor<LocalDateTime> cutoffArgumentCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(persistenceAuditEventRepository).deleteByEventDateBefore(cutoffArgumentCaptor.capture());

        assertThat(cutoffArgumentCaptor.getValue()).isBetween(before.minusMinutes(1), before.plusMinutes(1));
    }

    @Test
    void testPurgeExpiredEventsIsDisabledWhenRetentionDaysIsNotPositive() {
        createJob(0L, "0 0 2 * * *").purgeExpiredEvents();
        createJob(-1L, "0 0 2 * * *").purgeExpiredEvents();

        verify(persistenceAuditEventRepository, never()).deleteByEventDateBefore(any());
    }

    @Test
    void testPurgeExpiredEventsRunsOncePerTenantWhenMultiTenant() {
        List<String> purgedTenantIds = new ArrayList<>();

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIds()).thenReturn(List.of("tenant_a", "tenant_b", "tenant_c"));
        when(persistenceAuditEventRepository.deleteByEventDateBefore(any())).thenAnswer(invocation -> {
            String tenantId = TenantContext.getCurrentTenantId();

            purgedTenantIds.add(tenantId);

            if ("tenant_b".equals(tenantId)) {
                throw new NoClassDefFoundError("org/postgresql/Missing");
            }

            return 1;
        });

        createJob(30L, "0 0 2 * * *").purgeExpiredEvents();

        verify(persistenceAuditEventRepository, times(3)).deleteByEventDateBefore(any());

        assertThat(purgedTenantIds).containsExactly("tenant_a", "tenant_b", "tenant_c");
        assertThat(counterValue(AuditEventRetentionJob.FAILURE_METRIC)).isEqualTo(1.0);
        assertThat(counterValue(AuditEventRetentionJob.PURGED_METRIC)).isEqualTo(2.0);
    }

    @Test
    void testSingleTenantPurgeFailureIsCountedAndDoesNotPropagate() {
        when(persistenceAuditEventRepository.deleteByEventDateBefore(any()))
            .thenThrow(new IllegalStateException("database unavailable"));

        createJob(30L, "0 0 2 * * *").purgeExpiredEvents();

        assertThat(counterValue(AuditEventRetentionJob.FAILURE_METRIC)).isEqualTo(1.0);
    }

    @Test
    void testConfigureTasksRegistersConfiguredCron() {
        ScheduledTaskRegistrar scheduledTaskRegistrar = new ScheduledTaskRegistrar();

        createJob(30L, "0 30 3 * * *").configureTasks(scheduledTaskRegistrar);

        List<CronTask> cronTasks = scheduledTaskRegistrar.getCronTaskList();

        assertThat(cronTasks).hasSize(1);
        assertThat(cronTasks.getFirst()
            .getExpression()).isEqualTo("0 30 3 * * *");
    }

    private AuditEventRetentionJob createJob(long retentionDays, String retentionCron) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Audit audit = applicationProperties.getAudit();

        audit.setRetentionCron(retentionCron);
        audit.setRetentionDays(retentionDays);

        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        return new AuditEventRetentionJob(
            applicationProperties, meterRegistryProvider, persistenceAuditEventRepository, tenantService);
    }

    private double counterValue(String metricName) {
        Counter counter = meterRegistry.find(metricName)
            .counter();

        assertThat(counter).isNotNull();

        return counter.count();
    }
}
