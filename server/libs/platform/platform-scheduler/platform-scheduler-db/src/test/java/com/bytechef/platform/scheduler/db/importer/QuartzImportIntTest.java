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

package com.bytechef.platform.scheduler.db.importer;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.scheduler.db.config.DbSchedulerTestConfiguration;
import com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors;
import com.bytechef.platform.scheduler.db.task.ScheduleTriggerData;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(
    classes = DbSchedulerTestConfiguration.class,
    properties = {
        "spring.profiles.active=test", "bytechef.scheduler.db-scheduler.importer.enabled=false"
    })
@Import(PostgreSQLContainerConfiguration.class)
class QuartzImportIntTest {

    public static class ScheduleTriggerJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
        }
    }

    public static class PollingTriggerJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
        }
    }

    public static class DynamicWebhookTriggerRefreshJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
        }
    }

    public static class ConnectionOAuth2TokenRefreshJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
        }
    }

    public static class OneTimeSchedulerJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
        }
    }

    public static class StripeUsageReportingJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Scheduler quartzScheduler;

    @Autowired
    private SchedulerClient schedulerClient;

    private final Instant future = Instant.now()
        .plus(Duration.ofDays(1))
        .truncatedTo(ChronoUnit.MILLIS);

    @AfterEach
    void tearDown() throws SchedulerException {
        quartzScheduler.clear();
        jdbcTemplate.update("DELETE FROM scheduled_tasks");
    }

    @Test
    void testImportsEveryKindPreservesNextFireTimeIsIdempotentAndLeavesQuartzUntouched() throws SchedulerException {
        WorkflowExecutionId scheduleId = WorkflowExecutionId.of(PlatformType.AUTOMATION, 1L, "s", "t");
        WorkflowExecutionId pollingId = WorkflowExecutionId.of(PlatformType.AUTOMATION, 2L, "p", "t");
        WorkflowExecutionId webhookId = WorkflowExecutionId.of(PlatformType.AUTOMATION, 3L, "w", "t");

        quartzScheduler.scheduleJob(
            JobBuilder.newJob(ScheduleTriggerJob.class)
                .withIdentity(scheduleId.toString(), "ScheduleTrigger")
                .usingJobData("output", "{\"expression\":\"0 9 * * *\",\"timezone\":\"Europe/Zagreb\"}")
                .usingJobData("workflowExecutionId", scheduleId.toString())
                .build(),
            TriggerBuilder.newTrigger()
                .withIdentity(scheduleId.toString(), "ScheduleTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 9 * * ?")
                    .inTimeZone(TimeZone.getTimeZone("Europe/Zagreb")))
                .startNow()
                .build());
        quartzScheduler.scheduleJob(
            JobBuilder.newJob(PollingTriggerJob.class)
                .withIdentity(pollingId.toString(), "PollingTrigger")
                .usingJobData("workflowExecutionId", pollingId.toString())
                .build(),
            TriggerBuilder.newTrigger()
                .withIdentity(pollingId.toString(), "PollingTrigger")
                .withSchedule(SimpleScheduleBuilder.repeatMinutelyForever(5))
                .startAt(Date.from(future))
                .build());
        quartzScheduler.scheduleJob(
            JobBuilder.newJob(DynamicWebhookTriggerRefreshJob.class)
                .withIdentity(webhookId.toString(), "ScheduleTrigger")
                .usingJobData("workflowExecutionId", webhookId.toString())
                .usingJobData("connectionId", 77L)
                .build(),
            TriggerBuilder.newTrigger()
                .withIdentity(webhookId.toString(), "ScheduleTrigger")
                .startAt(Date.from(future))
                .build());
        quartzScheduler.scheduleJob(
            JobBuilder.newJob(ConnectionOAuth2TokenRefreshJob.class)
                .withIdentity("public77", "ConnectionOauth2TokenRefresh")
                .usingJobData("connectionId", 77L)
                .usingJobData("tenantId", "public")
                .build(),
            TriggerBuilder.newTrigger()
                .withIdentity("public77", "ConnectionOauth2TokenRefresh")
                .startAt(Date.from(future))
                .build());
        quartzScheduler.scheduleJob(
            JobBuilder.newJob(OneTimeSchedulerJob.class)
                .withIdentity("42", "OneTimeTask")
                .usingJobData("jobId", 42L)
                .usingJobData("continueParameters", "{\"k\":\"v\"}")
                .build(),
            TriggerBuilder.newTrigger()
                .withIdentity("42", "OneTimeTask")
                .startAt(Date.from(future))
                .build());
        quartzScheduler.addJob(
            JobBuilder.newJob(StripeUsageReportingJob.class)
                .withIdentity("stripeUsageReportingJob")
                .storeDurably()
                .build(),
            true);

        Trigger scheduleTrigger = quartzScheduler.getTrigger(
            TriggerKey.triggerKey(scheduleId.toString(), "ScheduleTrigger"));
        Instant scheduleNextFireTime = scheduleTrigger.getNextFireTime()
            .toInstant();
        List<Map<String, Object>> quartzRowsBefore = jdbcTemplate.queryForList(
            "SELECT job_name, job_group, job_class_name FROM qrtz_job_details ORDER BY job_name");

        QuartzImporter importer = new QuartzImporter(new QuartzJobReader(quartzScheduler), schedulerClient, 5);

        ImportSummary first = importer.importJobs();
        ImportSummary second = importer.importJobs();

        Assertions.assertThat(first)
            .isEqualTo(new ImportSummary(5, 5, 0, 1, 0, 0, 0, true));
        Assertions.assertThat(second)
            .isEqualTo(new ImportSummary(5, 0, 5, 1, 0, 0, 0, true));

        Assertions.assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM scheduled_tasks WHERE task_name IN (?, ?, ?, ?, ?)", Integer.class,
            DbSchedulerTaskDescriptors.SCHEDULE_TRIGGER_NAME, DbSchedulerTaskDescriptors.POLLING_TRIGGER_NAME,
            DbSchedulerTaskDescriptors.DYNAMIC_WEBHOOK_REFRESH_NAME,
            DbSchedulerTaskDescriptors.OAUTH2_TOKEN_REFRESH_NAME, DbSchedulerTaskDescriptors.ONE_TIME_RESUME_NAME))
            .as("only the five task kinds actually imported in this test, independent of any other task's rows")
            .isEqualTo(5);

        Assertions.assertThat(schedulerClient.getScheduledExecution(
            DbSchedulerTaskDescriptors.SCHEDULE_TRIGGER.instanceId(scheduleId.toString())))
            .get()
            .satisfies(execution -> {
                Assertions.assertThat(execution.getExecutionTime())
                    .as("cron instance keeps Quartz's next fire time instead of recomputing from now")
                    .isEqualTo(scheduleNextFireTime);
                Assertions.assertThat((ScheduleTriggerData) execution.getData())
                    .isEqualTo(new ScheduleTriggerData(
                        "0 0 9 * * ?", "Europe/Zagreb",
                        "{\"expression\":\"0 9 * * *\",\"timezone\":\"Europe/Zagreb\"}"));
            });
        Assertions.assertThat(schedulerClient.getScheduledExecution(
            DbSchedulerTaskDescriptors.DYNAMIC_WEBHOOK_REFRESH.instanceId(webhookId.toString())))
            .as("broken-group dynamic webhook row imports into its own task")
            .get()
            .satisfies(execution -> Assertions.assertThat(execution.getExecutionTime())
                .isEqualTo(future));
        Assertions.assertThat(schedulerClient.getScheduledExecution(
            DbSchedulerTaskDescriptors.OAUTH2_TOKEN_REFRESH.instanceId("public77")))
            .get()
            .satisfies(execution -> Assertions.assertThat(execution.getExecutionTime())
                .isEqualTo(future));
        Assertions.assertThat(schedulerClient.getScheduledExecution(
            DbSchedulerTaskDescriptors.ONE_TIME_RESUME.instanceId("42")))
            .isPresent();
        Assertions.assertThat(schedulerClient.getScheduledExecution(
            DbSchedulerTaskDescriptors.POLLING_TRIGGER.instanceId(pollingId.toString())))
            .isPresent();

        Assertions.assertThat(jdbcTemplate.queryForList(
            "SELECT job_name, job_group, job_class_name FROM qrtz_job_details ORDER BY job_name"))
            .as("Quartz rows are untouched")
            .isEqualTo(quartzRowsBefore);
        Assertions.assertThat(quartzScheduler.checkExists(JobKey.jobKey("42", "OneTimeTask")))
            .isTrue();
    }

    @Test
    void testCompletedQuartzTriggerIsSkipped() throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(OneTimeSchedulerJob.class)
            .withIdentity("99", "OneTimeTask")
            .usingJobData("jobId", 99L)
            .storeDurably()
            .build();

        quartzScheduler.addJob(jobDetail, true);

        QuartzImporter importer = new QuartzImporter(new QuartzJobReader(quartzScheduler), schedulerClient, 5);

        Assertions.assertThat(importer.importJobs()
            .skippedComplete())
            .isEqualTo(1);
        Assertions.assertThat(schedulerClient.getScheduledExecution(
            DbSchedulerTaskDescriptors.ONE_TIME_RESUME.instanceId("99")))
            .isEmpty();
    }
}
