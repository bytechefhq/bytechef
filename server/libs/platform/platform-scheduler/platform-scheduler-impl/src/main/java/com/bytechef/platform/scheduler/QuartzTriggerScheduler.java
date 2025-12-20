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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.scheduler.job.DelaySchedulerJob;
import com.bytechef.platform.scheduler.job.DynamicWebhookTriggerRefreshJob;
import com.bytechef.platform.scheduler.job.PollingTriggerJob;
import com.bytechef.platform.scheduler.job.ScheduleTriggerJob;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import org.quartz.CronScheduleBuilder;
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
public class QuartzTriggerScheduler implements TriggerScheduler {

    private final int pollingTriggerCheckPeriod;
    private final Scheduler scheduler;

    @SuppressFBWarnings("EI")
    public QuartzTriggerScheduler(
        ApplicationProperties.Coordinator.Trigger.Polling polling, Scheduler scheduler) {

        this.pollingTriggerCheckPeriod = polling.getCheckPeriod();
        this.scheduler = scheduler;
    }

    public void cancelDynamicWebhookTriggerRefresh(String workflowExecutionId) {
        deleteJob(workflowExecutionId, "DynamicWebhookTriggerRefresh");
    }

    @Override
    public void cancelScheduleTrigger(String workflowExecutionId) {
        deleteJob(workflowExecutionId, "ScheduleTrigger");
    }

    @Override
    public void cancelPollingTrigger(String workflowExecutionId) {
        deleteJob(workflowExecutionId, "PollingTrigger");
    }

    @Override
    public void scheduleDynamicWebhookTriggerRefresh(
        Instant webhookExpirationDate, String componentName, int componentVersion,
        WorkflowExecutionId workflowExecutionId, Long connectionId) {

        JobDetail jobDetail = JobBuilder.newJob(DynamicWebhookTriggerRefreshJob.class)
            .withIdentity(JobKey.jobKey(workflowExecutionId.toString(), "ScheduleTrigger"))
            .usingJobData("workflowExecutionId", workflowExecutionId.toString())
            .usingJobData("connectionId", connectionId)
            .build();

        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(workflowExecutionId.toString(), "ScheduleTrigger"))
            .startAt(Date.from(webhookExpirationDate))
            .build();

        schedule(jobDetail, trigger);
    }

    @Override
    public void scheduleScheduleTrigger(
        String pattern, String zoneId, Map<String, Object> output, WorkflowExecutionId workflowExecutionId) {

        JobDetail jobDetail = JobBuilder.newJob(ScheduleTriggerJob.class)
            .withIdentity(JobKey.jobKey(workflowExecutionId.toString(), "ScheduleTrigger"))
            .usingJobData("output", JsonUtils.write(output))
            .usingJobData("workflowExecutionId", workflowExecutionId.toString())
            .build();

        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(pattern)
            .inTimeZone(TimeZone.getTimeZone(ZoneId.of(zoneId)));

        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(workflowExecutionId.toString(), "ScheduleTrigger"))
            .withSchedule(cronScheduleBuilder)
            .startNow()
            .build();

        schedule(jobDetail, trigger);
    }

    @Override
    public void schedulePollingTrigger(WorkflowExecutionId workflowExecutionId) {
        JobDetail jobDetail = JobBuilder.newJob(PollingTriggerJob.class)
            .withIdentity(JobKey.jobKey(workflowExecutionId.toString(), "PollingTrigger"))
            .usingJobData("workflowExecutionId", workflowExecutionId.toString())
            .build();

        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(workflowExecutionId.toString(), "PollingTrigger"))
            .withSchedule(
                SimpleScheduleBuilder.repeatMinutelyForever(pollingTriggerCheckPeriod))
            .startNow()
            .build();

        schedule(jobDetail, trigger);
    }

    @Override
    public void scheduleOneTimeTask(
        Instant executeAt, Map<String, Object> output, WorkflowExecutionId workflowExecutionId,
        String taskExecutionId) {

        JobDetail jobDetail = JobBuilder.newJob(DelaySchedulerJob.class)
            .withIdentity(JobKey.jobKey(taskExecutionId, "DelayTask"))
            .usingJobData("output", JsonUtils.write(output))
            .usingJobData("workflowExecutionId", workflowExecutionId.toString())
            .usingJobData("taskExecutionId", taskExecutionId)
            .build();

        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(taskExecutionId, "DelayTask"))
            .startAt(Date.from(executeAt))
            .build();

        schedule(jobDetail, trigger);
    }

    private void deleteJob(String workflowExecutionId, String pollingTrigger) {
        try {
            scheduler.deleteJob(JobKey.jobKey(workflowExecutionId, pollingTrigger));
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private void schedule(JobDetail jobDetail, Trigger trigger) {
        try {
            scheduler.deleteJob(jobDetail.getKey());

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
}
