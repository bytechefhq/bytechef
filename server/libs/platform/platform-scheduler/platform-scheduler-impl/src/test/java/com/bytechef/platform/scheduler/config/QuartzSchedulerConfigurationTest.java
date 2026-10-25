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
import static org.mockito.Mockito.mock;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.scheduler.TriggerScheduler;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @author Ivica Cardic
 */
class QuartzSchedulerConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(QuartzSchedulerConfiguration.class)
        .withBean(ApplicationProperties.class, ApplicationProperties::new)
        .withBean(Scheduler.class, () -> mock(Scheduler.class));

    @Test
    void testQuartzDelayerIsRegisteredOnCoordinator() {
        applicationContextRunner.run(context -> assertThat(context)
            .hasBean("quartzDelayer"));
    }

    @Test
    void testQuartzDelayerIsAbsentWhenCoordinatorIsDisabled() {
        applicationContextRunner.withPropertyValues("bytechef.coordinator.enabled=false")
            .run(context -> assertThat(context)
                .doesNotHaveBean("quartzDelayer")
                .hasSingleBean(TriggerScheduler.class));
    }
}
