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
import com.bytechef.platform.scheduler.config.QuartzJdbcTestConfiguration;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test to reproduce Quartz 2.5.1 column name case sensitivity bug. This test demonstrates the issue where
 * Quartz 2.5.1 fails to load JobDataMap and trigger extended properties from the database when column names are
 * returned in lowercase (which PostgreSQL does by default). Root cause: StdJDBCDelegate.containsColumnNames() uses
 * case-sensitive comparison between rs.getMetaData().getColumnName(i) (lowercase in PostgreSQL) and COL_* constants
 * (uppercase). Error manifestation: - JobDataMap is null even though data exists in database - Trigger retrieval fails
 * with NoRecordFoundException - "Couldn't retrieve trigger: No record found for selection of Trigger" - MisfireHandler
 * errors: "Error retrieving the misfired trigger" See: https://github.com/quartz-scheduler/quartz/issues/943
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = QuartzJdbcTestConfiguration.class)
@Import({
    PostgreSQLContainerConfiguration.class, QuartzIntTest.QuartzJdbcConfiguration.class
})
@Testcontainers
@SuppressFBWarnings("NP")
public class QuartzIntTest {

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

    /**
     * Tests that JobDataMap is correctly loaded when scheduling a polling trigger. In Quartz 2.5.1 with PostgreSQL
     * (which returns lowercase column names), this test WILL FAIL because: - JobDataMap will be null despite data being
     * stored in the database - containsColumnNames() fails to find "job_data" column (checks "JOB_DATA")
     */
    @Test
    public void testPollingTriggerJobDataMapLoaded() throws Exception {
        // Given: A workflow execution ID with job data
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 1059L, "50849def-93e2-44eb-aa05-797ebf849954", "trigger_1");

        // When: Schedule the polling trigger (stores in PostgreSQL)
        quartzTriggerScheduler.schedulePollingTrigger(workflowExecutionId);

        JobKey jobKey = JobKey.jobKey(workflowExecutionId.toString(), "PollingTrigger");

        // Then: Retrieve the job and verify JobDataMap is loaded
        // THIS WILL FAIL in Quartz 2.5.1 with PostgreSQL
        JobDetail retrievedJob = scheduler.getJobDetail(jobKey);

        Assertions.assertNotNull(retrievedJob, "Job detail should be retrieved");

        JobDataMap jobDataMap = retrievedJob.getJobDataMap();

        Assertions.assertNotNull(jobDataMap,
            "JobDataMap should not be null - FAILS in Quartz 2.5.1 with PostgreSQL lowercase column names");
        Assertions.assertEquals(
            workflowExecutionId.toString(), jobDataMap.getString("workflowExecutionId"),
            "WorkflowExecutionId should be loaded from JobDataMap");
    }

    /**
     * Tests that CronTrigger extended properties are correctly loaded from PostgreSQL. In Quartz 2.5.1, this WILL FAIL
     * with NoRecordFoundException because: - CronTriggerPersistenceDelegate can't find extended properties -
     * containsColumnNames() fails to match lowercase column names
     */
    @Test
    public void testScheduleTriggerWithJobDataMap() throws Exception {
        // Given: A cron schedule with output data in JobDataMap
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 789L, "test-schedule-workflow", "testTrigger");
        String cronPattern = "0/5 * * * * ?"; // Every 5 seconds
        Map<String, Object> output = Map.of("key", "value", "number", 42);

        ZoneId systemDefault = ZoneId.systemDefault();

        String zoneId = systemDefault.getId();

        // When: Schedule the cron trigger (stores in PostgreSQL)
        quartzTriggerScheduler.scheduleScheduleTrigger(cronPattern, zoneId, output, workflowExecutionId);

        JobKey jobKey = JobKey.jobKey(workflowExecutionId.toString(), "ScheduleTrigger");
        TriggerKey triggerKey = TriggerKey.triggerKey(workflowExecutionId.toString(), "ScheduleTrigger");

        // Then: Retrieve and verify both job data and trigger properties
        // THIS WILL FAIL in Quartz 2.5.1 with PostgreSQL
        JobDetail retrievedJob = scheduler.getJobDetail(jobKey);

        Assertions.assertNotNull(retrievedJob, "Job detail should be retrieved");

        JobDataMap jobDataMap = retrievedJob.getJobDataMap();

        Assertions.assertNotNull(jobDataMap, "JobDataMap should not be null - FAILS with PostgreSQL");
        Assertions.assertNotNull(jobDataMap.getString("output"), "Output data should be loaded from JobDataMap");

        // Retrieve trigger - THIS THROWS NoRecordFoundException in Quartz 2.5.1
        Trigger retrievedTrigger = scheduler.getTrigger(triggerKey);

        Assertions.assertNotNull(retrievedTrigger,
            "Trigger should be retrieved - FAILS in Quartz 2.5.1 with NoRecordFoundException");
        Assertions.assertInstanceOf(CronTrigger.class, retrievedTrigger, "Trigger should be a CronTrigger");
    }

    /**
     * Tests SimpleTrigger (webhook refresh) with JobDataMap and PostgreSQL. SimpleTrigger extended properties are also
     * affected by the column name case bug.
     */
    @Test
    public void testDynamicWebhookTriggerJobDataMap() throws Exception {
        // Given: A webhook refresh trigger with connection data
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 123L, "test-webhook-workflow", "testTrigger");

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime webhookExpirationDate = now.plusMinutes(5);

        String componentName = "testComponent";
        int componentVersion = 1;
        Long connectionId = 456L;

        // When: Schedule the webhook refresh (stores in PostgreSQL)
        quartzTriggerScheduler.scheduleDynamicWebhookTriggerRefresh(
            webhookExpirationDate, componentName, componentVersion, workflowExecutionId, connectionId);

        JobKey jobKey = JobKey.jobKey(workflowExecutionId.toString(), "ScheduleTrigger");
        TriggerKey triggerKey = TriggerKey.triggerKey(workflowExecutionId.toString(), "ScheduleTrigger");

        // Then: Verify JobDataMap and trigger are loaded correctly
        // THIS WILL FAIL in Quartz 2.5.1 with PostgreSQL
        JobDetail retrievedJob = scheduler.getJobDetail(jobKey);

        Assertions.assertNotNull(retrievedJob, "Job detail should be retrieved");

        JobDataMap jobDataMap = retrievedJob.getJobDataMap();

        Assertions.assertNotNull(jobDataMap, "JobDataMap should not be null - FAILS with PostgreSQL");
        Assertions.assertEquals(workflowExecutionId.toString(),
            jobDataMap.getString("workflowExecutionId"), "WorkflowExecutionId should be in JobDataMap");
        Assertions.assertEquals(
            connectionId, jobDataMap.getLong("connectionId"), "ConnectionId should be in JobDataMap");

        // Retrieve SimpleTrigger - THIS FAILS in Quartz 2.5.1
        Trigger retrievedTrigger = scheduler.getTrigger(triggerKey);

        Assertions.assertNotNull(
            retrievedTrigger, "SimpleTrigger should be retrieved - FAILS with NoRecordFoundException");
        Assertions.assertNotNull(retrievedTrigger.getStartTime(), "Start time should be set");
    }

    /**
     * Tests that scheduler can handle jobs after being started/stopped with PostgreSQL. This simulates the real-world
     * scenario where MisfireHandler runs and tries to retrieve triggers from the PostgreSQL database. This is the exact
     * scenario that produces the error: "Error retrieving the misfired trigger: PollingTrigger.xxx"
     */
    @Test
    public void testSchedulerStartStopWithPostgreSQL() throws Exception {
        // Given: A polling trigger stored in PostgreSQL
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 2000L, "test-restart-workflow", "trigger_restart");

        quartzTriggerScheduler.schedulePollingTrigger(workflowExecutionId);

        JobKey jobKey = JobKey.jobKey(workflowExecutionId.toString(), "PollingTrigger");
        TriggerKey triggerKey = TriggerKey.triggerKey(workflowExecutionId.toString(), "PollingTrigger");

        // When: Pause and resume scheduler
        // This triggers MisfireHandler which tries to retrieve triggers from PostgreSQL
        scheduler.standby();

        // Small delay to let any background threads settle
        Thread.sleep(100);

        scheduler.start();

        // Then: Verify we can still retrieve job and trigger
        // THIS WILL FAIL in Quartz 2.5.1 when MisfireHandler runs
        JobDetail retrievedJob = scheduler.getJobDetail(jobKey);
        Trigger retrievedTrigger = scheduler.getTrigger(triggerKey);

        Assertions.assertNotNull(retrievedJob, "Job should be retrievable after scheduler restart");
        Assertions.assertNotNull(
            retrievedTrigger,
            "Trigger should be retrievable after scheduler restart - FAILS with MisfireHandler error");
        Assertions.assertNotNull(
            retrievedJob.getJobDataMap(), "JobDataMap should persist across scheduler restarts");
    }

    @TestConfiguration
    public static class QuartzJdbcConfiguration {

        @Bean
        ApplicationProperties applicationProperties() {
            return new ApplicationProperties();
        }

        @Bean
        SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("org/quartz/impl/jdbcjobstore/tables_postgres.sql"));
            populator.execute(dataSource);

            SchedulerFactoryBean factory = new SchedulerFactoryBean();

            factory.setDataSource(dataSource);
            factory.setAutoStartup(true);

            Properties properties = new Properties();

            properties.setProperty(
                "org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
            properties.setProperty("org.quartz.jobStore.tablePrefix", "QRTZ_");
            properties.setProperty("org.quartz.jobStore.isClustered", "false");

            // Misfire threshold - triggers MisfireHandler
            properties.setProperty("org.quartz.jobStore.misfireThreshold", "60000");

            factory.setQuartzProperties(properties);

            return factory;
        }

        @Bean
        @Primary
        Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean) throws SchedulerException {
            return schedulerFactoryBean.getScheduler();
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
