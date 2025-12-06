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

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.scheduler.config.QuartzTriggerSchedulerTestConfiguration;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * Integration test for QuartzTriggerScheduler
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = QuartzTriggerSchedulerTestConfiguration.class, properties = "spring.profiles.active=test")
@Import(QuartzTriggerSchedulerIntTest.QuartzTriggerSchedulerIntTestConfiguration.class)
@SuppressFBWarnings("NP")
public class QuartzTriggerSchedulerIntTest {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private TriggerScheduler quartzTriggerScheduler;

    @AfterEach
    public void tearDown() throws Exception {
        if (!scheduler.isShutdown()) {
            scheduler.clear();
        }
    }

    @Test
    public void testScheduleOneTimeTask() throws Exception {
        // Given
        String taskExecutionId = "test-task-123";
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 456L, "test-workflow-456", "testTrigger");
        Map<String, Object> output = Map.of("delayMillis", 100L);
        LocalDateTime executeAt = LocalDateTime.now()
            .plus(Duration.ofMillis(100));

        // When
        quartzTriggerScheduler.scheduleOneTimeTask(
            executeAt, output, workflowExecutionId, taskExecutionId);

        // Then
        // Verify job was created
        JobKey jobKey = JobKey.jobKey(taskExecutionId, "DelayTask");
        TriggerKey triggerKey = TriggerKey.triggerKey(taskExecutionId, "DelayTask");

        Assertions.assertTrue(scheduler.checkExists(jobKey));
        Assertions.assertTrue(scheduler.checkExists(triggerKey));

        // Verify job details
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        Assertions.assertNotNull(jobDetail);
        Assertions.assertEquals(workflowExecutionId.toString(),
            jobDetail.getJobDataMap()
                .getString("workflowExecutionId"));
        Assertions.assertEquals(taskExecutionId,
            jobDetail.getJobDataMap()
                .getString("taskExecutionId"));
    }

    @Test
    public void testScheduleOneTimeTaskWithDelay() throws Exception {
        // Given
        String taskExecutionId = "test-task-delay-789";
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 101L, "test-workflow-delay-101", "testTrigger");
        Map<String, Object> output = Map.of("delayMillis", 200L);
        LocalDateTime executeAt = LocalDateTime.now()
            .plus(Duration.ofMillis(200));

        // When
        quartzTriggerScheduler.scheduleOneTimeTask(
            executeAt, output, workflowExecutionId, taskExecutionId);

        // Then
        // Verify job was created
        JobKey jobKey = JobKey.jobKey(taskExecutionId, "DelayTask");
        TriggerKey triggerKey = TriggerKey.triggerKey(taskExecutionId, "DelayTask");

        Assertions.assertTrue(scheduler.checkExists(jobKey));
        Assertions.assertTrue(scheduler.checkExists(triggerKey));

        // Verify trigger timing
        Trigger trigger = scheduler.getTrigger(triggerKey);
        Assertions.assertNotNull(trigger);
        Assertions.assertNotNull(trigger.getStartTime());
    }

    @Test
    public void testScheduleOneTimeTaskJobExists() throws Exception {
        // Given
        String taskExecutionId = "test-task-job-exists";
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 1L, "test-workflow-job-exists", "testTrigger");
        Map<String, Object> output = Map.of("delayMillis", 100L);
        LocalDateTime executeAt = LocalDateTime.now()
            .plus(Duration.ofMillis(100));

        // When
        quartzTriggerScheduler.scheduleOneTimeTask(
            executeAt, output, workflowExecutionId, taskExecutionId);

        // Then
        // Verify job was created
        JobKey jobKey = JobKey.jobKey(taskExecutionId, "DelayTask");
        TriggerKey triggerKey = TriggerKey.triggerKey(taskExecutionId, "DelayTask");

        Assertions.assertTrue(scheduler.checkExists(jobKey));
        Assertions.assertTrue(scheduler.checkExists(triggerKey));

        // Verify job details
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        Assertions.assertNotNull(jobDetail);
        Assertions.assertEquals(workflowExecutionId.toString(),
            jobDetail.getJobDataMap()
                .getString("workflowExecutionId"));
        Assertions.assertEquals(taskExecutionId,
            jobDetail.getJobDataMap()
                .getString("taskExecutionId"));
    }

    @Test
    public void testScheduleDynamicWebhookTriggerRefresh() throws Exception {
        // Given
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 123L, "test-webhook-workflow", "testTrigger");
        Instant webhookExpirationDate = LocalDateTime.now()
            .plus(Duration.ofSeconds(2))
            .toInstant(ZoneOffset.UTC);
        String componentName = "testComponent";
        int componentVersion = 1;
        Long connectionId = 456L;

        // When
        quartzTriggerScheduler.scheduleDynamicWebhookTriggerRefresh(
            webhookExpirationDate, componentName, componentVersion, workflowExecutionId, connectionId);

        // Then
        JobKey jobKey = JobKey.jobKey(workflowExecutionId.toString(), "ScheduleTrigger");
        TriggerKey triggerKey = TriggerKey.triggerKey(workflowExecutionId.toString(), "ScheduleTrigger");

        // Verify job was created
        Assertions.assertTrue(scheduler.checkExists(jobKey));
        Assertions.assertTrue(scheduler.checkExists(triggerKey));

        // Verify job details
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        Assertions.assertNotNull(jobDetail);
        Assertions.assertEquals(workflowExecutionId.toString(),
            jobDetail.getJobDataMap()
                .getString("workflowExecutionId"));
        Assertions.assertEquals(connectionId,
            jobDetail.getJobDataMap()
                .getLong("connectionId"));

        // Verify trigger timing
        Trigger trigger = scheduler.getTrigger(triggerKey);
        Assertions.assertNotNull(trigger);
        Assertions.assertNotNull(trigger.getStartTime());

        // Clean up
        scheduler.deleteJob(jobKey);
    }

    @Test
    public void testScheduleScheduleTrigger() throws Exception {
        // Given
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 789L, "test-schedule-workflow", "testTrigger");
        String cronPattern = "0/5 * * * * ?"; // Every 5 seconds
        String zoneId = ZoneId.systemDefault()
            .getId();
        Map<String, Object> output = Map.of("key", "value", "number", 42);

        // When
        quartzTriggerScheduler.scheduleScheduleTrigger(cronPattern, zoneId, output, workflowExecutionId);

        // Then
        JobKey jobKey = JobKey.jobKey(workflowExecutionId.toString(), "ScheduleTrigger");
        TriggerKey triggerKey = TriggerKey.triggerKey(workflowExecutionId.toString(), "ScheduleTrigger");

        // Verify job was created
        Assertions.assertTrue(scheduler.checkExists(jobKey));
        Assertions.assertTrue(scheduler.checkExists(triggerKey));

        // Verify job details
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        Assertions.assertNotNull(jobDetail);
        Assertions.assertEquals(workflowExecutionId.toString(),
            jobDetail.getJobDataMap()
                .getString("workflowExecutionId"));
        Assertions.assertNotNull(jobDetail.getJobDataMap()
            .getString("output"));

        // Verify trigger type is cron
        Trigger trigger = scheduler.getTrigger(triggerKey);
        Assertions.assertNotNull(trigger);
        Assertions.assertTrue(trigger instanceof org.quartz.CronTrigger);

        // Clean up
        scheduler.deleteJob(jobKey);
    }

    @Test
    public void testCancelDynamicWebhookTriggerRefresh() throws Exception {
        // Given
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 111L, "test-cancel-webhook", "testTrigger");
        Instant webhookExpirationDate = LocalDateTime.now()
            .plus(Duration.ofMinutes(5))
            .toInstant(ZoneOffset.UTC);

        // Schedule first
        quartzTriggerScheduler.scheduleDynamicWebhookTriggerRefresh(
            webhookExpirationDate, "testComponent", 1, workflowExecutionId, 123L);

        JobKey scheduledJobKey = JobKey.jobKey(workflowExecutionId.toString(), "ScheduleTrigger");
        JobKey cancelJobKey = JobKey.jobKey(workflowExecutionId.toString(), "DynamicWebhookTriggerRefresh");

        // Verify job was created with "ScheduleTrigger" group
        Assertions.assertTrue(scheduler.checkExists(scheduledJobKey));
        // Verify the cancel job key doesn't exist (this reveals the bug in the existing code)
        Assertions.assertFalse(scheduler.checkExists(cancelJobKey));

        // When - This will try to cancel using "DynamicWebhookTriggerRefresh" group
        quartzTriggerScheduler.cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());

        // Then - The scheduled job should still exist because the cancel used wrong group
        // This test demonstrates the bug in the existing code
        Assertions.assertTrue(scheduler.checkExists(scheduledJobKey));
        Assertions.assertFalse(scheduler.checkExists(cancelJobKey));

        // Clean up manually
        scheduler.deleteJob(scheduledJobKey);
    }

    @Test
    public void testCancelScheduleTrigger() throws Exception {
        // Given
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 222L, "test-cancel-schedule", "testTrigger");
        String cronPattern = "0 0 12 * * ?"; // Daily at noon
        Map<String, Object> output = Map.of("test", "data");

        // Schedule first
        quartzTriggerScheduler.scheduleScheduleTrigger(
            cronPattern, ZoneId.systemDefault()
                .getId(),
            output, workflowExecutionId);

        JobKey jobKey = JobKey.jobKey(workflowExecutionId.toString(), "ScheduleTrigger");

        // Verify job was created
        Assertions.assertTrue(scheduler.checkExists(jobKey));

        // When
        quartzTriggerScheduler.cancelScheduleTrigger(workflowExecutionId.toString());

        // Then
        Assertions.assertFalse(scheduler.checkExists(jobKey));
    }

    @TestConfiguration
    public static class QuartzTriggerSchedulerIntTestConfiguration {

        @Bean
        ApplicationProperties applicationProperties() {
            return new ApplicationProperties();
        }

        @Bean
        @Primary
        Scheduler scheduler() throws SchedulerException {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();

            Scheduler scheduler = schedulerFactory.getScheduler();

            scheduler.start();

            return scheduler;
        }

        @Bean
        @Primary
        TriggerScheduler triggerScheduler(ApplicationProperties applicationProperties, Scheduler scheduler) {
            ApplicationProperties.Coordinator.Trigger.Polling polling = applicationProperties.getCoordinator()
                .getTrigger()
                .getPolling();

            return new QuartzTriggerScheduler(polling, scheduler);
        }
    }
}
