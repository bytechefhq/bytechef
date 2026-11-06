/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.scheduler.config.MultiTenantQuartzTriggerSchedulerConfiguration;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.sql.BaseDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.sql.DataSource;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.boot.quartz.autoconfigure.QuartzProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * @version ee
 */
final class MultiTenantQuartzTestSupport {

    static final String GROUP_NAME = "ScheduleTrigger";
    static final String TENANT_ID = "000002";
    static final String TENANT_SCHEMA = "bytechef_" + TENANT_ID;
    static final String TENANT_FALLBACK_SEARCH_PATH = TENANT_SCHEMA + ", public";

    private MultiTenantQuartzTestSupport() {
    }

    static void initializeDatabase(DataSource dataSource) {
        ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator(
            new ClassPathResource("org/quartz/impl/jdbcjobstore/tables_postgres.sql"));

        resourceDatabasePopulator.execute(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("CREATE SCHEMA " + TENANT_SCHEMA);
        jdbcTemplate.execute("CREATE TYPE public.public_fallback_probe AS ENUM ('resolved')");
    }

    static Scheduler createScheduler(
        String schedulerName, boolean clustered, DataSource schedulerDataSource,
        List<SchedulerFactoryBean> schedulerFactoryBeans) throws Exception {

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
        schedulerFactoryBean.setDataSource(schedulerDataSource);
        schedulerFactoryBean.setSchedulerName(schedulerName);

        multiTenantQuartzTriggerSchedulerConfiguration.customize(schedulerFactoryBean);

        schedulerFactoryBean.afterPropertiesSet();

        schedulerFactoryBeans.add(schedulerFactoryBean);

        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        assertThat(scheduler.getMetaData()
            .getJobStoreClass()).isEqualTo(MultiTenantLocalDataSourceJobStore.class);

        return scheduler;
    }

    static <T> T callAsTenant(Callable<T> callable) throws Exception {
        TenantContext.setCurrentTenantId(TENANT_ID);

        try {
            return callable.call();
        } finally {
            TenantContext.resetCurrentTenantId();
        }
    }

    static Connection openConnection(DataSource dataSource, String searchPath) throws SQLException {
        Connection connection = dataSource.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(
            "SELECT set_config('search_path', ?, false)")) {

            preparedStatement.setString(1, searchPath);

            preparedStatement.execute();
        }

        return connection;
    }

    static String showSearchPath(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SHOW search_path")) {

            if (!resultSet.next()) {
                throw new IllegalStateException("SHOW search_path returned no row");
            }

            return resultSet.getString(1);
        }
    }

    static String showSearchPath(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject("SHOW search_path", String.class);
    }

    static boolean isPublicFallbackProbeResolvable(JdbcTemplate jdbcTemplate) {
        return Boolean.TRUE.equals(
            jdbcTemplate.queryForObject("SELECT to_regtype('public_fallback_probe') IS NOT NULL", Boolean.class));
    }

    static JobDetail createJobDetail(String jobName) {
        return JobBuilder.newJob(NoOpJob.class)
            .withIdentity(jobName, GROUP_NAME)
            .storeDurably()
            .build();
    }

    static Trigger createTrigger(String triggerName, JobDetail jobDetail) {
        Instant now = Instant.now();

        Date startTime = Date.from(now.plus(Duration.ofDays(1)));

        return TriggerBuilder.newTrigger()
            .withIdentity(triggerName, GROUP_NAME)
            .forJob(jobDetail)
            .startAt(startTime)
            .withSchedule(SimpleScheduleBuilder.repeatHourlyForever())
            .build();
    }

    public static class NoOpJob implements Job {

        @Override
        public void execute(JobExecutionContext context) {
            JobKey jobKey = context.getJobDetail()
                .getKey();

            throw new IllegalStateException("Job " + jobKey + " is not expected to run");
        }
    }

    static final class TenantFallbackDataSource extends BaseDataSource {

        TenantFallbackDataSource(DataSource dataSource) {
            super(dataSource);
        }
    }

    static final class TenantSearchPathDataSource extends DelegatingDataSource {

        TenantSearchPathDataSource(DataSource targetDataSource) {
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
