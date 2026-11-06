/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.TENANT_FALLBACK_SEARCH_PATH;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.callAsTenant;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.createJobDetail;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.createTrigger;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.isPublicFallbackProbeResolvable;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.showSearchPath;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.TenantFallbackDataSource;
import com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.TenantSearchPathDataSource;
import com.bytechef.tenant.TenantContext;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.impl.StdScheduler;
import org.quartz.impl.jdbcjobstore.DriverDelegate;
import org.quartz.impl.jdbcjobstore.JobStoreSupport;
import org.quartz.impl.jdbcjobstore.Semaphore;
import org.quartz.impl.jdbcjobstore.StdRowLockSemaphore;
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
class MultiTenantLocalDataSourceJobStoreIntTest {

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    private static JdbcTemplate jdbcTemplate;
    private static DataSource tenantFallbackDataSource;
    private static DataSource tenantSearchPathDataSource;

    private final List<SchedulerFactoryBean> schedulerFactoryBeans = new ArrayList<>();

    @BeforeAll
    static void beforeAll() {
        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource(
            postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());

        jdbcTemplate = new JdbcTemplate(driverManagerDataSource);

        MultiTenantQuartzTestSupport.initializeDatabase(driverManagerDataSource);

        tenantFallbackDataSource = new TenantFallbackDataSource(driverManagerDataSource);
        tenantSearchPathDataSource = new TenantSearchPathDataSource(driverManagerDataSource);
    }

    @AfterEach
    void afterEach() throws Exception {
        TenantContext.resetCurrentTenantId();

        for (SchedulerFactoryBean schedulerFactoryBean : schedulerFactoryBeans) {
            schedulerFactoryBean.destroy();
        }
    }

    @Test
    void testDelegateAndLockHandlerWrappersAreUsedAndReusedAcrossQuartzCalls() throws Exception {
        Scheduler scheduler = createScheduler("wrappersReused", tenantSearchPathDataSource);
        MultiTenantLocalDataSourceJobStore multiTenantLocalDataSourceJobStore = getJobStore(scheduler);
        JobDetail jobDetail = createJobDetail("job");

        callAsTenant(() -> {
            scheduler.scheduleJob(jobDetail, createTrigger("trigger", jobDetail));

            return null;
        });

        DriverDelegate driverDelegate = multiTenantLocalDataSourceJobStore.getDelegate();
        Semaphore lockHandler = multiTenantLocalDataSourceJobStore.getLockHandler();

        assertThat(driverDelegate).isInstanceOf(MultiTenantDriverDelegate.class);
        assertThat(lockHandler).isInstanceOf(MultiTenantSemaphore.class);
        assertThat(lockHandler.requiresConnection()).isTrue();

        assertThat(callAsTenant(() -> scheduler.checkExists(jobDetail.getKey()))).isTrue();
        assertThat(callAsTenant(() -> scheduler.deleteJob(jobDetail.getKey()))).isTrue();

        assertThat(multiTenantLocalDataSourceJobStore.getDelegate()).isSameAs(driverDelegate);
        assertThat(multiTenantLocalDataSourceJobStore.getLockHandler()).isSameAs(lockHandler);
        assertThat(countLocks("wrappersReused", "TRIGGER_ACCESS")).isOne();
    }

    @Test
    void testLockedQuartzCallsInTenantTransactionKeepSearchPathOfTransactionConnection() throws Exception {
        Scheduler scheduler = createScheduler("lockedTenantTransaction", tenantFallbackDataSource);
        JobDetail jobDetail = createJobDetail("job");
        JdbcTemplate tenantJdbcTemplate = new JdbcTemplate(tenantFallbackDataSource);
        TransactionTemplate transactionTemplate = new TransactionTemplate(
            new DataSourceTransactionManager(tenantFallbackDataSource));

        List<Object> observations = callAsTenant(() -> transactionTemplate.execute(transactionStatus -> {
            List<Object> currentObservations = new ArrayList<>();

            currentObservations.add(showSearchPath(tenantJdbcTemplate));

            callScheduler(() -> scheduler.scheduleJob(jobDetail, createTrigger("trigger", jobDetail)));

            currentObservations.add(showSearchPath(tenantJdbcTemplate));

            callScheduler(() -> scheduler.deleteJob(jobDetail.getKey()));

            currentObservations.add(showSearchPath(tenantJdbcTemplate));
            currentObservations.add(isPublicFallbackProbeResolvable(tenantJdbcTemplate));

            return currentObservations;
        }));

        assertThat(observations)
            .as("search_path before and after each locked Quartz call in the tenant transaction, then whether an " +
                "object in public still resolves")
            .containsExactly(
                TENANT_FALLBACK_SEARCH_PATH, TENANT_FALLBACK_SEARCH_PATH, TENANT_FALLBACK_SEARCH_PATH, true);
        assertThat(countLocks("lockedTenantTransaction", "TRIGGER_ACCESS")).isOne();
    }

    @Test
    void testLockHandlerWrapperFollowsReplacedLockHandler() throws Exception {
        Scheduler scheduler = createScheduler("lockHandlerReplaced", tenantSearchPathDataSource);
        MultiTenantLocalDataSourceJobStore multiTenantLocalDataSourceJobStore = getJobStore(scheduler);
        JobDetail jobDetail = createJobDetail("job");

        Semaphore firstLockHandler = multiTenantLocalDataSourceJobStore.getLockHandler();

        multiTenantLocalDataSourceJobStore.setLockHandler(
            new StdRowLockSemaphore("QRTZ_", "lockHandlerReplaced", null));

        Semaphore secondLockHandler = multiTenantLocalDataSourceJobStore.getLockHandler();

        assertThat(secondLockHandler)
            .isInstanceOf(MultiTenantSemaphore.class)
            .isNotSameAs(firstLockHandler);

        callAsTenant(() -> {
            scheduler.scheduleJob(jobDetail, createTrigger("trigger", jobDetail));

            return null;
        });

        assertThat(multiTenantLocalDataSourceJobStore.getLockHandler()).isSameAs(secondLockHandler);
        assertThat(countLocks("lockHandlerReplaced", "TRIGGER_ACCESS")).isOne();

        multiTenantLocalDataSourceJobStore.setLockHandler(null);

        assertThat(multiTenantLocalDataSourceJobStore.getLockHandler()).isNull();
    }

    @Test
    void testDelegateWrapperFollowsRecreatedDelegate() throws Exception {
        Scheduler scheduler = createScheduler("delegateRecreated", tenantSearchPathDataSource);
        MultiTenantLocalDataSourceJobStore multiTenantLocalDataSourceJobStore = getJobStore(scheduler);
        JobDetail jobDetail = createJobDetail("job");

        DriverDelegate firstDriverDelegate = multiTenantLocalDataSourceJobStore.getDelegate();

        resetUnderlyingDelegate(multiTenantLocalDataSourceJobStore);

        DriverDelegate secondDriverDelegate = multiTenantLocalDataSourceJobStore.getDelegate();

        assertThat(secondDriverDelegate)
            .isInstanceOf(MultiTenantDriverDelegate.class)
            .isNotSameAs(firstDriverDelegate);

        callAsTenant(() -> {
            scheduler.scheduleJob(jobDetail, createTrigger("trigger", jobDetail));

            return null;
        });

        assertThat(callAsTenant(() -> scheduler.checkExists(jobDetail.getKey()))).isTrue();
        assertThat(multiTenantLocalDataSourceJobStore.getDelegate()).isSameAs(secondDriverDelegate);
    }

    @Test
    void testConcurrentGetDelegateCallsShareOneWrapper() throws Exception {
        Scheduler scheduler = createScheduler("concurrentDelegate", tenantSearchPathDataSource);
        MultiTenantLocalDataSourceJobStore multiTenantLocalDataSourceJobStore = getJobStore(scheduler);
        int threadCount = 8;

        resetUnderlyingDelegate(multiTenantLocalDataSourceJobStore);

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        try {
            List<Future<DriverDelegate>> futures = new ArrayList<>();
            Callable<DriverDelegate> getDelegateCall = () -> {
                assertThat(startLatch.await(10, TimeUnit.SECONDS)).isTrue();

                return multiTenantLocalDataSourceJobStore.getDelegate();
            };

            for (int index = 0; index < threadCount; index++) {
                futures.add(executorService.submit(getDelegateCall));
            }

            startLatch.countDown();

            DriverDelegate expectedDriverDelegate = futures.getFirst()
                .get(10, TimeUnit.SECONDS);

            for (Future<DriverDelegate> future : futures) {
                assertThat(future.get(10, TimeUnit.SECONDS)).isSameAs(expectedDriverDelegate);
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    private Scheduler createScheduler(String schedulerName, DataSource schedulerDataSource) throws Exception {
        return MultiTenantQuartzTestSupport.createScheduler(
            schedulerName, true, schedulerDataSource, schedulerFactoryBeans);
    }

    private static MultiTenantLocalDataSourceJobStore getJobStore(Scheduler scheduler)
        throws ReflectiveOperationException {

        Field quartzSchedulerField = StdScheduler.class.getDeclaredField("sched");

        quartzSchedulerField.setAccessible(true);

        QuartzScheduler quartzScheduler = (QuartzScheduler) quartzSchedulerField.get(scheduler);

        Field resourcesField = QuartzScheduler.class.getDeclaredField("resources");

        resourcesField.setAccessible(true);

        QuartzSchedulerResources quartzSchedulerResources = (QuartzSchedulerResources) resourcesField.get(
            quartzScheduler);

        return (MultiTenantLocalDataSourceJobStore) quartzSchedulerResources.getJobStore();
    }

    private static void resetUnderlyingDelegate(MultiTenantLocalDataSourceJobStore multiTenantLocalDataSourceJobStore)
        throws ReflectiveOperationException {

        Field delegateField = JobStoreSupport.class.getDeclaredField("delegate");

        delegateField.setAccessible(true);

        delegateField.set(multiTenantLocalDataSourceJobStore, null);
    }

    private static void callScheduler(SchedulerCall schedulerCall) {
        try {
            schedulerCall.call();
        } catch (SchedulerException schedulerException) {
            throw new IllegalStateException(schedulerException);
        }
    }

    private static int countLocks(String schedulerName, String lockName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM public.qrtz_locks WHERE sched_name = ? AND lock_name = ?", Integer.class,
            schedulerName, lockName);

        return count == null ? 0 : count;
    }

    @FunctionalInterface
    private interface SchedulerCall {

        void call() throws SchedulerException;
    }
}
