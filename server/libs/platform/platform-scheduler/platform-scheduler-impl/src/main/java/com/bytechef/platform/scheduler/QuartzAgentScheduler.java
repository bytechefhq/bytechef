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

import com.bytechef.platform.scheduler.job.AgentScheduleJob;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;
import org.jspecify.annotations.Nullable;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

/**
 * Quartz-backed {@link AgentScheduler}. Group {@value #GROUP}, name format {@code agent-schedule-<id>}. Misfire policy:
 * fire once on recovery (no catch-up loop).
 *
 * @author Ivica Cardic
 */
public class QuartzAgentScheduler implements AgentScheduler {

    private static final String GROUP = "agent-run";

    private final Scheduler scheduler;

    @SuppressFBWarnings("EI")
    public QuartzAgentScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void scheduleAgentRun(
        long agentScheduleId, String cronExpression, String zoneId, @Nullable Instant startAt) {

        JobKey jobKey = jobKey(agentScheduleId);
        TriggerKey triggerKey = triggerKey(agentScheduleId);

        JobDetail jobDetail = JobBuilder.newJob(AgentScheduleJob.class)
            .withIdentity(jobKey)
            .usingJobData(AgentScheduleJob.JOB_DATA_KEY_SCHEDULE_ID, agentScheduleId)
            .storeDurably(false)
            .build();

        CronTrigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(triggerKey)
            .forJob(jobKey)
            .startAt(startAt != null ? Date.from(startAt) : new Date())
            .withSchedule(
                CronScheduleBuilder.cronSchedule(cronExpression)
                    .inTimeZone(TimeZone.getTimeZone(zoneId))
                    .withMisfireHandlingInstructionFireAndProceed())
            .build();

        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to schedule agent run " + agentScheduleId, e);
        }
    }

    @Override
    public void rescheduleAgentRun(
        long agentScheduleId, String cronExpression, String zoneId, @Nullable Instant startAt) {

        scheduleAgentRun(agentScheduleId, cronExpression, zoneId, startAt);
    }

    @Override
    public void cancelAgentRun(long agentScheduleId) {
        try {
            scheduler.deleteJob(jobKey(agentScheduleId));
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to cancel agent run " + agentScheduleId, e);
        }
    }

    @Override
    public boolean exists(long agentScheduleId) {
        try {
            return scheduler.checkExists(jobKey(agentScheduleId));
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to check agent run " + agentScheduleId, e);
        }
    }

    private static JobKey jobKey(long id) {
        return JobKey.jobKey("agent-schedule-" + id, GROUP);
    }

    private static TriggerKey triggerKey(long id) {
        return TriggerKey.triggerKey("agent-schedule-" + id, GROUP);
    }
}
