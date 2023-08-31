
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.scheduler;

import com.bytechef.commons.util.DateUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.scheduler.job.DynamicWebhookTriggerRefreshJob;
import com.bytechef.hermes.scheduler.job.PollingTriggerJob;
import com.bytechef.hermes.scheduler.job.ScheduleTriggerJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.quartz.CronExpression;
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
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerSchedulerImpl implements TriggerScheduler {

    private final ObjectMapper objectMapper;
    private final Scheduler scheduler;

    @SuppressFBWarnings("EI")
    public TriggerSchedulerImpl(ObjectMapper objectMapper, Scheduler scheduler) {
        this.objectMapper = objectMapper;
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
        LocalDateTime webhookExpirationDate, String componentName, int componentVersion,
        WorkflowExecutionId workflowExecutionId) {

        JobDetail jobDetail = JobBuilder.newJob(DynamicWebhookTriggerRefreshJob.class)
            .withIdentity(JobKey.jobKey(workflowExecutionId.toString(), "ScheduleTrigger"))
            .usingJobData("workflowExecutionId", workflowExecutionId.toString())
            .build();

        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(workflowExecutionId.toString(), "ScheduleTrigger"))
            .startAt(DateUtils.getDate(webhookExpirationDate))
            .build();

        schedule(jobDetail, trigger);
    }

    @Override
    public void scheduleScheduleTrigger(
        String pattern, String zoneId, Map<String, Object> output, WorkflowExecutionId workflowExecutionId) {

        JobDetail jobDetail = JobBuilder.newJob(ScheduleTriggerJob.class)
            .withIdentity(JobKey.jobKey(workflowExecutionId.toString(), "ScheduleTrigger"))
            .usingJobData("output", JsonUtils.write(output, objectMapper))
            .usingJobData("workflowExecutionId", workflowExecutionId.toString())
            .build();

        CronExpression cronExpression;

        try {
            cronExpression = new CronExpression(pattern);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        cronExpression.setTimeZone(TimeZone.getTimeZone(ZoneId.of(zoneId)));

        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(workflowExecutionId.toString(), "ScheduleTrigger"))
            .withSchedule(
                CronScheduleBuilder.cronSchedule(pattern))
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
                SimpleScheduleBuilder.repeatMinutelyForever(5))
            .startNow()
            .build();

        schedule(jobDetail, trigger);
    }

    private void deleteJob(String workflowExecutionId, String PollingTrigger) {
        try {
            scheduler.deleteJob(JobKey.jobKey(workflowExecutionId, PollingTrigger));
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private void schedule(JobDetail jobDetail, Trigger trigger) {
        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
}
