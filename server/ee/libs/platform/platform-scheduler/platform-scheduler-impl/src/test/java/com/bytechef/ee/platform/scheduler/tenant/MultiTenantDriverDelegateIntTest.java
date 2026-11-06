/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.GROUP_NAME;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.TENANT_FALLBACK_SEARCH_PATH;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.TENANT_ID;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.TENANT_SCHEMA;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.callAsTenant;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.createJobDetail;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.createTrigger;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.isPublicFallbackProbeResolvable;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.openConnection;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.showSearchPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.TenantFallbackDataSource;
import com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.TenantSearchPathDataSource;
import com.bytechef.tenant.TenantContext;
import java.io.NotSerializableException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.calendar.HolidayCalendar;
import org.quartz.impl.jdbcjobstore.DriverDelegate;
import org.quartz.impl.jdbcjobstore.NoSuchDelegateException;
import org.quartz.impl.jdbcjobstore.PostgreSQLDelegate;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @version ee
 */
@Testcontainers
class MultiTenantDriverDelegateIntTest {

    private static final String ISOLATED_GROUP_NAME = "IsolatedGroup";
    private static final String MISSING_TABLE_PREFIX = "MISSING_";
    private static final String TABLE_PREFIX = "QRTZ_";

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    private static DataSource dataSource;
    private static DataSource databaseDataSource;
    private static JdbcTemplate jdbcTemplate;
    private static DataSource tenantFallbackDataSource;

    private final List<Connection> connections = new ArrayList<>();
    private final List<SchedulerFactoryBean> schedulerFactoryBeans = new ArrayList<>();

    @BeforeAll
    static void beforeAll() {
        databaseDataSource = new DriverManagerDataSource(
            postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());

        jdbcTemplate = new JdbcTemplate(databaseDataSource);

        MultiTenantQuartzTestSupport.initializeDatabase(databaseDataSource);

        dataSource = new TenantSearchPathDataSource(databaseDataSource);
        tenantFallbackDataSource = new TenantFallbackDataSource(databaseDataSource);
    }

    @AfterEach
    void afterEach() throws Exception {
        TenantContext.resetCurrentTenantId();

        for (Connection connection : connections) {
            connection.close();
        }

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

    @Test
    void testQuartzCallsInTenantTransactionKeepSearchPathOfTransactionConnection() throws Exception {
        Scheduler scheduler = createScheduler("tenantTransaction", false, tenantFallbackDataSource);
        JobDetail jobDetail = createJobDetail("job");
        JdbcTemplate tenantJdbcTemplate = new JdbcTemplate(tenantFallbackDataSource);
        TransactionTemplate transactionTemplate = new TransactionTemplate(
            new DataSourceTransactionManager(tenantFallbackDataSource));

        List<Object> observations = callAsTenant(() -> transactionTemplate.execute(transactionStatus -> {
            List<Object> currentObservations = new ArrayList<>();

            currentObservations.add(showSearchPath(tenantJdbcTemplate));

            callScheduler(() -> scheduler.scheduleJob(jobDetail, createTrigger("trigger", jobDetail)));

            currentObservations.add(showSearchPath(tenantJdbcTemplate));

            callScheduler(() -> scheduler.checkExists(jobDetail.getKey()));

            currentObservations.add(showSearchPath(tenantJdbcTemplate));

            callScheduler(() -> scheduler.deleteJob(jobDetail.getKey()));

            currentObservations.add(showSearchPath(tenantJdbcTemplate));
            currentObservations.add(isPublicFallbackProbeResolvable(tenantJdbcTemplate));

            return currentObservations;
        }));

        assertThat(observations)
            .as("search_path before and after each Quartz call in the tenant transaction, then whether an object in " +
                "public still resolves")
            .containsExactly(
                TENANT_FALLBACK_SEARCH_PATH, TENANT_FALLBACK_SEARCH_PATH, TENANT_FALLBACK_SEARCH_PATH,
                TENANT_FALLBACK_SEARCH_PATH, true);
        assertThat(countTriggers("tenantTransaction")).isZero();
    }

    @Test
    void testQuartzCallUnderOtherTenantContextKeepsSearchPathOfTransactionConnection() throws Exception {
        Scheduler scheduler = createScheduler("otherTenantContext", false, tenantFallbackDataSource);
        JdbcTemplate tenantJdbcTemplate = new JdbcTemplate(tenantFallbackDataSource);
        TransactionTemplate transactionTemplate = new TransactionTemplate(
            new DataSourceTransactionManager(tenantFallbackDataSource));

        List<String> searchPaths = callAsTenant(() -> transactionTemplate.execute(transactionStatus -> {
            String originalSearchPath = showSearchPath(tenantJdbcTemplate);

            TenantContext.setCurrentTenantId("000003");

            try {
                callScheduler(() -> scheduler.checkExists(JobKey.jobKey("job", GROUP_NAME)));
            } finally {
                TenantContext.setCurrentTenantId(TENANT_ID);
            }

            return List.of(originalSearchPath, showSearchPath(tenantJdbcTemplate));
        }));

        assertThat(searchPaths)
            .as("The connection was obtained, and its search_path set, before the tenant context changed")
            .containsExactly(TENANT_FALLBACK_SEARCH_PATH, TENANT_FALLBACK_SEARCH_PATH);
    }

    @Test
    @SuppressWarnings("deprecation")
    void testCallsPreviouslyServedByUninitializedStdJdbcDelegateSeeRowsOfConfiguredScheduler() throws Exception {
        MultiTenantDriverDelegate multiTenantDriverDelegate = new MultiTenantDriverDelegate(
            createPostgreSQLDelegate(TABLE_PREFIX, "previouslyUninitialized"));
        Connection connection = openTrackedConnection(TENANT_SCHEMA);
        JobDetail jobDetail = JobBuilder.newJob(MultiTenantQuartzTestSupport.NoOpJob.class)
            .withIdentity("job", ISOLATED_GROUP_NAME)
            .storeDurably()
            .build();
        OperableTrigger operableTrigger = (OperableTrigger) TriggerBuilder.newTrigger()
            .withIdentity("trigger", ISOLATED_GROUP_NAME)
            .forJob(jobDetail)
            .startAt(Date.from(Instant.now()
                .plus(Duration.ofDays(1))))
            .build();

        operableTrigger.computeFirstFireTime(null);
        operableTrigger.setFireInstanceId("fired-entry");

        TenantContext.setCurrentTenantId(TENANT_ID);

        multiTenantDriverDelegate.insertJobDetail(connection, jobDetail);
        multiTenantDriverDelegate.insertTrigger(connection, operableTrigger, "WAITING", jobDetail);
        multiTenantDriverDelegate.insertCalendar(connection, "holidays", new HolidayCalendar());
        multiTenantDriverDelegate.insertPausedTriggerGroup(connection, ISOLATED_GROUP_NAME);
        multiTenantDriverDelegate.insertFiredTrigger(connection, operableTrigger, "ACQUIRED", jobDetail);

        assertThat(multiTenantDriverDelegate.selectNumJobs(connection)).isOne();
        assertThat(multiTenantDriverDelegate.selectJobGroups(connection)).containsExactly(ISOLATED_GROUP_NAME);
        assertThat(multiTenantDriverDelegate.selectNumTriggers(connection)).isOne();
        assertThat(multiTenantDriverDelegate.selectTriggerGroups(connection)).containsExactly(ISOLATED_GROUP_NAME);
        assertThat(multiTenantDriverDelegate.selectTriggerKeysForJob(connection, jobDetail.getKey()))
            .containsExactly(operableTrigger.getKey());
        assertThat(multiTenantDriverDelegate.selectNumCalendars(connection)).isOne();
        assertThat(multiTenantDriverDelegate.selectCalendars(connection)).containsExactly("holidays");
        assertThat(multiTenantDriverDelegate.selectNextFireTime(connection))
            .isEqualTo(operableTrigger.getNextFireTime()
                .getTime());
        assertThat(multiTenantDriverDelegate.selectFiredTriggerInstanceNames(connection))
            .containsExactly("NON_CLUSTERED");
        assertThat(
            multiTenantDriverDelegate.selectJobDetails(
                connection, GroupMatcher.jobGroupEquals(ISOLATED_GROUP_NAME), createClassLoadHelper()))
                    .extracting(JobDetail::getKey)
                    .containsExactly(jobDetail.getKey());
        assertThat(
            multiTenantDriverDelegate.getTriggersByJobAndTriggerGroup(
                connection, GroupMatcher.jobGroupEquals(ISOLATED_GROUP_NAME),
                GroupMatcher.triggerGroupEquals(ISOLATED_GROUP_NAME)))
                    .extracting(OperableTrigger::getKey)
                    .containsExactly(operableTrigger.getKey());
        assertThat(multiTenantDriverDelegate.selectPausedTriggerGroups(connection))
            .containsExactly(ISOLATED_GROUP_NAME);
        assertThat(multiTenantDriverDelegate.deleteAllPausedTriggerGroups(connection)).isOne();
        assertThat(multiTenantDriverDelegate.deleteFiredTriggers(connection)).isOne();
        assertThat(showSearchPath(connection)).isEqualTo(TENANT_SCHEMA);
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "bytechef_000002, public | 000002",
            "bytechef_000002, public | 000005",
            "public | 000005",
            "bytechef_vectorstore_000002, \"$user\", public | 000002"
        })
    void testSearchPathOfConnectionIsRestoredUnchanged(String searchPath, String tenantId) throws Exception {
        MultiTenantDriverDelegate multiTenantDriverDelegate = new MultiTenantDriverDelegate(
            createPostgreSQLDelegate(TABLE_PREFIX, "restoreSearchPath"));
        Connection connection = openTrackedConnection(searchPath);

        TenantContext.setCurrentTenantId(tenantId);

        assertThat(multiTenantDriverDelegate.jobExists(connection, JobKey.jobKey("job", GROUP_NAME))).isFalse();

        assertThat(showSearchPath(connection)).isEqualTo(searchPath);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(tenantId);
    }

    @Test
    void testSqlExceptionOfDelegateIsPropagatedAndSearchPathRestored() throws Exception {
        MultiTenantDriverDelegate multiTenantDriverDelegate = new MultiTenantDriverDelegate(
            createPostgreSQLDelegate(MISSING_TABLE_PREFIX, "sqlException"));
        Connection connection = openTrackedConnection(TENANT_FALLBACK_SEARCH_PATH);

        TenantContext.setCurrentTenantId(TENANT_ID);

        assertThatThrownBy(
            () -> multiTenantDriverDelegate.deleteTrigger(connection, TriggerKey.triggerKey("trigger", GROUP_NAME)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("missing_");

        assertThat(showSearchPath(connection)).isEqualTo(TENANT_FALLBACK_SEARCH_PATH);
    }

    @Test
    void testIoExceptionOfDelegateIsPropagatedAndSearchPathRestored() throws Exception {
        MultiTenantDriverDelegate multiTenantDriverDelegate = new MultiTenantDriverDelegate(
            createPostgreSQLDelegate(TABLE_PREFIX, "ioException"));
        Connection connection = openTrackedConnection(TENANT_FALLBACK_SEARCH_PATH);
        JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put("notSerializable", new Object());

        JobDetail jobDetail = JobBuilder.newJob(MultiTenantQuartzTestSupport.NoOpJob.class)
            .withIdentity("job", GROUP_NAME)
            .usingJobData(jobDataMap)
            .storeDurably()
            .build();

        TenantContext.setCurrentTenantId(TENANT_ID);

        assertThatThrownBy(() -> multiTenantDriverDelegate.insertJobDetail(connection, jobDetail))
            .isInstanceOf(NotSerializableException.class)
            .hasMessageContaining("'notSerializable'");

        assertThat(showSearchPath(connection)).isEqualTo(TENANT_FALLBACK_SEARCH_PATH);
    }

    @Test
    void testClassNotFoundExceptionOfDelegateIsPropagatedAndSearchPathRestored() throws Exception {
        MultiTenantDriverDelegate multiTenantDriverDelegate = new MultiTenantDriverDelegate(
            createPostgreSQLDelegate(TABLE_PREFIX, "classNotFound"));
        Connection connection = openTrackedConnection(TENANT_FALLBACK_SEARCH_PATH);
        JobDetail jobDetail = createJobDetail("job");

        TenantContext.setCurrentTenantId(TENANT_ID);

        multiTenantDriverDelegate.insertJobDetail(connection, jobDetail);

        jdbcTemplate.update(
            "UPDATE public.qrtz_job_details SET job_class_name = 'com.example.MissingJob' WHERE sched_name = ?",
            "classNotFound");

        assertThatThrownBy(
            () -> multiTenantDriverDelegate.selectJobDetail(
                connection, jobDetail.getKey(), createClassLoadHelper()))
                    .isInstanceOf(ClassNotFoundException.class)
                    .hasMessageContaining("com.example.MissingJob");

        assertThat(showSearchPath(connection)).isEqualTo(TENANT_FALLBACK_SEARCH_PATH);
    }

    @Test
    void testJobPersistenceExceptionOfDelegateIsPropagatedAndSearchPathRestored() throws Exception {
        MultiTenantDriverDelegate multiTenantDriverDelegate = new MultiTenantDriverDelegate(
            createPostgreSQLDelegate(TABLE_PREFIX, "jobPersistence"));
        Connection connection = openTrackedConnection(TENANT_FALLBACK_SEARCH_PATH);
        JobDetail jobDetail = createJobDetail("job");
        OperableTrigger operableTrigger = (OperableTrigger) createTrigger("trigger", jobDetail);

        TenantContext.setCurrentTenantId(TENANT_ID);

        multiTenantDriverDelegate.insertJobDetail(connection, jobDetail);
        multiTenantDriverDelegate.insertTrigger(connection, operableTrigger, "WAITING", jobDetail);

        jdbcTemplate.update(
            "UPDATE public.qrtz_triggers SET trigger_type = 'UNKNOWN' WHERE sched_name = ?", "jobPersistence");

        assertThatThrownBy(() -> multiTenantDriverDelegate.selectTrigger(connection, operableTrigger.getKey()))
            .isInstanceOf(JobPersistenceException.class)
            .hasMessageContaining("UNKNOWN");

        assertThat(showSearchPath(connection)).isEqualTo(TENANT_FALLBACK_SEARCH_PATH);
    }

    @Test
    void testFailureToRestoreSearchPathIsSuppressedOnDelegateException() throws Exception {
        MultiTenantDriverDelegate multiTenantDriverDelegate = new MultiTenantDriverDelegate(
            createPostgreSQLDelegate(MISSING_TABLE_PREFIX, "restoreFailure"));
        Connection connection = openTrackedConnection(TENANT_FALLBACK_SEARCH_PATH);

        connection.setAutoCommit(false);

        TenantContext.setCurrentTenantId(TENANT_ID);

        assertThatThrownBy(
            () -> multiTenantDriverDelegate.deleteTrigger(connection, TriggerKey.triggerKey("trigger", GROUP_NAME)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("missing_")
                .satisfies(throwable -> assertThat(throwable.getSuppressed())
                    .singleElement()
                    .satisfies(suppressed -> assertThat(suppressed)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("current transaction is aborted")));

        connection.rollback();
    }

    @Test
    void testFailureToSwitchSearchPathIsPropagatedWithoutCallingDelegate() throws Exception {
        MultiTenantDriverDelegate multiTenantDriverDelegate = new MultiTenantDriverDelegate(
            createPostgreSQLDelegate(TABLE_PREFIX, "switchFailure"));
        Connection connection = openTrackedConnection(TENANT_FALLBACK_SEARCH_PATH);

        connection.setAutoCommit(false);

        abortTransaction(connection);

        TenantContext.setCurrentTenantId(TENANT_ID);

        assertThatThrownBy(
            () -> multiTenantDriverDelegate.jobExists(connection, JobKey.jobKey("job", GROUP_NAME)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("current transaction is aborted")
                .satisfies(throwable -> assertThat(throwable.getSuppressed()).isEmpty());

        connection.rollback();

        assertThat(showSearchPath(connection)).isEqualTo(TENANT_FALLBACK_SEARCH_PATH);
    }

    @Test
    void testInitializeIsForwardedToWrappedDelegate() throws Exception {
        MultiTenantDriverDelegate multiTenantDriverDelegate = new MultiTenantDriverDelegate(new PostgreSQLDelegate());
        Connection connection = openTrackedConnection(TENANT_SCHEMA);

        multiTenantDriverDelegate.initialize(
            LoggerFactory.getLogger(MultiTenantDriverDelegateIntTest.class), TABLE_PREFIX, "initializeForwarded",
            "NON_CLUSTERED", createClassLoadHelper(), false, null);

        TenantContext.setCurrentTenantId(TENANT_ID);

        JobDetail jobDetail = createJobDetail("job");

        multiTenantDriverDelegate.insertJobDetail(connection, jobDetail);

        assertThat(multiTenantDriverDelegate.jobExists(connection, jobDetail.getKey())).isTrue();
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.qrtz_job_details WHERE sched_name = ?", Integer.class,
                "initializeForwarded")).isOne();
    }

    @Test
    void testEnhancedStatementsSettingIsForwardedToWrappedDelegate() throws Exception {
        PostgreSQLDelegate postgreSQLDelegate = createPostgreSQLDelegate(TABLE_PREFIX, "enhancedStatements");

        MultiTenantDriverDelegate multiTenantDriverDelegate = new MultiTenantDriverDelegate(postgreSQLDelegate);

        multiTenantDriverDelegate.setUseEnhancedStatements(true);

        assertThat(postgreSQLDelegate.isUsingEnhancedStatements()).isTrue();
        assertThat(multiTenantDriverDelegate.isUsingEnhancedStatements()).isTrue();

        multiTenantDriverDelegate.setUseEnhancedStatements(false);

        assertThat(postgreSQLDelegate.isUsingEnhancedStatements()).isFalse();
        assertThat(multiTenantDriverDelegate.isUsingEnhancedStatements()).isFalse();
    }

    @Test
    void testEveryDriverDelegateMethodIsDeclaredByMultiTenantDriverDelegate() {
        List<String> inheritedMethods = new ArrayList<>();

        for (Method interfaceMethod : driverDelegateMethods()) {
            Method implementationMethod = findImplementationMethod(interfaceMethod);

            Class<?> declaringClass = implementationMethod.getDeclaringClass();

            if (declaringClass != MultiTenantDriverDelegate.class) {
                inheritedMethods.add(describe(interfaceMethod) + " served by " + declaringClass.getName());
            }
        }

        assertThat(inheritedMethods)
            .as(
                "Every DriverDelegate method must be overridden by MultiTenantDriverDelegate and forwarded to the " +
                    "wrapped delegate, otherwise it bypasses the delegate and the search_path switch")
            .isEmpty();
    }

    private Scheduler createScheduler(String schedulerName, boolean clustered) throws Exception {
        return createScheduler(schedulerName, clustered, dataSource);
    }

    private Scheduler createScheduler(String schedulerName, boolean clustered, DataSource schedulerDataSource)
        throws Exception {

        return MultiTenantQuartzTestSupport.createScheduler(
            schedulerName, clustered, schedulerDataSource, schedulerFactoryBeans);
    }

    private Connection openTrackedConnection(String searchPath) throws SQLException {
        Connection connection = openConnection(databaseDataSource, searchPath);

        connections.add(connection);

        return connection;
    }

    private static void abortTransaction(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1 / 0");
        } catch (SQLException sqlException) {
            assertThat(sqlException.getMessage()).contains("division by zero");
        }
    }

    private static PostgreSQLDelegate createPostgreSQLDelegate(String tablePrefix, String schedulerName)
        throws NoSuchDelegateException {

        PostgreSQLDelegate postgreSQLDelegate = new PostgreSQLDelegate();

        postgreSQLDelegate.initialize(
            LoggerFactory.getLogger(MultiTenantDriverDelegateIntTest.class), tablePrefix, schedulerName,
            "NON_CLUSTERED", createClassLoadHelper(), false, null);

        return postgreSQLDelegate;
    }

    private static CascadingClassLoadHelper createClassLoadHelper() {
        CascadingClassLoadHelper cascadingClassLoadHelper = new CascadingClassLoadHelper();

        cascadingClassLoadHelper.initialize();

        return cascadingClassLoadHelper;
    }

    private static void callScheduler(SchedulerCall schedulerCall) {
        try {
            schedulerCall.call();
        } catch (SchedulerException schedulerException) {
            throw new IllegalStateException(schedulerException);
        }
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

    private static List<Method> driverDelegateMethods() {
        return Arrays.stream(DriverDelegate.class.getMethods())
            .filter(method -> !Modifier.isStatic(method.getModifiers()) && !method.isSynthetic())
            .sorted(Comparator.comparing(MultiTenantDriverDelegateIntTest::describe))
            .toList();
    }

    private static Method findImplementationMethod(Method interfaceMethod) {
        try {
            return MultiTenantDriverDelegate.class.getMethod(
                interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new IllegalStateException(noSuchMethodException);
        }
    }

    private static String describe(Method method) {
        String parameterTypeNames = Arrays.stream(method.getParameterTypes())
            .map(Class::getSimpleName)
            .collect(Collectors.joining(", "));

        return method.getName() + "(" + parameterTypeNames + ")";
    }

    @FunctionalInterface
    private interface SchedulerCall {

        void call() throws SchedulerException;
    }
}
