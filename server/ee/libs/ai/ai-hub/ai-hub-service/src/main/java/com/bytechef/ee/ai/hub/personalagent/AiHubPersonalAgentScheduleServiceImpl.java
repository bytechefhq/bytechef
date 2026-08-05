/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import com.bytechef.ee.ai.hub.audit.AiHubAuditEvent;
import com.bytechef.ee.ai.hub.audit.AiHubAuditPublisher;
import com.bytechef.ee.ai.hub.personalagent.repository.AiHubPersonalAgentScheduleRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.scheduler.AgentScheduler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import org.jspecify.annotations.Nullable;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
public class AiHubPersonalAgentScheduleServiceImpl implements AiHubPersonalAgentScheduleService {

    private static final Logger log = LoggerFactory.getLogger(AiHubPersonalAgentScheduleServiceImpl.class);
    private static final int FAILURE_DISABLE_THRESHOLD = 3;

    private final AiHubPersonalAgentScheduleRepository repository;
    private final AgentScheduler agentScheduler;
    private final AiHubAuditPublisher auditPublisher;
    private final @Nullable MeterRegistry meterRegistry;

    @SuppressFBWarnings({
        "EI2", "CT"
    })
    public AiHubPersonalAgentScheduleServiceImpl(
        AiHubPersonalAgentScheduleRepository repository, AgentScheduler agentScheduler,
        AiHubAuditPublisher auditPublisher,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.repository = repository;
        this.agentScheduler = agentScheduler;
        this.auditPublisher = auditPublisher;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @Override
    @Transactional
    public AiHubPersonalAgentSchedule create(AiHubPersonalAgentSchedule schedule) {
        normalizeOnWrite(schedule);

        AiHubPersonalAgentSchedule saved = repository.save(schedule);

        Instant startAt = startAtInstant(saved);

        agentScheduler.scheduleAgentRun(
            saved.getId(), saved.getEffectiveCronExpression(), saved.getZoneId(), startAt);

        saved.setNextRunAt(computeNextRunAt(saved.getEffectiveCronExpression(), saved.getZoneId(), startAt));

        return repository.save(saved);
    }

    @Override
    @Transactional
    public AiHubPersonalAgentSchedule update(AiHubPersonalAgentSchedule schedule) {
        normalizeOnWrite(schedule);

        AiHubPersonalAgentSchedule saved = repository.save(schedule);

        Instant startAt = startAtInstant(saved);

        if (saved.isEnabled()) {
            agentScheduler.rescheduleAgentRun(
                saved.getId(), saved.getEffectiveCronExpression(), saved.getZoneId(), startAt);

            saved.setNextRunAt(computeNextRunAt(saved.getEffectiveCronExpression(), saved.getZoneId(), startAt));
        } else {
            agentScheduler.cancelAgentRun(saved.getId());

            saved.setNextRunAt(null);
        }

        return repository.save(saved);
    }

    @Override
    @Transactional
    public void delete(long scheduleId, long workspaceId, long userId) {
        AiHubPersonalAgentSchedule schedule = requireOwned(scheduleId, workspaceId, userId);

        agentScheduler.cancelAgentRun(scheduleId);
        repository.deleteById(schedule.getId());
    }

    @Override
    public Optional<AiHubPersonalAgentSchedule> findById(long scheduleId) {
        return repository.findById(scheduleId);
    }

    @Override
    public Optional<AiHubPersonalAgentSchedule> findByAgentId(long agentId) {
        return repository.findByAiHubPersonalAgentId(agentId);
    }

    @Override
    @Transactional
    @Nullable
    public AiHubPersonalAgentSchedule upsertOrDelete(
        long agentId, long workspaceId, long userId, Environment environment,
        @Nullable ScheduleInput input) {

        Optional<AiHubPersonalAgentSchedule> existing = repository.findByAiHubPersonalAgentId(agentId);

        if (input == null) {
            existing.ifPresent(row -> {
                delete(row.getId(), workspaceId, userId);

                auditPublisher.publish(
                    AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_DELETED,
                    Map.of(
                        "workspaceId", String.valueOf(workspaceId),
                        "agentId", String.valueOf(agentId),
                        "scheduleId", String.valueOf(row.getId())));
            });

            return null;
        }

        if (existing.isPresent()) {
            AiHubPersonalAgentSchedule row = existing.get();

            if (row.getWorkspaceId() != workspaceId || row.getUserId() != userId) {
                throw new IllegalArgumentException("Schedule for agent " + agentId + " not owned by caller");
            }

            applyInput(row, input);

            AiHubPersonalAgentSchedule saved = update(row);

            publishUpsertedEvent(saved, workspaceId, agentId);

            return saved;
        }

        AiHubPersonalAgentSchedule row = new AiHubPersonalAgentSchedule();

        row.setAiHubPersonalAgentId(agentId);
        row.setWorkspaceId(workspaceId);
        row.setUserId(userId);
        row.setEnvironment(environment);

        applyInput(row, input);

        try {
            AiHubPersonalAgentSchedule saved = create(row);

            publishUpsertedEvent(saved, workspaceId, agentId);

            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                "Schedule already exists for agent " + agentId + " (concurrent create)", e);
        }
    }

    private void publishUpsertedEvent(AiHubPersonalAgentSchedule saved, long workspaceId, long agentId) {
        auditPublisher.publish(
            AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_UPSERTED,
            Map.of(
                "workspaceId", String.valueOf(workspaceId),
                "agentId", String.valueOf(agentId),
                "scheduleId", String.valueOf(saved.getId()),
                "enabled", String.valueOf(saved.isEnabled()),
                "frequencyKind", saved.getFrequencyKind()
                    .name(),
                "effectiveCronExpression", saved.getEffectiveCronExpression()));
    }

    private static void applyInput(AiHubPersonalAgentSchedule row, ScheduleInput input) {
        row.setEnabled(input.enabled());
        row.setTitle(input.title());
        row.setPrompt(input.prompt());
        row.setFrequencyKind(input.frequencyKind());
        row.setIntervalMinutes(input.intervalMinutes());
        row.setMinuteOfHour(input.minuteOfHour());
        row.setTimeOfDay(input.timeOfDay());
        row.setDayOfWeek(input.dayOfWeek());
        row.setDayOfMonth(input.dayOfMonth());
        row.setCronExpression(input.cronExpression());
        row.setZoneId(input.zoneId());
        row.setStartDate(input.startDate());
        row.setLifecycleKind(input.lifecycleKind());
        row.setMaxRuns(input.maxRuns());
    }

    @Override
    @Transactional
    public boolean recordFire(long scheduleId) {
        AiHubPersonalAgentSchedule schedule = repository.findById(scheduleId)
            .orElseThrow(() -> new IllegalStateException("Schedule not found: " + scheduleId));

        schedule.setLastRunAt(LocalDateTime.now());
        schedule.setConsecutiveFailures(0);

        Integer remainingRuns = schedule.getRemainingRuns();

        if (remainingRuns != null) {
            int remaining = remainingRuns - 1;

            schedule.setRemainingRuns(remaining);

            if (remaining <= 0) {
                schedule.setEnabled(false);
                schedule.setNextRunAt(null);

                agentScheduler.cancelAgentRun(scheduleId);
                repository.save(schedule);

                counter("success");

                return false;
            }
        }

        schedule.setNextRunAt(
            computeNextRunAt(schedule.getEffectiveCronExpression(), schedule.getZoneId(), null));

        repository.save(schedule);

        counter("success");

        return true;
    }

    @Override
    @Transactional
    public void recordFailure(long scheduleId) {
        AiHubPersonalAgentSchedule schedule = repository.findById(scheduleId)
            .orElseThrow(() -> new IllegalStateException("Schedule not found: " + scheduleId));

        schedule.setConsecutiveFailures(schedule.getConsecutiveFailures() + 1);

        if (schedule.getConsecutiveFailures() >= FAILURE_DISABLE_THRESHOLD) {
            schedule.setEnabled(false);
            schedule.setNextRunAt(null);

            agentScheduler.cancelAgentRun(scheduleId);

            auditPublisher.publish(
                AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_AUTO_DISABLED,
                Map.of(
                    "workspaceId", String.valueOf(schedule.getWorkspaceId()),
                    "agentId", String.valueOf(schedule.getAiHubPersonalAgentId()),
                    "scheduleId", String.valueOf(scheduleId),
                    "reason", "three_consecutive_failures"));
        }

        repository.save(schedule);

        counter("failed");
    }

    @Override
    @Nullable
    public AiHubPersonalAgentSchedule findEnabled(long scheduleId) {
        AiHubPersonalAgentSchedule schedule = repository.findById(scheduleId)
            .orElse(null);

        return (schedule != null && schedule.isEnabled()) ? schedule : null;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnStartup() {
        List<AiHubPersonalAgentSchedule> enabled = repository.findAllEnabled();

        int reregistered = 0;

        for (AiHubPersonalAgentSchedule schedule : enabled) {
            if (!agentScheduler.exists(schedule.getId())) {
                Instant startAt = startAtInstant(schedule);

                agentScheduler.scheduleAgentRun(
                    schedule.getId(), schedule.getEffectiveCronExpression(), schedule.getZoneId(), startAt);

                reregistered++;
            }
        }

        if (reregistered > 0) {
            log.info("Reconciled {} agent schedule(s) at boot", reregistered);
        }
    }

    private void normalizeOnWrite(AiHubPersonalAgentSchedule schedule) {
        schedule.setEffectiveCronExpression(ScheduleCronNormalizer.normalize(schedule));

        clearOffFrequencyFields(schedule);

        if (schedule.getLifecycleKind() == ScheduleLifecycleKind.RECURRING) {
            schedule.setMaxRuns(null);
            schedule.setRemainingRuns(null);
        } else if (schedule.getMaxRuns() != null) {
            schedule.setRemainingRuns(schedule.getMaxRuns());
        } else {
            schedule.setRemainingRuns(null);
        }
    }

    private void clearOffFrequencyFields(AiHubPersonalAgentSchedule schedule) {
        ScheduleFrequencyKind kind = schedule.getFrequencyKind();

        if (kind != ScheduleFrequencyKind.EVERY_X_MINUTES) {
            schedule.setIntervalMinutes(null);
        }
        if (kind != ScheduleFrequencyKind.HOURLY) {
            schedule.setMinuteOfHour(null);
        }
        if (kind == ScheduleFrequencyKind.EVERY_X_MINUTES || kind == ScheduleFrequencyKind.HOURLY
            || kind == ScheduleFrequencyKind.CUSTOM_CRON) {
            schedule.setTimeOfDay(null);
        }
        if (kind != ScheduleFrequencyKind.WEEKLY) {
            schedule.setDayOfWeek(null);
        }
        if (kind != ScheduleFrequencyKind.MONTHLY) {
            schedule.setDayOfMonth(null);
        }
        if (kind != ScheduleFrequencyKind.CUSTOM_CRON) {
            schedule.setCronExpression(null);
        }
    }

    private AiHubPersonalAgentSchedule requireOwned(long scheduleId, long workspaceId, long userId) {
        AiHubPersonalAgentSchedule schedule = repository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        if (schedule.getWorkspaceId() != workspaceId || schedule.getUserId() != userId) {
            throw new IllegalArgumentException("Schedule not owned by caller: " + scheduleId);
        }

        return schedule;
    }

    private @Nullable Instant startAtInstant(AiHubPersonalAgentSchedule schedule) {
        LocalDateTime startDate = schedule.getStartDate();

        if (startDate == null) {
            return null;
        }

        return startDate
            .atZone(ZoneId.of(schedule.getZoneId()))
            .toInstant();
    }

    private @Nullable LocalDateTime computeNextRunAt(String cron, String zoneId, @Nullable Instant startAt) {
        try {
            CronExpression expression = new CronExpression(cron);
            expression.setTimeZone(TimeZone.getTimeZone(zoneId));

            Date after;

            if (startAt != null) {
                after = Date.from(startAt.isAfter(Instant.now()) ? startAt : Instant.now());
            } else {
                after = new Date();
            }

            Date next = expression.getNextValidTimeAfter(after);

            return next != null ? LocalDateTime.ofInstant(next.toInstant(), ZoneId.of(zoneId)) : null;
        } catch (ParseException e) {
            log.warn("Failed to compute next run from cron {} in zone {}: {}", cron, zoneId, e.getMessage());

            return null;
        }
    }

    private void counter(String outcome) {
        if (meterRegistry != null) {
            Counter.builder("bytechef_ai_hub_agent_schedule_fire")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
        }
    }
}
