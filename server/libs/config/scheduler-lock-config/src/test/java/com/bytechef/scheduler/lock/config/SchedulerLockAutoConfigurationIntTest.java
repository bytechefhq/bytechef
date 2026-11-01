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

package com.bytechef.scheduler.lock.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class SchedulerLockAutoConfigurationIntTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SchedulerLockAutoConfiguration.class))
        .withUserConfiguration(DataSourceConfiguration.class);

    @Test
    void testLockedJobRunsOnceWhileItsLockIsHeld() {
        applicationContextRunner
            .withUserConfiguration(LockedJobConfiguration.class)
            .run(context -> {
                LockedJob lockedJob = context.getBean(LockedJob.class);

                lockedJob.run();
                lockedJob.run();

                JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));

                assertThat(lockedJob.getRunCount()).isEqualTo(1);
                assertThat(
                    jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM public.shedlock WHERE name = ?", Integer.class, LockedJob.LOCK_NAME))
                            .isEqualTo(1);
            });
    }

    @Test
    void testExistingLockProviderIsKept() {
        applicationContextRunner
            .withUserConfiguration(ExistingLockProviderConfiguration.class)
            .run(context -> assertThat(context.getBeansOfType(LockProvider.class))
                .containsOnlyKeys("existingLockProvider"));
    }

    @Configuration
    static class DataSourceConfiguration {

        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        }

        @Bean
        SpringLiquibase springLiquibase(DataSource dataSource) {
            SpringLiquibase springLiquibase = new SpringLiquibase();

            springLiquibase.setChangeLog(
                "classpath:config/liquibase/changelog/scheduler/20261029000000_platform_scheduler_added_shedlock_table.xml");
            springLiquibase.setDataSource(dataSource);

            return springLiquibase;
        }
    }

    @Configuration
    static class ExistingLockProviderConfiguration {

        @Bean
        LockProvider existingLockProvider() {
            return lockConfiguration -> Optional.empty();
        }
    }

    @Configuration
    static class LockedJobConfiguration {

        @Bean
        LockedJob lockedJob() {
            return new LockedJob();
        }
    }

    static class LockedJob {

        static final String LOCK_NAME = "schedulerLockAutoConfigurationIntTest";

        private final AtomicInteger runCount = new AtomicInteger();

        @SchedulerLock(name = LOCK_NAME, lockAtLeastFor = "PT5M")
        public void run() {
            runCount.incrementAndGet();
        }

        int getRunCount() {
            return runCount.get();
        }
    }
}
