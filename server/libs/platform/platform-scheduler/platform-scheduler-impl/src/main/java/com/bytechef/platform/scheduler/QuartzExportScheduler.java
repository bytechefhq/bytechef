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

import com.bytechef.platform.scheduler.job.ExportExecutionJob;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

/**
 * @author Ivica Cardic
 */
public class QuartzExportScheduler implements ExportScheduler {

    private static final String GROUP = "ExportExecution";

    private final Scheduler scheduler;

    @SuppressFBWarnings("EI")
    public QuartzExportScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void scheduleExport(long exportJobId, String cronExpression) {
        String exportJobIdStr = String.valueOf(exportJobId);

        JobDetail jobDetail = JobBuilder.newJob(ExportExecutionJob.class)
            .withIdentity(JobKey.jobKey(exportJobIdStr, GROUP))
            .usingJobData("exportJobId", exportJobId)
            .build();

        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(exportJobIdStr, GROUP))
            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
            .build();

        try {
            scheduler.deleteJob(jobDetail.getKey());

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException schedulerException) {
            throw new RuntimeException(schedulerException);
        }
    }

    @Override
    public void cancelExport(long exportJobId) {
        try {
            scheduler.deleteJob(JobKey.jobKey(String.valueOf(exportJobId), GROUP));
        } catch (SchedulerException schedulerException) {
            throw new RuntimeException(schedulerException);
        }
    }
}
