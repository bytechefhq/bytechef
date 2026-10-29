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

package com.bytechef.platform.scheduler.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.tenant.multi.sql.MultiTenantDataSource;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class SchedulerLockConfigurationIntTest {

    private static final String CHANGELOG = "classpath:config/liquibase/changelog/scheduler/" +
        "20261029000000_platform_scheduler_added_shedlock_table.xml";

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine");

    private static DriverManagerDataSource dataSource;
    private static JdbcTemplate jdbcTemplate;

    @BeforeAll
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    static void beforeAll() throws Exception {
        dataSource = new DriverManagerDataSource(
            POSTGRESQL_CONTAINER.getJdbcUrl(), POSTGRESQL_CONTAINER.getUsername(),
            POSTGRESQL_CONTAINER.getPassword());

        SpringLiquibase springLiquibase = new SpringLiquibase();

        springLiquibase.setChangeLog(CHANGELOG);
        springLiquibase.setDataSource(dataSource);
        springLiquibase.setResourceLoader(new DefaultResourceLoader());

        springLiquibase.afterPropertiesSet();

        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute(
            "CREATE SCHEMA " + TenantContext.callWithTenantId("000001", TenantContext::getCurrentDatabaseSchema));
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM public.shedlock");
    }

    @Test
    void testRunsAJobOnOneCoordinatorWhenEveryCoordinatorFiresIt() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CyclicBarrier cyclicBarrier = new CyclicBarrier(2);

        try (AnnotationConfigApplicationContext firstCoordinator = createCoordinator(dataSource, runs);
            AnnotationConfigApplicationContext secondCoordinator = createCoordinator(dataSource, runs)) {

            List<CompletableFuture<Void>> firings = List.of(
                fire(firstCoordinator.getBean(CleanupJob.class), cyclicBarrier),
                fire(secondCoordinator.getBean(CleanupJob.class), cyclicBarrier));

            CompletableFuture.allOf(firings.toArray(CompletableFuture[]::new))
                .get();

            assertThat(runs).hasValue(1);

            CleanupJob lateCoordinatorCleanupJob = secondCoordinator.getBean(CleanupJob.class);

            lateCoordinatorCleanupJob.run();

            assertThat(runs).hasValue(1);
        }

        assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM public.shedlock WHERE name = 'cleanupJob'", Integer.class)).isEqualTo(1);
    }

    @Test
    void testKeepsOneLockInThePublicSchemaForAllTenants() {
        AtomicInteger runs = new AtomicInteger();

        try (AnnotationConfigApplicationContext coordinator = createCoordinator(
            new MultiTenantDataSource(dataSource), runs)) {

            CleanupJob cleanupJob = coordinator.getBean(CleanupJob.class);

            TenantContext.runWithTenantId("000001", cleanupJob::run);
            TenantContext.runWithTenantId("000002", cleanupJob::run);
        }

        assertThat(runs).hasValue(1);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM public.shedlock WHERE name = 'cleanupJob'", Integer.class)).isEqualTo(1);
    }

    @Test
    void testRegisteredOnCoordinatorOnly() {
        ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SchedulerLockConfiguration.class)
            .withBean(DataSource.class, () -> dataSource);

        applicationContextRunner.run(context -> assertThat(context).hasSingleBean(LockProvider.class));

        applicationContextRunner.withPropertyValues("bytechef.coordinator.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(LockProvider.class));
    }

    private static AnnotationConfigApplicationContext createCoordinator(DataSource dataSource, AtomicInteger runs) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

        applicationContext.registerBean(DataSource.class, () -> dataSource);
        applicationContext.registerBean(CleanupJob.class, () -> new CleanupJob(runs));
        applicationContext.register(SchedulerLockConfiguration.class);

        applicationContext.refresh();

        return applicationContext;
    }

    private static CompletableFuture<Void> fire(CleanupJob cleanupJob, CyclicBarrier cyclicBarrier) {
        return CompletableFuture.runAsync(() -> {
            try {
                cyclicBarrier.await();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }

            cleanupJob.run();
        });
    }

    static class CleanupJob {

        private final AtomicInteger runs;

        CleanupJob(AtomicInteger runs) {
            this.runs = runs;
        }

        @SchedulerLock(name = "cleanupJob", lockAtLeastFor = "PT5M")
        public void run() {
            runs.incrementAndGet();
        }
    }
}
