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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class SingleCoordinatorCheckIntTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine");

    private static DriverManagerDataSource dataSource;
    private static JdbcTemplate jdbcTemplate;

    private final List<SchedulerFactoryBean> schedulerFactoryBeans = new ArrayList<>();

    @BeforeAll
    static void beforeAll() {
        dataSource = new DriverManagerDataSource(
            POSTGRESQL_CONTAINER.getJdbcUrl(), POSTGRESQL_CONTAINER.getUsername(),
            POSTGRESQL_CONTAINER.getPassword());

        ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator(
            new ClassPathResource("org/quartz/impl/jdbcjobstore/tables_postgres.sql"));

        resourceDatabasePopulator.execute(dataSource);

        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void afterEach() throws SchedulerException {
        for (SchedulerFactoryBean schedulerFactoryBean : schedulerFactoryBeans) {
            schedulerFactoryBean.destroy();
        }

        jdbcTemplate.update("DELETE FROM QRTZ_SCHEDULER_STATE");
    }

    @Test
    void testDetectsAnotherClusteredCoordinator() throws Exception {
        Scheduler firstScheduler = startScheduler(true);
        Scheduler secondScheduler = startScheduler(true);

        SingleCoordinatorCheck firstSingleCoordinatorCheck = new SingleCoordinatorCheck(jdbcTemplate, firstScheduler);
        SingleCoordinatorCheck secondSingleCoordinatorCheck =
            new SingleCoordinatorCheck(jdbcTemplate, secondScheduler);

        await()
            .atMost(Duration.ofSeconds(20))
            .untilAsserted(() -> {
                assertThat(firstSingleCoordinatorCheck.isAnotherCoordinatorRunning()).isTrue();
                assertThat(secondSingleCoordinatorCheck.isAnotherCoordinatorRunning()).isTrue();
            });
    }

    @Test
    void testIgnoresItsOwnScheduler() throws Exception {
        Scheduler scheduler = startScheduler(true);

        await()
            .atMost(Duration.ofSeconds(20))
            .until(() -> countSchedulerStates() == 1);

        assertThat(new SingleCoordinatorCheck(jdbcTemplate, scheduler).isAnotherCoordinatorRunning()).isFalse();
    }

    @Test
    void testIgnoresACoordinatorThatStoppedCheckingIn() throws Exception {
        Scheduler scheduler = startScheduler(true);

        jdbcTemplate.update(
            "INSERT INTO QRTZ_SCHEDULER_STATE (SCHED_NAME, INSTANCE_NAME, LAST_CHECKIN_TIME, CHECKIN_INTERVAL) " +
                "VALUES (?, ?, ?, ?)",
            scheduler.getSchedulerName(), "stopped", System.currentTimeMillis() - 60_000, 1_000);

        assertThat(new SingleCoordinatorCheck(jdbcTemplate, scheduler).isAnotherCoordinatorRunning()).isFalse();
    }

    @Test
    void testIgnoresSchedulersThatAreNotClustered() throws Exception {
        Scheduler scheduler = startScheduler(false);

        jdbcTemplate.update(
            "INSERT INTO QRTZ_SCHEDULER_STATE (SCHED_NAME, INSTANCE_NAME, LAST_CHECKIN_TIME, CHECKIN_INTERVAL) " +
                "VALUES (?, ?, ?, ?)",
            scheduler.getSchedulerName(), "other", System.currentTimeMillis(), 1_000);

        assertThat(new SingleCoordinatorCheck(jdbcTemplate, scheduler).isAnotherCoordinatorRunning()).isFalse();
    }

    private Integer countSchedulerStates() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM QRTZ_SCHEDULER_STATE", Integer.class);
    }

    private Scheduler startScheduler(boolean clustered) throws Exception {
        Properties properties = new Properties();

        properties.setProperty(
            "org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        properties.setProperty("org.quartz.jobStore.isClustered", String.valueOf(clustered));
        properties.setProperty("org.quartz.jobStore.clusterCheckinInterval", "1000");
        properties.setProperty("org.quartz.scheduler.instanceId", "AUTO");

        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();

        schedulerFactoryBean.setDataSource(dataSource);
        schedulerFactoryBean.setQuartzProperties(properties);
        schedulerFactoryBean.setSchedulerName("singleCoordinatorCheckScheduler");

        schedulerFactoryBean.afterPropertiesSet();

        schedulerFactoryBeans.add(schedulerFactoryBean);

        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        scheduler.start();

        return scheduler;
    }
}
