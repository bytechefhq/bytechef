/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class JobStatusAuditListenerTest {

    private AuditEventService auditEventService;
    private PrincipalJobService principalJobService;
    private ObjectProvider<PrincipalJobService> principalJobServiceProvider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        auditEventService = mock(AuditEventService.class);
        principalJobService = mock(PrincipalJobService.class);
        principalJobServiceProvider = mock(ObjectProvider.class);

        when(principalJobServiceProvider.getIfAvailable()).thenReturn(principalJobService);
        when(principalJobService.fetchJobPrincipalId(anyLong(), any()))
            .thenReturn(Optional.empty());
    }

    @ParameterizedTest
    @EnumSource(PlatformType.class)
    void testPlatformIsTheTypeOfTheJobPrincipal(PlatformType platformType) {
        when(principalJobService.fetchJobPrincipalId(42L, platformType)).thenReturn(Optional.of(5L));

        createListener().onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.COMPLETED));

        assertThat(captureSavedEvent().getPlatform()).isEqualTo(platformType);
    }

    @Test
    void testJobWithoutPrincipalHasNoPlatform() {
        createListener().onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.STARTED));

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getPlatform()).isNull();
        assertThat(persistentAuditEvent.getEventType()).isEqualTo("JOB_STARTED");
    }

    @Test
    void testUnavailablePrincipalJobServiceStillWritesTheRowWithoutPlatform() {
        when(principalJobServiceProvider.getIfAvailable()).thenReturn(null);

        createListener().onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.FAILED));

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getPlatform()).isNull();
        assertThat(persistentAuditEvent.getEventType()).isEqualTo("JOB_FAILED");
        assertThat(persistentAuditEvent.getData()).containsEntry("jobId", "42");
    }

    @Test
    void testFailingLookupStillWritesTheRowWithoutPlatform() {
        when(principalJobService.fetchJobPrincipalId(eq(42L), any()))
            .thenThrow(new IllegalStateException("execution app down"));

        createListener().onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.COMPLETED));

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getPlatform()).isNull();
        assertThat(persistentAuditEvent.getEventType()).isEqualTo("JOB_COMPLETED");
    }

    @Test
    void testLaterStatusFillsInThePlatformOfTheCreatedEvent() {
        when(principalJobService.fetchJobPrincipalId(42L, PlatformType.EMBEDDED)).thenReturn(Optional.of(5L));

        createListener().onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.STARTED));

        verify(auditEventService).updateJobCreatedPlatform(eq(42L), eq(PlatformType.EMBEDDED), any());
    }

    @Test
    void testCreatedEventDoesNotUpdateItself() {
        when(principalJobService.fetchJobPrincipalId(42L, PlatformType.AUTOMATION)).thenReturn(Optional.of(5L));

        createListener().onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.CREATED));

        verify(auditEventService, never()).updateJobCreatedPlatform(anyLong(), any(), any());
    }

    @Test
    void testLaterStatusWithoutPlatformDoesNotUpdateTheCreatedEvent() {
        createListener().onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.STARTED));

        verify(auditEventService, never()).updateJobCreatedPlatform(anyLong(), any(), any());
    }

    @Test
    void testFailingCreatedEventUpdateKeepsTheLaterStatusRow() {
        when(principalJobService.fetchJobPrincipalId(42L, PlatformType.AUTOMATION)).thenReturn(Optional.of(5L));
        doThrow(new IllegalStateException("database down")).when(auditEventService)
            .updateJobCreatedPlatform(anyLong(), any(), any());

        createListener().onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.COMPLETED));

        assertThat(captureSavedEvent().getPlatform()).isEqualTo(PlatformType.AUTOMATION);
    }

    private JobStatusAuditListener createListener() {
        return new JobStatusAuditListener(auditEventService, principalJobServiceProvider);
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
