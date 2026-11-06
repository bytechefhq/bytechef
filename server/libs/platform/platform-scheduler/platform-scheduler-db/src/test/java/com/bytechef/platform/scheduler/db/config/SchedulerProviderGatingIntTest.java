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

package com.bytechef.platform.scheduler.db.config;

import com.bytechef.platform.scheduler.db.DbTriggerScheduler;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SchedulerProviderGatingIntTest {

    @Nested
    @SpringBootTest(
        classes = DbSchedulerTestConfiguration.class,
        properties = {
            "spring.profiles.active=test", "bytechef.scheduler.provider=quartz"
        })
    @Import(PostgreSQLContainerConfiguration.class)
    class QuartzProvider {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        void testNoDbSchedulerBeansUnderQuartz() {
            Assertions.assertThat(applicationContext.getBeanNamesForType(DbTriggerScheduler.class))
                .isEmpty();
            Assertions.assertThat(applicationContext.containsBean("quartzImportTask"))
                .as("the quartz-import recurring task is a db-scheduler-only bean")
                .isFalse();
            Assertions.assertThat(
                applicationContext.getBeanNamesForType(com.github.kagkarlsson.scheduler.Scheduler.class))
                .as("db-scheduler.enabled=false keeps the starter off")
                .isEmpty();
        }
    }

    @Nested
    @SpringBootTest(classes = DbSchedulerTestConfiguration.class, properties = "spring.profiles.active=test")
    @Import(PostgreSQLContainerConfiguration.class)
    class DbSchedulerProvider {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private Scheduler quartzScheduler;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @AfterEach
        void tearDown() {
            jdbcTemplate.update("DELETE FROM scheduled_tasks");
        }

        @Test
        void testQuartzIsPresentButNeverStartedUnderDbScheduler() throws SchedulerException {
            Assertions.assertThat(quartzScheduler.isStarted())
                .isFalse();
            Assertions.assertThat(applicationContext.getBeanNamesForType(DbTriggerScheduler.class))
                .hasSize(1);
            Assertions.assertThat(applicationContext.containsBean("quartzImportTask"))
                .as("the quartz-import recurring task registers exactly once under db-scheduler")
                .isTrue();
            Assertions.assertThat(applicationContext.getBean(com.github.kagkarlsson.scheduler.Scheduler.class)
                .getSchedulerState()
                .isStarted())
                .isTrue();
        }
    }
}
