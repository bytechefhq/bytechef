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

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.OperableTrigger;

/**
 * @author Ivica Cardic
 */
class QuartzJobReaderTest {

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

    public static class SomethingElseJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
        }
    }

    private final Scheduler scheduler = Mockito.mock(Scheduler.class);
    private final QuartzJobReader reader = new QuartzJobReader(scheduler);
    private final Date nextFireTime = Date.from(Instant.parse("2031-01-01T00:00:00Z"));

    @Test
    void testReadsScheduleTriggerWithCronZoneAndOutput() throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(ScheduleTriggerJob.class)
            .withIdentity("wfe-1", "ScheduleTrigger")
            .usingJobData("workflowExecutionId", "wfe-1")
            .usingJobData("output", "{\"expression\":\"0 9 * * *\"}")
            .build();
        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity("wfe-1", "ScheduleTrigger")
            .withSchedule(CronScheduleBuilder.cronSchedule("0 0 9 * * ?")
                .inTimeZone(TimeZone.getTimeZone("Europe/Zagreb")))
            .startAt(nextFireTime)
            .build();

        computeFirstFireTime(trigger);
        stub(jobDetail, trigger);

        QuartzJobReader.ReadResult result = reader.read();

        Assertions.assertThat(result.jobs())
            .containsExactly(new ImportedJob.ScheduleTrigger(
                "wfe-1", "0 0 9 * * ?", "Europe/Zagreb", "{\"expression\":\"0 9 * * *\"}",
                trigger.getNextFireTime()
                    .toInstant()));
        Mockito.verify(scheduler, Mockito.times(1))
            .getTriggersOfJob(jobDetail.getKey());
    }

    @Test
    void testScheduleTriggerWithNonCronTriggerIsCountedAsFailedNotThrown() throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(ScheduleTriggerJob.class)
            .withIdentity("wfe-1b", "ScheduleTrigger")
            .usingJobData("workflowExecutionId", "wfe-1b")
            .usingJobData("output", "{}")
            .build();
        Trigger simpleTrigger = TriggerBuilder.newTrigger()
            .withIdentity("wfe-1b", "ScheduleTrigger")
            .withSchedule(SimpleScheduleBuilder.repeatMinutelyForever(5))
            .startAt(nextFireTime)
            .build();

        stub(jobDetail, computeFirstFireTime(simpleTrigger));

        QuartzJobReader.ReadResult result = reader.read();

        Assertions.assertThat(result.jobs())
            .isEmpty();
        Assertions.assertThat(result.failed())
            .isEqualTo(1);
    }

    @Test
    void testReadsDynamicWebhookRefreshUsingTheKeyTheWriterUsed() throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(DynamicWebhookTriggerRefreshJob.class)
            .withIdentity("wfe-2", "ScheduleTrigger")
            .usingJobData("workflowExecutionId", "wfe-2")
            .usingJobData("connectionId", 77L)
            .build();

        stub(jobDetail, oneShot("wfe-2", "ScheduleTrigger"));

        Assertions.assertThat(reader.read()
            .jobs())
            .containsExactly(new ImportedJob.DynamicWebhookRefresh("wfe-2", 77L, nextFireTime.toInstant()));
    }

    @Test
    void testReadsDynamicWebhookRefreshWithHistoricalIntegerConnectionId() throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(DynamicWebhookTriggerRefreshJob.class)
            .withIdentity("wfe-2b", "ScheduleTrigger")
            .usingJobData("workflowExecutionId", "wfe-2b")
            .usingJobData("connectionId", 77)
            .build();

        stub(jobDetail, oneShot("wfe-2b", "ScheduleTrigger"));

        Assertions.assertThat(reader.read()
            .jobs())
            .containsExactly(new ImportedJob.DynamicWebhookRefresh("wfe-2b", 77L, nextFireTime.toInstant()));
    }

    @Test
    void testReadsOAuth2RefreshWithHistoricalIntegerTenantId() throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(ConnectionOAuth2TokenRefreshJob.class)
            .withIdentity("00000178", "ConnectionOauth2TokenRefresh")
            .usingJobData("connectionId", 78L)
            .usingJobData("tenantId", 1)
            .build();

        stub(jobDetail, oneShot("00000178", "ConnectionOauth2TokenRefresh"));

        Assertions.assertThat(reader.read()
            .jobs())
            .containsExactly(new ImportedJob.OAuth2TokenRefresh("00000178", 78L, "1", nextFireTime.toInstant()));
    }

    @Test
    void testReadsOAuth2RefreshPollingAndOneTimeResume() throws SchedulerException {
        JobDetail oauth = JobBuilder.newJob(ConnectionOAuth2TokenRefreshJob.class)
            .withIdentity("00000177", "ConnectionOauth2TokenRefresh")
            .usingJobData("connectionId", 77L)
            .usingJobData("tenantId", "000001")
            .build();
        JobDetail polling = JobBuilder.newJob(PollingTriggerJob.class)
            .withIdentity("wfe-3", "PollingTrigger")
            .usingJobData("workflowExecutionId", "wfe-3")
            .build();
        JobDetail resume = JobBuilder.newJob(OneTimeSchedulerJob.class)
            .withIdentity("000001:42", "OneTimeTask")
            .usingJobData("jobId", 42L)
            .usingJobData("tenantId", "000001")
            .build();
        Trigger pollingTrigger = TriggerBuilder.newTrigger()
            .withIdentity("wfe-3", "PollingTrigger")
            .withSchedule(SimpleScheduleBuilder.repeatMinutelyForever(5))
            .startAt(nextFireTime)
            .build();

        computeFirstFireTime(pollingTrigger);
        stub(List.of(oauth, polling, resume),
            List.of(oneShot("00000177", "ConnectionOauth2TokenRefresh"), pollingTrigger,
                oneShot("000001:42", "OneTimeTask")));

        Assertions.assertThat(reader.read()
            .jobs())
            .containsExactlyInAnyOrder(
                new ImportedJob.OAuth2TokenRefresh("00000177", 77L, "000001", nextFireTime.toInstant()),
                new ImportedJob.PollingTrigger("wfe-3", nextFireTime.toInstant()),
                new ImportedJob.OneTimeResume("000001:42", 42L, null, "000001", nextFireTime.toInstant()));
    }

    @Test
    void testReadsOneTimeResumeWithHistoricalIntegerJobId() throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(OneTimeSchedulerJob.class)
            .withIdentity("44", "OneTimeTask")
            .usingJobData("jobId", 44)
            .usingJobData("continueParameters", "{\"step\":\"1\"}")
            .build();

        stub(jobDetail, oneShot("44", "OneTimeTask"));

        Assertions.assertThat(reader.read()
            .jobs())
            .containsExactly(
                new ImportedJob.OneTimeResume("44", 44L, "{\"step\":\"1\"}", null, nextFireTime.toInstant()));
    }

    @Test
    void testSkipsStaticUnknownAndCompleteJobsAndCounts() throws SchedulerException {
        JobDetail stripe = JobBuilder.newJob(StripeUsageReportingJob.class)
            .withIdentity("stripeUsageReportingJob")
            .build();
        JobDetail unknown = JobBuilder.newJob(SomethingElseJob.class)
            .withIdentity("x", "y")
            .build();
        JobDetail complete = JobBuilder.newJob(PollingTriggerJob.class)
            .withIdentity("wfe-4", "PollingTrigger")
            .usingJobData("workflowExecutionId", "wfe-4")
            .build();

        Mockito.when(scheduler.getJobGroupNames())
            .thenReturn(List.of("DEFAULT", "y", "PollingTrigger"));
        Mockito.when(scheduler.getJobKeys(Mockito.any(GroupMatcher.class)))
            .thenAnswer(invocation -> Set.of(stripe.getKey(), unknown.getKey(), complete.getKey()));
        Mockito.when(scheduler.getJobDetail(stripe.getKey()))
            .thenReturn(stripe);
        Mockito.when(scheduler.getJobDetail(unknown.getKey()))
            .thenReturn(unknown);
        Mockito.when(scheduler.getJobDetail(complete.getKey()))
            .thenReturn(complete);
        Mockito.when(scheduler.getTriggersOfJob(Mockito.any(JobKey.class)))
            .thenReturn(List.of());

        QuartzJobReader.ReadResult result = reader.read();

        Assertions.assertThat(result.jobs())
            .isEmpty();
        Assertions.assertThat(result.skippedStatic())
            .isEqualTo(1);
        Assertions.assertThat(result.skippedUnknown())
            .isEqualTo(1);
        Assertions.assertThat(result.skippedComplete())
            .isEqualTo(1);
    }

    @Test
    void testMappingFailureIsCountedAndDoesNotAbort() throws SchedulerException {
        JobDetail broken = JobBuilder.newJob(OneTimeSchedulerJob.class)
            .withIdentity("broken", "OneTimeTask")
            .build();
        JobDetail good = JobBuilder.newJob(OneTimeSchedulerJob.class)
            .withIdentity("43", "OneTimeTask")
            .usingJobData("jobId", 43L)
            .build();

        stub(List.of(broken, good), List.of(oneShot("broken", "OneTimeTask"), oneShot("43", "OneTimeTask")));

        QuartzJobReader.ReadResult result = reader.read();

        Assertions.assertThat(result.failed())
            .isEqualTo(1);
        Assertions.assertThat(result.jobs())
            .containsExactly(new ImportedJob.OneTimeResume("43", 43L, null, null, nextFireTime.toInstant()));
    }

    @Test
    void testUnreadableJobStoreYieldsEmptyResult() throws SchedulerException {
        Mockito.when(scheduler.getJobGroupNames())
            .thenThrow(new SchedulerException("relation qrtz_job_details does not exist"));

        QuartzJobReader.ReadResult result = reader.read();

        Assertions.assertThat(result.quartzReadable())
            .isFalse();
        Assertions.assertThat(result.jobs())
            .isEmpty();
    }

    private Trigger oneShot(String name, String group) {
        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(name, group)
            .startAt(nextFireTime)
            .build();

        return computeFirstFireTime(trigger);
    }

    private static Trigger computeFirstFireTime(Trigger trigger) {
        ((OperableTrigger) trigger).computeFirstFireTime(null);

        return trigger;
    }

    private void stub(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
        stub(List.of(jobDetail), List.of(trigger));
    }

    private void stub(List<JobDetail> jobDetails, List<Trigger> triggers) throws SchedulerException {
        Mockito.when(scheduler.getJobGroupNames())
            .thenReturn(List.of("any"));
        Mockito.when(scheduler.getJobKeys(Mockito.any(GroupMatcher.class)))
            .thenReturn(Set.copyOf(jobDetails.stream()
                .map(JobDetail::getKey)
                .toList()));

        for (int index = 0; index < jobDetails.size(); index++) {
            JobDetail jobDetail = jobDetails.get(index);
            Trigger trigger = triggers.get(index);

            Mockito.when(scheduler.getJobDetail(jobDetail.getKey()))
                .thenReturn(jobDetail);
            Mockito.doReturn(List.of(trigger))
                .when(scheduler)
                .getTriggersOfJob(jobDetail.getKey());
        }
    }
}
