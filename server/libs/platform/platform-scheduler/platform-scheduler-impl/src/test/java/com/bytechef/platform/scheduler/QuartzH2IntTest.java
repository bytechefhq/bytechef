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
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.scheduler.config.QuartzJdbcTestConfiguration;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.config.h2.H2DataSourceConfiguration;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
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

/**
 * Verifies that Quartz works on H2, the database behind the {@code h2} profile.
 *
 * <p>
 * Two things are proven that the PostgreSQL twin cannot. The shipped Quartz DDL is named
 * {@code quartz_postgres_init.sql} and is not gated by DBMS, so H2 executes its {@code BOOL} and {@code BYTEA} columns
 * verbatim - this test runs that exact file. And the {@code h2} profile swaps the delegate to
 * {@link org.quartz.impl.jdbcjobstore.StdJDBCDelegate}, whose {@code containsColumnNames()} compares column names
 * case-sensitively against uppercase constants; H2 returns identifiers uppercase, so the case bug that
 * {@link QuartzIntTest} documents for PostgreSQL must not reappear here. Both only surface once a JobDataMap makes a
 * round trip through the database, which is what these tests exercise.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = QuartzJdbcTestConfiguration.class)
@Import({
    H2DataSourceConfiguration.class, QuartzH2IntTest.QuartzH2Configuration.class
})
public class QuartzH2IntTest {

    @Autowired
    private DataSource dataSource;

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
    public void testShippedPostgresDdlCreatesQuartzTablesOnH2() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            Assertions.assertTrue(
                tableExists(databaseMetaData, "QRTZ_JOB_DETAILS"),
                "The shipped quartz_postgres_init.sql must create its tables on H2");
            Assertions.assertTrue(
                tableExists(databaseMetaData, "QRTZ_TRIGGERS"), "QRTZ_TRIGGERS must exist on H2");
            Assertions.assertTrue(
                tableExists(databaseMetaData, "QRTZ_CRON_TRIGGERS"), "QRTZ_CRON_TRIGGERS must exist on H2");
        }
    }

    @Test
    public void testPollingTriggerJobDataMapRoundTrips() throws Exception {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 1059L, "50849def-93e2-44eb-aa05-797ebf849954", "trigger_1");

        quartzTriggerScheduler.schedulePollingTrigger(workflowExecutionId);

        JobKey jobKey = JobKey.jobKey(workflowExecutionId.toString(), "PollingTrigger");

        JobDetail retrievedJob = scheduler.getJobDetail(jobKey);

        Assertions.assertNotNull(retrievedJob, "Job detail should be retrieved from H2");

        JobDataMap jobDataMap = retrievedJob.getJobDataMap();

        Assertions.assertNotNull(jobDataMap, "JobDataMap should survive the BYTEA column mapped to H2 BINARY VARYING");
        Assertions.assertEquals(
            workflowExecutionId.toString(), jobDataMap.getString("workflowExecutionId"),
            "WorkflowExecutionId should be loaded from JobDataMap");
    }

    @Test
    public void testScheduleTriggerJobDataMapAndCronPropertiesRoundTrip() throws Exception {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 789L, "test-schedule-workflow", "testTrigger");
        String cronPattern = "0/5 * * * * ?";
        Map<String, Object> output = Map.of("key", "value", "number", 42);

        quartzTriggerScheduler.scheduleScheduleTrigger(cronPattern, "UTC", output, workflowExecutionId);

        JobKey jobKey = JobKey.jobKey(workflowExecutionId.toString(), "ScheduleTrigger");
        TriggerKey triggerKey = TriggerKey.triggerKey(workflowExecutionId.toString(), "ScheduleTrigger");

        JobDetail retrievedJob = scheduler.getJobDetail(jobKey);

        Assertions.assertNotNull(retrievedJob, "Job detail should be retrieved from H2");

        JobDataMap jobDataMap = retrievedJob.getJobDataMap();

        Assertions.assertNotNull(jobDataMap, "JobDataMap should not be null on H2");
        Assertions.assertNotNull(jobDataMap.getString("output"), "Output data should be loaded from JobDataMap");

        Trigger retrievedTrigger = scheduler.getTrigger(triggerKey);

        Assertions.assertNotNull(
            retrievedTrigger, "Trigger extended properties should be readable on H2 via StdJDBCDelegate");
        Assertions.assertInstanceOf(CronTrigger.class, retrievedTrigger, "Trigger should be a CronTrigger");
    }

    private static boolean tableExists(DatabaseMetaData databaseMetaData, String tableName) throws Exception {
        try (ResultSet resultSet = databaseMetaData.getTables(null, null, tableName, null)) {
            return resultSet.next();
        }
    }

    @TestConfiguration
    public static class QuartzH2Configuration {

        @Bean
        ApplicationProperties applicationProperties() {
            return new ApplicationProperties();
        }

        @Bean
        SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) {
            ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();

            resourceDatabasePopulator.addScript(
                new ClassPathResource("config/liquibase/changelog/quartz/quartz_postgres_init.sql"));
            resourceDatabasePopulator.execute(dataSource);

            SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();

            schedulerFactoryBean.setDataSource(dataSource);
            schedulerFactoryBean.setAutoStartup(true);

            Properties properties = new Properties();

            properties.setProperty(
                "org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
            properties.setProperty("org.quartz.jobStore.tablePrefix", "QRTZ_");
            properties.setProperty("org.quartz.jobStore.isClustered", "false");
            properties.setProperty("org.quartz.jobStore.misfireThreshold", "60000");

            schedulerFactoryBean.setQuartzProperties(properties);

            return schedulerFactoryBean;
        }

        @Bean
        @Primary
        Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean) {
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
