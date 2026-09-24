/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.SharedApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class JobStatusAuditListenerTest {

    private final AuditEventService auditEventService = mock(AuditEventService.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private JobStatusAuditListener jobStatusAuditListener;

    @BeforeEach
    void beforeEach() {
        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        jobStatusAuditListener = new JobStatusAuditListener(auditEventService, meterRegistryProvider);
    }

    @ParameterizedTest
    @CsvSource({
        "CREATED,JOB_CREATED", "STARTED,JOB_STARTED", "COMPLETED,JOB_COMPLETED", "FAILED,JOB_FAILED",
        "STOPPED,JOB_STOPPED"
    })
    void testJobStatusIsRecordedAsItsOwnEventType(String status, String expectedEventType) {
        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.valueOf(status)));

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(expectedEventType);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("SYSTEM");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("jobId", "42")
            .containsEntry("status", status);
        assertThat(persistentAuditEvent.getEventDate()).isNotNull();
    }

    @Test
    void testListenerIsSharedWithSynchronousJobExecutors() {
        assertThat(jobStatusAuditListener).isInstanceOf(SharedApplicationEventListener.class);
    }

    @Test
    void testUnrelatedEventIsIgnored() {
        jobStatusAuditListener.onApplicationEvent(mock(com.bytechef.atlas.coordinator.event.ApplicationEvent.class));

        verify(auditEventService, never()).saveAll(any());
    }

    @Test
    void testPersistenceFailureIsCountedAndDoesNotPropagate() {
        doThrow(new IllegalStateException("audit backend down"))
            .when(auditEventService)
            .saveAll(any());

        assertThatCode(
            () -> jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.COMPLETED)))
                .doesNotThrowAnyException();

        Counter counter = meterRegistry.find("bytechef_audit_failure")
            .tag("reason", "persist")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @SuppressWarnings("unchecked")
    private PersistentAuditEvent captureSavedEvent() {
        ArgumentCaptor<List<PersistentAuditEvent>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(auditEventService).saveAll(argumentCaptor.capture());

        List<PersistentAuditEvent> persistentAuditEvents = argumentCaptor.getValue();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }
}
