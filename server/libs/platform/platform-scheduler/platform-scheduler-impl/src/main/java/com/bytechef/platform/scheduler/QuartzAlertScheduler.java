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

import com.bytechef.platform.scheduler.job.AlertEvaluationJob;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

/**
 * @author Ivica Cardic
 */
public class QuartzAlertScheduler implements AlertScheduler {

    private final Scheduler scheduler;

    @SuppressFBWarnings("EI")
    public QuartzAlertScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void scheduleAlertEvaluation(long alertRuleId, int windowMinutes) {
        String alertRuleIdStr = String.valueOf(alertRuleId);

        JobDetail jobDetail = JobBuilder.newJob(AlertEvaluationJob.class)
            .withIdentity(JobKey.jobKey(alertRuleIdStr, "AlertEvaluation"))
            .usingJobData("alertRuleId", alertRuleId)
            .build();

        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(alertRuleIdStr, "AlertEvaluation"))
            .withSchedule(
                SimpleScheduleBuilder.repeatMinutelyForever(windowMinutes))
            .startNow()
            .build();

        try {
            scheduler.deleteJob(jobDetail.getKey());

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException schedulerException) {
            throw new RuntimeException(schedulerException);
        }
    }

    @Override
    public void cancelAlertEvaluation(long alertRuleId) {
        try {
            scheduler.deleteJob(JobKey.jobKey(String.valueOf(alertRuleId), "AlertEvaluation"));
        } catch (SchedulerException schedulerException) {
            throw new RuntimeException(schedulerException);
        }
    }
}
