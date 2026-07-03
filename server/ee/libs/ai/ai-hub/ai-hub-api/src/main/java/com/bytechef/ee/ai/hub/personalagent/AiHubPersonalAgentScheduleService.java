/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import com.bytechef.platform.configuration.domain.Environment;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubPersonalAgentScheduleService {

    AiHubPersonalAgentSchedule create(AiHubPersonalAgentSchedule schedule);

    AiHubPersonalAgentSchedule update(AiHubPersonalAgentSchedule schedule);

    void delete(long scheduleId, long workspaceId, long userId);

    Optional<AiHubPersonalAgentSchedule> findById(long scheduleId);

    Optional<AiHubPersonalAgentSchedule> findByAgentId(long agentId);

    /**
     * Upserts the agent's single schedule when {@code input} is non-null; deletes any existing schedule when
     * {@code input} is null. The DB-level UNIQUE constraint on {@code (ai_hub_personal_agent_id)} is the authoritative
     * 1:1 guard; implementations catch the resulting integrity exception and re-throw as {@link IllegalStateException}.
     */
    @Nullable
    AiHubPersonalAgentSchedule upsertOrDelete(
        long agentId, long workspaceId, long userId, Environment environment, @Nullable ScheduleInput input);

    /**
     * Called by listener on each fire; updates last_run_at, decrements remaining_runs, recomputes next_run_at. Returns
     * {@code true} if the schedule is still enabled after this update; {@code false} when it auto-disabled
     * (remaining_runs hit zero).
     */
    boolean recordFire(long scheduleId);

    /** Called by listener on dispatch failure. */
    void recordFailure(long scheduleId);

    @Nullable
    AiHubPersonalAgentSchedule findEnabled(long scheduleId);

    /**
     * Transport-shaped schedule payload for {@link #upsertOrDelete}. All fields mirror the schedule entity's user-set
     * columns; lifecycle bookkeeping (remaining_runs, consecutive_failures, last/next_run_at) is owned by the service.
     */
    record ScheduleInput(
        boolean enabled,
        String title,
        String prompt,
        ScheduleFrequencyKind frequencyKind,
        @Nullable Integer intervalMinutes,
        @Nullable Integer minuteOfHour,
        @Nullable LocalTime timeOfDay,
        @Nullable Integer dayOfWeek,
        @Nullable Integer dayOfMonth,
        @Nullable String cronExpression,
        String zoneId,
        @Nullable LocalDateTime startDate,
        ScheduleLifecycleKind lifecycleKind,
        @Nullable Integer maxRuns) {
    }
}
