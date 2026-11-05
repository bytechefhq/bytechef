/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.scheduler.config.MultiTenantQuartzTriggerSchedulerConfiguration;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.calendar.HolidayCalendar;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.boot.quartz.autoconfigure.QuartzProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs a real Quartz scheduler, configured the way {@link MultiTenantQuartzTriggerSchedulerConfiguration} configures it
 * in multi-tenant mode, against PostgreSQL. The Quartz tables live only in {@code public}, while connections handed out
 * to a tenant caller are on the tenant's schema only, so every Quartz statement that does not go through the wrapped
 * delegate with the search_path switched either fails or, as with the queries the never-initialized
 * {@code StdJDBCDelegate} superclass used to serve, silently matches no rows.
 *
 * @version ee
 */
@Testcontainers
class MultiTenantDriverDelegateIntTest {

    private static final String GROUP_NAME = "ScheduleTrigger";
    private static final String TENANT_ID = "000002";

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    private static DataSource dataSource;
    private static JdbcTemplate jdbcTemplate;

    private final List<SchedulerFactoryBean> schedulerFactoryBeans = new ArrayList<>();

    @BeforeAll
    static void beforeAll() {
        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource(
            postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());

        jdbcTemplate = new JdbcTemplate(driverManagerDataSource);

        ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator(
            new ClassPathResource("org/quartz/impl/jdbcjobstore/tables_postgres.sql"));

        resourceDatabasePopulator.execute(driverManagerDataSource);

        jdbcTemplate.execute("CREATE SCHEMA bytechef_" + TENANT_ID);

        dataSource = new TenantSearchPathDataSource(driverManagerDataSource);
    }

    @AfterEach
    void afterEach() throws Exception {
        TenantContext.resetCurrentTenantId();

        for (SchedulerFactoryBean schedulerFactoryBean : schedulerFactoryBeans) {
            schedulerFactoryBean.destroy();
        }
    }

    @Test
    void testGroupAndCalendarQueriesSeeJobsScheduledByTenantCaller() throws Exception {
        Scheduler scheduler = createScheduler("groupQueries", false);
        JobDetail jobDetail = createJobDetail("job");

        List<String> jobGroupNames = callAsTenant(() -> {
            scheduler.scheduleJob(jobDetail, createTrigger("trigger", jobDetail));
            scheduler.addCalendar("holidays", new HolidayCalendar(), false, false);
            scheduler.pauseTriggers(GroupMatcher.triggerGroupEquals(GROUP_NAME));

            return scheduler.getJobGroupNames();
        });

        assertThat(jobGroupNames).containsExactly(GROUP_NAME);
        assertThat(callAsTenant(scheduler::getTriggerGroupNames)).containsExactly(GROUP_NAME);
        assertThat(callAsTenant(scheduler::getCalendarNames)).containsExactly("holidays");
        assertThat(callAsTenant(scheduler::getPausedTriggerGroups)).containsExactly(GROUP_NAME);
        assertThat(callAsTenant(() -> scheduler.getJobKeys(GroupMatcher.anyJobGroup())))
            .containsExactly(jobDetail.getKey());
    }

    @Test
    void testPauseAllAndResumeAllReachEveryTriggerGroup() throws Exception {
        Scheduler scheduler = createScheduler("pauseAll", false);
        JobDetail jobDetail = createJobDetail("job");
        TriggerKey triggerKey = TriggerKey.triggerKey("trigger", GROUP_NAME);

        Trigger.TriggerState pausedState = callAsTenant(() -> {
            scheduler.scheduleJob(jobDetail, createTrigger("trigger", jobDetail));
            scheduler.pauseAll();

            return scheduler.getTriggerState(triggerKey);
        });

        assertThat(pausedState).isEqualTo(Trigger.TriggerState.PAUSED);

        Trigger.TriggerState resumedState = callAsTenant(() -> {
            scheduler.resumeAll();

            return scheduler.getTriggerState(triggerKey);
        });

        assertThat(resumedState).isEqualTo(Trigger.TriggerState.NORMAL);
        assertThat(callAsTenant(scheduler::getPausedTriggerGroups)).isEmpty();
    }

    @Test
    void testDeleteJobRemovesItsTriggersBeforeTheJobDetail() throws Exception {
        Scheduler scheduler = createScheduler("deleteJob", false);
        JobDetail jobDetail = createJobDetail("job");

        boolean deleted = callAsTenant(() -> {
            scheduler.scheduleJob(jobDetail, createTrigger("trigger1", jobDetail));
            scheduler.scheduleJob(createTrigger("trigger2", jobDetail));

            return scheduler.deleteJobs(List.of(jobDetail.getKey()));
        });

        assertThat(deleted).isTrue();
        assertThat(callAsTenant(() -> scheduler.checkExists(jobDetail.getKey()))).isFalse();
        assertThat(countTriggers("deleteJob")).isZero();
    }

    @Test
    void testStartingNonClusteredSchedulerClearsStaleFiredTriggers() throws Exception {
        Scheduler scheduler = createScheduler("staleFiredTriggers", false);

        insertFiredTrigger("staleFiredTriggers", "NON_CLUSTERED");

        assertThat(countFiredTriggers("staleFiredTriggers")).isOne();

        scheduler.start();

        assertThat(countFiredTriggers("staleFiredTriggers")).isZero();
    }

    @Test
    void testClusteredSchedulerRecoversFiredTriggersOfOrphanedInstance() throws Exception {
        Scheduler scheduler = createScheduler("orphanedInstance", true);

        insertFiredTrigger("orphanedInstance", "crashed-instance");

        scheduler.start();

        await()
            .atMost(Duration.ofSeconds(20))
            .untilAsserted(() -> assertThat(countFiredTriggers("orphanedInstance")).isZero());
    }

    private Scheduler createScheduler(String schedulerName, boolean clustered) throws Exception {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Tenant tenant = applicationProperties.getTenant();

        tenant.setMode(ApplicationProperties.Tenant.Mode.MULTI);

        QuartzProperties quartzProperties = new QuartzProperties();

        Map<String, String> properties = quartzProperties.getProperties();

        properties.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        properties.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
        properties.put("org.quartz.jobStore.isClustered", String.valueOf(clustered));
        properties.put("org.quartz.jobStore.clusterCheckinInterval", "1000");
        properties.put("org.quartz.scheduler.instanceId", clustered ? "AUTO" : "NON_CLUSTERED");
        properties.put("org.quartz.threadPool.threadCount", "1");

        MultiTenantQuartzTriggerSchedulerConfiguration multiTenantQuartzTriggerSchedulerConfiguration =
            new MultiTenantQuartzTriggerSchedulerConfiguration(
                applicationProperties, new AdaptableJobFactory(), quartzProperties);

        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();

        schedulerFactoryBean.setAutoStartup(false);
        schedulerFactoryBean.setDataSource(dataSource);
        schedulerFactoryBean.setSchedulerName(schedulerName);

        multiTenantQuartzTriggerSchedulerConfiguration.customize(schedulerFactoryBean);

        schedulerFactoryBean.afterPropertiesSet();

        schedulerFactoryBeans.add(schedulerFactoryBean);

        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        assertThat(scheduler.getMetaData()
            .getJobStoreClass()).isEqualTo(MultiTenantLocalDataSourceJobStore.class);

        return scheduler;
    }

    private static <T> T callAsTenant(Callable<T> callable) throws Exception {
        TenantContext.setCurrentTenantId(TENANT_ID);

        try {
            return callable.call();
        } finally {
            TenantContext.resetCurrentTenantId();
        }
    }

    private static JobDetail createJobDetail(String jobName) {
        return JobBuilder.newJob(NoOpJob.class)
            .withIdentity(jobName, GROUP_NAME)
            .storeDurably()
            .build();
    }

    private static Trigger createTrigger(String triggerName, JobDetail jobDetail) {
        Instant now = Instant.now();

        Date startTime = Date.from(now.plus(Duration.ofDays(1)));

        return TriggerBuilder.newTrigger()
            .withIdentity(triggerName, GROUP_NAME)
            .forJob(jobDetail)
            .startAt(startTime)
            .withSchedule(SimpleScheduleBuilder.repeatHourlyForever())
            .build();
    }

    private static int countFiredTriggers(String schedulerName) {
        return count("SELECT COUNT(*) FROM public.qrtz_fired_triggers WHERE sched_name = ?", schedulerName);
    }

    private static int countTriggers(String schedulerName) {
        return count("SELECT COUNT(*) FROM public.qrtz_triggers WHERE sched_name = ?", schedulerName);
    }

    private static int count(String countQuery, String schedulerName) {
        Integer count = jdbcTemplate.queryForObject(countQuery, Integer.class, schedulerName);

        return count == null ? 0 : count;
    }

    private static void insertFiredTrigger(String schedulerName, String instanceName) {
        long now = System.currentTimeMillis();

        jdbcTemplate.update(
            "INSERT INTO public.qrtz_fired_triggers (sched_name, entry_id, trigger_name, trigger_group, " +
                "instance_name, fired_time, sched_time, priority, state, job_name, job_group, is_nonconcurrent, " +
                "requests_recovery) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            schedulerName, instanceName + "-entry", "trigger", GROUP_NAME, instanceName, now, now, 5, "EXECUTING",
            "job", GROUP_NAME, false, false);
    }

    /**
     * A no-op job; the triggers in these tests start a day from now, so it never runs.
     */
    public static class NoOpJob implements Job {

        @Override
        public void execute(JobExecutionContext context) {
            JobKey jobKey = context.getJobDetail()
                .getKey();

            throw new IllegalStateException("Job " + jobKey + " is not expected to run");
        }
    }

    /**
     * Hands out connections whose search_path is the current tenant's schema only, without the {@code public} fallback,
     * so that only statements run with the search_path switched to {@code public} can reach the Quartz tables.
     */
    private static final class TenantSearchPathDataSource extends DelegatingDataSource {

        private TenantSearchPathDataSource(DataSource targetDataSource) {
            super(targetDataSource);
        }

        @Override
        @SuppressFBWarnings("SQL_INJECTION_JDBC")
        public Connection getConnection() throws SQLException {
            Connection connection = super.getConnection();

            try (Statement statement = connection.createStatement()) {
                statement.execute("SET search_path TO " + TenantContext.getCurrentDatabaseSchema());
            }

            return connection;
        }
    }
}
