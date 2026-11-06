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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Ivica Cardic
 */
class DbSchedulerEnvironmentPostProcessorTest {

    private final DbSchedulerEnvironmentPostProcessor postProcessor = new DbSchedulerEnvironmentPostProcessor();

    @Test
    void testQuartzProviderDisablesBothStarters() {
        MockEnvironment environment = new MockEnvironment().withProperty("bytechef.scheduler.provider", "quartz");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        Assertions.assertThat(environment.getProperty("db-scheduler.enabled", Boolean.class))
            .isFalse();
        Assertions.assertThat(environment.getProperty("db-scheduler-ui.enabled", Boolean.class))
            .isFalse();
    }

    @Test
    void testMissingProviderDisablesBothStarters() {
        MockEnvironment environment = new MockEnvironment();

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        Assertions.assertThat(environment.getProperty("db-scheduler.enabled", Boolean.class))
            .isFalse();
    }

    @Test
    void testDbSchedulerProviderEnablesBothStarters() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.scheduler.provider", "db-scheduler");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        Assertions.assertThat(environment.getProperty("db-scheduler.enabled", Boolean.class))
            .isTrue();
        Assertions.assertThat(environment.getProperty("db-scheduler-ui.enabled", Boolean.class))
            .isTrue();
    }

    @Test
    void testUiCanBeDisabledSeparately() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.scheduler.provider", "db-scheduler")
            .withProperty("bytechef.scheduler.db-scheduler.ui.enabled", "false");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        Assertions.assertThat(environment.getProperty("db-scheduler.enabled", Boolean.class))
            .isTrue();
        Assertions.assertThat(environment.getProperty("db-scheduler-ui.enabled", Boolean.class))
            .isFalse();
    }

    @Test
    void testExplicitPropertyWinsOverDerivedValue() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.scheduler.provider", "db-scheduler")
            .withProperty("db-scheduler.enabled", "false");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        Assertions.assertThat(environment.getProperty("db-scheduler.enabled", Boolean.class))
            .isFalse();
    }

    @Test
    void testWrongCaseProviderDisablesBothStarters() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.scheduler.provider", "DB-SCHEDULER");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        Assertions.assertThat(environment.getProperty("db-scheduler.enabled", Boolean.class))
            .isFalse();
        Assertions.assertThat(environment.getProperty("db-scheduler-ui.enabled", Boolean.class))
            .isFalse();
    }
}
