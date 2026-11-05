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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.ApplicationEvent;
import com.bytechef.platform.workflow.coordinator.event.ErrorEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerExecutionErrorEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerStartedApplicationEvent;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
class TriggerAuditListenerIntTest {

    @MockitoSpyBean
    private AuditEventService auditEventService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private TriggerAuditListener triggerAuditListener;

    @MockitoBean
    private TriggerExecutionService triggerExecutionService;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testTriggerStartedIsRecordedWithTheWorkflowThatFired() {
        when(triggerExecutionService.getTriggerExecution(11L)).thenReturn(
            TriggerExecution.builder()
                .id(11L)
                .workflowExecutionId(
                    WorkflowExecutionId.of(PlatformType.AUTOMATION, 3L, "workflow-uuid", "schedule_1"))
                .build());

        triggerAuditListener.onApplicationEvent(new TriggerStartedApplicationEvent(11L));

        PersistentAuditEvent persistentAuditEvent = findOnly();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo("TRIGGER_STARTED");
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("SYSTEM");
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("triggerExecutionId", "11")
            .containsEntry("principalId", "3")
            .containsEntry("workflowUuid", "workflow-uuid")
            .containsEntry("triggerName", "schedule_1");
    }

    @Test
    void testTriggerStartedIsRecordedWhenTheTriggerExecutionCannotBeResolved() {
        when(triggerExecutionService.getTriggerExecution(anyLong()))
            .thenThrow(new IllegalArgumentException("gone"));

        triggerAuditListener.onApplicationEvent(new TriggerStartedApplicationEvent(12L));

        PersistentAuditEvent persistentAuditEvent = findOnly();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo("TRIGGER_STARTED");
        assertThat(persistentAuditEvent.getData()).containsEntry("triggerExecutionId", "12");
        assertThat(persistentAuditEvent.getPlatform()).isNull();
    }

    @Test
    void testTriggerFailureIsRecordedWithItsError() {
        TriggerExecution triggerExecution = TriggerExecution.builder()
            .id(13L)
            .error(new ExecutionError("connection refused", List.of()))
            .workflowExecutionId(WorkflowExecutionId.of(PlatformType.AUTOMATION, 4L, "uuid-2", "webhook_1"))
            .build();

        triggerAuditListener.onErrorEvent(new TriggerExecutionErrorEvent(triggerExecution));

        PersistentAuditEvent persistentAuditEvent = findOnly();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo("TRIGGER_FAILED");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("triggerExecutionId", "13")
            .containsEntry("error", "connection refused")
            .containsEntry("triggerName", "webhook_1");
    }

    @Test
    void testUnrelatedEventsAreIgnored() {
        triggerAuditListener.onApplicationEvent(mock(ApplicationEvent.class));
        triggerAuditListener.onErrorEvent(mock(ErrorEvent.class));

        assertThat(persistenceAuditEventRepository.findAll()).isEmpty();
    }

    @Test
    void testPersistenceFailureDoesNotBreakTheCoordinatorListenerLoop() {
        doThrow(new IllegalStateException("audit backend down"))
            .when(auditEventService)
            .saveAll(any());

        assertThatCode(() -> triggerAuditListener.onApplicationEvent(new TriggerStartedApplicationEvent(14L)))
            .doesNotThrowAnyException();
    }

    private PersistentAuditEvent findOnly() {
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }
}
