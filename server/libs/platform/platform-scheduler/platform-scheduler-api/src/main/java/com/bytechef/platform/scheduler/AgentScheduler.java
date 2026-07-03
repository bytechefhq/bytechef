/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.scheduler;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Scheduler API for AI Hub personal-agent scheduled runs. Wraps the shared Quartz scheduler with a domain-specific
 * surface — jobs are keyed by {@code agentScheduleId} (an opaque long owned by the consuming module's table). Sibling
 * to {@code TriggerScheduler}, which is workflow-bound; this one is keyed by a plain long so it doesn't leak workflow
 * types.
 *
 * @author Ivica Cardic
 */
public interface AgentScheduler {

    /**
     * Register a new cron-triggered job. Idempotent — registering twice with the same id replaces the existing trigger.
     *
     * @param agentScheduleId row id from {@code ai_hub_personal_agent_schedule}
     * @param cronExpression  6-field Quartz cron (seconds-resolution)
     * @param zoneId          IANA zone id (e.g., "Europe/Zagreb")
     * @param startAt         earliest fire time, or {@code null} for "starts immediately"
     */
    void scheduleAgentRun(long agentScheduleId, String cronExpression, String zoneId, @Nullable Instant startAt);

    /**
     * Replace the trigger for an existing job. Equivalent to cancel + schedule, but preserves the job key so any
     * in-flight job that's already executing finishes against the old trigger and the new trigger picks up on the next
     * fire boundary.
     */
    void rescheduleAgentRun(long agentScheduleId, String cronExpression, String zoneId, @Nullable Instant startAt);

    /**
     * Remove the trigger and job. No-op if not registered.
     */
    void cancelAgentRun(long agentScheduleId);

    /**
     * Returns {@code true} iff a job/trigger pair exists for this id. Used by boot reconciliation to detect drift
     * between persisted schedules and Quartz state.
     */
    boolean exists(long agentScheduleId);
}
