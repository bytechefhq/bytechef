/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.audit.AiHubAuditEvent;
import com.bytechef.ee.ai.hub.audit.AiHubAuditPublisher;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentScheduleService.ScheduleInput;
import com.bytechef.ee.ai.hub.personalagent.repository.AiHubPersonalAgentScheduleRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.scheduler.AgentScheduler;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubPersonalAgentScheduleServiceImplTest {

    private AiHubPersonalAgentScheduleRepository repository;
    private AgentScheduler agentScheduler;
    private AiHubAuditPublisher auditPublisher;
    private AiHubPersonalAgentScheduleServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = Mockito.mock(AiHubPersonalAgentScheduleRepository.class);
        agentScheduler = Mockito.mock(AgentScheduler.class);
        auditPublisher = Mockito.mock(AiHubAuditPublisher.class);

        ObjectProvider<MeterRegistry> meterRegistryProvider = Mockito.mock(ObjectProvider.class);
        when(meterRegistryProvider.getIfAvailable()).thenReturn(null);

        service = new AiHubPersonalAgentScheduleServiceImpl(
            repository, agentScheduler, auditPublisher, meterRegistryProvider);
    }

    @Test
    void testCreateNormalizesCronAndRegistersQuartzJob() {
        AiHubPersonalAgentSchedule input = buildDaily9AmRecurring();
        when(repository.save(any())).thenAnswer(invocation -> {
            AiHubPersonalAgentSchedule saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        AiHubPersonalAgentSchedule result = service.create(input);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getEffectiveCronExpression()).isEqualTo("0 0 9 * * ?");
        verify(agentScheduler).scheduleAgentRun(42L, "0 0 9 * * ?", "Europe/Zagreb", null);
    }

    @Test
    void testCreateRecurringClearsMaxRuns() {
        AiHubPersonalAgentSchedule input = buildDaily9AmRecurring();
        input.setMaxRuns(99);
        when(repository.save(any())).thenAnswer(invocation -> {
            AiHubPersonalAgentSchedule saved = invocation.getArgument(0);
            if (saved.getId() == null)
                saved.setId(1L);
            return saved;
        });

        AiHubPersonalAgentSchedule result = service.create(input);

        assertThat(result.getMaxRuns()).isNull();
        assertThat(result.getRemainingRuns()).isNull();
    }

    @Test
    void testCreateNumberOfRunsMirrorsMaxIntoRemaining() {
        AiHubPersonalAgentSchedule input = buildDaily9AmRecurring();
        input.setLifecycleKind(ScheduleLifecycleKind.NUMBER_OF_RUNS);
        input.setMaxRuns(5);
        when(repository.save(any())).thenAnswer(invocation -> {
            AiHubPersonalAgentSchedule saved = invocation.getArgument(0);
            if (saved.getId() == null)
                saved.setId(1L);
            return saved;
        });

        AiHubPersonalAgentSchedule result = service.create(input);

        assertThat(result.getRemainingRuns()).isEqualTo(5);
    }

    @Test
    void testRecordFireDecrementsAndReturnsTrueWhenStillBudget() {
        AiHubPersonalAgentSchedule existing = buildDaily9AmRecurring();
        existing.setId(7L);
        existing.setLifecycleKind(ScheduleLifecycleKind.NUMBER_OF_RUNS);
        existing.setMaxRuns(3);
        existing.setRemainingRuns(3);
        existing.setEffectiveCronExpression("0 0 9 * * ?");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.<AiHubPersonalAgentSchedule>getArgument(0));

        boolean stillEnabled = service.recordFire(7L);

        assertThat(stillEnabled).isTrue();
        assertThat(existing.getRemainingRuns()).isEqualTo(2);
        verify(agentScheduler, never()).cancelAgentRun(anyLong());
    }

    @Test
    void testRecordFireDisablesAndCancelsAtZero() {
        AiHubPersonalAgentSchedule existing = buildDaily9AmRecurring();
        existing.setId(7L);
        existing.setLifecycleKind(ScheduleLifecycleKind.NUMBER_OF_RUNS);
        existing.setMaxRuns(1);
        existing.setRemainingRuns(1);
        existing.setEffectiveCronExpression("0 0 9 * * ?");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.<AiHubPersonalAgentSchedule>getArgument(0));

        boolean stillEnabled = service.recordFire(7L);

        assertThat(stillEnabled).isFalse();
        assertThat(existing.isEnabled()).isFalse();
        verify(agentScheduler, times(1)).cancelAgentRun(7L);
    }

    @Test
    void testRecordFailureAtThreeStrikesDisables() {
        AiHubPersonalAgentSchedule existing = buildDaily9AmRecurring();
        existing.setId(7L);
        existing.setConsecutiveFailures(2);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.<AiHubPersonalAgentSchedule>getArgument(0));

        service.recordFailure(7L);

        assertThat(existing.getConsecutiveFailures()).isEqualTo(3);
        assertThat(existing.isEnabled()).isFalse();
        verify(agentScheduler).cancelAgentRun(7L);
    }

    @Test
    void testDeleteCancelsQuartzJob() {
        AiHubPersonalAgentSchedule existing = buildDaily9AmRecurring();
        existing.setId(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));

        service.delete(7L, existing.getWorkspaceId(), existing.getUserId());

        verify(agentScheduler).cancelAgentRun(7L);
        verify(repository).deleteById(7L);
    }

    @Test
    void testUpsertOrDeleteUpdatesExistingRow() {
        AiHubPersonalAgentSchedule existing = sampleSchedule();
        existing.setId(99L);
        existing.setEnabled(true);

        when(repository.findByAiHubPersonalAgentId(42L)).thenReturn(Optional.of(existing));
        when(repository.save(any(AiHubPersonalAgentSchedule.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleInput input = new ScheduleInput(
            false, "Updated title", "Updated prompt",
            ScheduleFrequencyKind.DAILY, null, null,
            LocalTime.of(10, 30), null, null, null,
            "UTC", null, ScheduleLifecycleKind.RECURRING, null);

        AiHubPersonalAgentSchedule result = service.upsertOrDelete(42L, 1L, 10L, Environment.DEVELOPMENT, input);

        assertThat(result).isNotNull();
        assertThat(result.isEnabled()).isFalse();
        assertThat(result.getTitle()).isEqualTo("Updated title");

        verify(agentScheduler).cancelAgentRun(99L);
    }

    @Test
    void testFindByAgentIdReturnsTheSingleRowIfPresent() {
        AiHubPersonalAgentSchedule schedule = sampleSchedule();
        schedule.setId(7L);

        when(repository.findByAiHubPersonalAgentId(42L)).thenReturn(Optional.of(schedule));

        Optional<AiHubPersonalAgentSchedule> result = service.findByAgentId(42L);

        assertThat(result).hasValueSatisfying(row -> assertThat(row.getId()).isEqualTo(7L));
    }

    @Test
    void testUpsertOrDeleteInsertsWhenAbsent() {
        when(repository.findByAiHubPersonalAgentId(42L)).thenReturn(Optional.empty());
        when(repository.save(any(AiHubPersonalAgentSchedule.class))).thenAnswer(invocation -> {
            AiHubPersonalAgentSchedule row = invocation.getArgument(0);
            row.setId(123L);
            return row;
        });

        ScheduleInput input = new ScheduleInput(
            true, "New", "Prompt",
            ScheduleFrequencyKind.DAILY, null, null,
            LocalTime.of(9, 0), null, null, null,
            "UTC", null, ScheduleLifecycleKind.RECURRING, null);

        AiHubPersonalAgentSchedule result = service.upsertOrDelete(42L, 1L, 10L, Environment.DEVELOPMENT, input);

        assertThat(result).isNotNull();
        assertThat(result.getAiHubPersonalAgentId()).isEqualTo(42L);

        verify(agentScheduler).scheduleAgentRun(eq(123L), anyString(), eq("UTC"), any());
    }

    @Test
    void testUpsertOrDeleteDeletesWhenInputNull() {
        AiHubPersonalAgentSchedule existing = sampleSchedule();
        existing.setId(99L);

        when(repository.findByAiHubPersonalAgentId(42L)).thenReturn(Optional.of(existing));
        when(repository.findById(99L)).thenReturn(Optional.of(existing));

        AiHubPersonalAgentSchedule result = service.upsertOrDelete(42L, 1L, 10L, Environment.DEVELOPMENT, null);

        assertThat(result).isNull();

        verify(repository).deleteById(99L);
        verify(agentScheduler).cancelAgentRun(99L);
    }

    @Test
    void testUpsertOrDeleteWithInputNullAndNoExistingIsNoop() {
        when(repository.findByAiHubPersonalAgentId(42L)).thenReturn(Optional.empty());

        AiHubPersonalAgentSchedule result = service.upsertOrDelete(42L, 1L, 10L, Environment.DEVELOPMENT, null);

        assertThat(result).isNull();

        verify(repository, never()).deleteById(anyLong());
        verify(agentScheduler, never()).cancelAgentRun(anyLong());
    }

    @Test
    void testUpsertOrDeleteRejectsForeignWorkspace() {
        AiHubPersonalAgentSchedule existing = sampleSchedule();
        existing.setId(99L);
        existing.setWorkspaceId(999L);
        existing.setUserId(999L);

        when(repository.findByAiHubPersonalAgentId(42L)).thenReturn(Optional.of(existing));

        ScheduleInput input = new ScheduleInput(
            true, "Hijack", "Prompt",
            ScheduleFrequencyKind.DAILY, null, null,
            LocalTime.of(9, 0), null, null, null,
            "UTC", null, ScheduleLifecycleKind.RECURRING, null);

        assertThatThrownBy(() -> service.upsertOrDelete(42L, 1L, 10L, Environment.DEVELOPMENT, input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not owned by caller");
    }

    @Test
    void testUpsertOrDeleteInsertEmitsUpsertedAuditEvent() {
        when(repository.findByAiHubPersonalAgentId(42L)).thenReturn(Optional.empty());
        when(repository.save(any(AiHubPersonalAgentSchedule.class)))
            .thenAnswer(invocation -> {
                AiHubPersonalAgentSchedule row = invocation.getArgument(0);
                row.setId(123L);
                return row;
            });

        ScheduleInput input = new ScheduleInput(
            true, "Title", "Prompt",
            ScheduleFrequencyKind.DAILY, null, null,
            LocalTime.of(9, 0), null, null, null,
            "UTC", null, ScheduleLifecycleKind.RECURRING, null);

        service.upsertOrDelete(42L, 1L, 10L, Environment.DEVELOPMENT, input);

        verify(auditPublisher).publish(
            eq(AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_UPSERTED),
            argThat(data -> "1".equals(data.get("workspaceId"))
                && "42".equals(data.get("agentId"))
                && "123".equals(data.get("scheduleId"))));
    }

    @Test
    void testUpsertOrDeleteDeleteEmitsDeletedAuditEvent() {
        AiHubPersonalAgentSchedule existing = sampleSchedule();
        existing.setId(99L);

        when(repository.findByAiHubPersonalAgentId(42L)).thenReturn(Optional.of(existing));
        when(repository.findById(99L)).thenReturn(Optional.of(existing));

        service.upsertOrDelete(42L, 1L, 10L, Environment.DEVELOPMENT, null);

        verify(auditPublisher).publish(
            eq(AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_DELETED),
            argThat(data -> "1".equals(data.get("workspaceId"))
                && "42".equals(data.get("agentId"))
                && "99".equals(data.get("scheduleId"))));
    }

    @Test
    void testRecordFailureThreeStrikesEmitsAutoDisabledAuditEvent() {
        AiHubPersonalAgentSchedule schedule = sampleSchedule();
        schedule.setId(99L);
        schedule.setConsecutiveFailures(2);

        when(repository.findById(99L)).thenReturn(Optional.of(schedule));
        when(repository.save(any(AiHubPersonalAgentSchedule.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service.recordFailure(99L);

        verify(auditPublisher).publish(
            eq(AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_AUTO_DISABLED),
            argThat(data -> "three_consecutive_failures".equals(data.get("reason"))));
    }

    private AiHubPersonalAgentSchedule buildDaily9AmRecurring() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setAiHubPersonalAgentId(100L);
        schedule.setWorkspaceId(1L);
        schedule.setUserId(2L);
        schedule.setEnvironment(Environment.DEVELOPMENT);
        schedule.setTitle("Daily report");
        schedule.setPrompt("Summarize yesterday");
        schedule.setFrequencyKind(ScheduleFrequencyKind.DAILY);
        schedule.setTimeOfDay(LocalTime.of(9, 0));
        schedule.setZoneId("Europe/Zagreb");
        schedule.setLifecycleKind(ScheduleLifecycleKind.RECURRING);
        return schedule;
    }

    private AiHubPersonalAgentSchedule sampleSchedule() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setAiHubPersonalAgentId(42L);
        schedule.setWorkspaceId(1L);
        schedule.setUserId(10L);
        schedule.setEnvironment(Environment.DEVELOPMENT);
        schedule.setTitle("Sample schedule");
        schedule.setPrompt("Sample prompt");
        schedule.setFrequencyKind(ScheduleFrequencyKind.DAILY);
        schedule.setTimeOfDay(LocalTime.of(8, 0));
        schedule.setZoneId("UTC");
        schedule.setLifecycleKind(ScheduleLifecycleKind.RECURRING);
        return schedule;
    }
}
