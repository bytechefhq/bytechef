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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.SharedApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class JobStatusAuditListenerIntTest {

    @MockitoSpyBean
    private AuditEventService auditEventService;

    @Autowired
    private JobStatusAuditListener jobStatusAuditListener;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @MockitoBean
    private PrincipalJobService principalJobService;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();
    }

    @ParameterizedTest
    @CsvSource({
        "CREATED,JOB_CREATED", "STARTED,JOB_STARTED", "COMPLETED,JOB_COMPLETED", "FAILED,JOB_FAILED",
        "STOPPED,JOB_STOPPED"
    })
    void testJobStatusIsStoredAsItsOwnEventType(String status, String expectedEventType) {
        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.valueOf(status)));

        List<PersistentAuditEvent> persistentAuditEvents = findAll();

        assertThat(persistentAuditEvents).hasSize(1);

        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(expectedEventType);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("SYSTEM");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("jobId", "42")
            .containsEntry("status", status);
        assertThat(persistentAuditEvent.getEventDate()).isNotNull();
        assertThat(persistentAuditEvent.getPlatform()).isNull();
    }

    @Test
    void testJobLifecycleIsReadableFromTheAuditTable() {
        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(7L, Job.Status.CREATED));
        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(7L, Job.Status.STARTED));
        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(7L, Job.Status.COMPLETED));

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, "7", true, null), PageRequest.of(0, 25));

        List<String> eventTypes = page.getContent()
            .stream()
            .map(PersistentAuditEvent::getEventType)
            .toList();

        assertThat(eventTypes).containsExactlyInAnyOrder("JOB_CREATED", "JOB_STARTED", "JOB_COMPLETED");
        assertThat(page.getContent())
            .allSatisfy(persistentAuditEvent -> assertThat(persistentAuditEvent.getData())
                .containsEntry("jobId", "7"));
    }

    @Test
    void testListenerIsSharedWithSynchronousJobExecutors() {
        assertThat(jobStatusAuditListener).isInstanceOf(SharedApplicationEventListener.class);
    }

    @Test
    void testUnrelatedEventIsIgnored() {
        jobStatusAuditListener.onApplicationEvent(mock(ApplicationEvent.class));

        assertThat(findAll()).isEmpty();
    }

    @Test
    void testPersistenceFailureDoesNotPropagate() {
        doThrow(new IllegalStateException("audit backend down"))
            .when(auditEventService)
            .saveAll(any());

        assertThatCode(
            () -> jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.COMPLETED)))
                .doesNotThrowAnyException();

        assertThat(findAll()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(PlatformType.class)
    void testPlatformIsTheTypeOfTheJobPrincipal(PlatformType platformType) {
        when(principalJobService.fetchJobPrincipalId(42L, platformType)).thenReturn(Optional.of(5L));

        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.COMPLETED));

        assertThat(findSingle("JOB_COMPLETED").getPlatform()).isEqualTo(platformType);
    }

    @Test
    void testUnavailablePrincipalJobServiceStillWritesTheRowWithoutPlatform() {
        @SuppressWarnings("unchecked")
        ObjectProvider<PrincipalJobService> principalJobServiceProvider = mock(ObjectProvider.class);

        JobStatusAuditListener listener = new JobStatusAuditListener(auditEventService, principalJobServiceProvider);

        listener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.FAILED));

        PersistentAuditEvent persistentAuditEvent = findSingle("JOB_FAILED");

        assertThat(persistentAuditEvent.getPlatform()).isNull();
        assertThat(persistentAuditEvent.getData()).containsEntry("jobId", "42");
    }

    @Test
    void testFailingLookupStillWritesTheRowWithoutPlatform() {
        when(principalJobService.fetchJobPrincipalId(eq(42L), any()))
            .thenThrow(new IllegalStateException("execution app down"));

        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.COMPLETED));

        assertThat(findSingle("JOB_COMPLETED").getPlatform()).isNull();
    }

    @Test
    void testLaterStatusFillsInThePlatformOfTheCreatedEvent() {
        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.CREATED));

        assertThat(findSingle("JOB_CREATED").getPlatform()).isNull();

        when(principalJobService.fetchJobPrincipalId(42L, PlatformType.EMBEDDED)).thenReturn(Optional.of(5L));

        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.STARTED));

        assertThat(findSingle("JOB_CREATED").getPlatform()).isEqualTo(PlatformType.EMBEDDED);
        assertThat(findSingle("JOB_STARTED").getPlatform()).isEqualTo(PlatformType.EMBEDDED);
    }

    @Test
    void testLaterStatusLeavesTheCreatedEventOfAnotherJobAlone() {
        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(7L, Job.Status.CREATED));

        when(principalJobService.fetchJobPrincipalId(42L, PlatformType.AUTOMATION)).thenReturn(Optional.of(5L));

        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.STARTED));

        assertThat(findSingle("JOB_CREATED").getPlatform()).isNull();
    }

    @Test
    void testCreatedEventDoesNotUpdateItself() {
        when(principalJobService.fetchJobPrincipalId(42L, PlatformType.AUTOMATION)).thenReturn(Optional.of(5L));

        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.CREATED));

        assertThat(findSingle("JOB_CREATED").getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        verify(auditEventService, never()).updateJobCreatedPlatform(anyLong(), any(), any());
    }

    @Test
    void testLaterStatusWithoutPlatformDoesNotUpdateTheCreatedEvent() {
        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.STARTED));

        verify(auditEventService, never()).updateJobCreatedPlatform(anyLong(), any(), any());
    }

    @Test
    void testFailingCreatedEventUpdateKeepsTheLaterStatusRow() {
        when(principalJobService.fetchJobPrincipalId(42L, PlatformType.AUTOMATION)).thenReturn(Optional.of(5L));
        doThrow(new IllegalStateException("database down")).when(auditEventService)
            .updateJobCreatedPlatform(anyLong(), any(), any());

        assertThatCode(
            () -> jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.COMPLETED)))
                .doesNotThrowAnyException();

        assertThat(findSingle("JOB_COMPLETED").getPlatform()).isEqualTo(PlatformType.AUTOMATION);
    }

    private PersistentAuditEvent findSingle(String eventType) {
        List<PersistentAuditEvent> allPersistentAuditEvents = findAll();

        List<PersistentAuditEvent> persistentAuditEvents = allPersistentAuditEvents.stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    private List<PersistentAuditEvent> findAll() {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            new AuditEventFilter(null, null, null, null, true, null), PageRequest.of(0, 25));

        return page.getContent();
    }
}
