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

package com.bytechef.platform.billing.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class BillingSchedulingConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(BillingSchedulingConfiguration.class);

    @Test
    void testRegistersUsageReportingJobForUppercaseQuartzProvider() {
        applicationContextRunner
            .withPropertyValues("billing.enabled=true", "bytechef.scheduler.provider=QUARTZ")
            .run(context -> assertThat(context).hasBean("stripeUsageReportingJobDetail"));
    }

    @Test
    void testRegistersUsageReportingJobForLowercaseQuartzProvider() {
        applicationContextRunner
            .withPropertyValues("billing.enabled=true", "bytechef.scheduler.provider=quartz")
            .run(context -> assertThat(context).hasBean("stripeUsageReportingJobDetail"));
    }

    @Test
    void testSkipsUsageReportingJobForAwsProvider() {
        applicationContextRunner
            .withPropertyValues("billing.enabled=true", "bytechef.scheduler.provider=AWS")
            .run(context -> assertThat(context).doesNotHaveBean("stripeUsageReportingJobDetail"));
    }

    @Test
    void testSkipsUsageReportingJobWhenBillingIsDisabled() {
        applicationContextRunner
            .withPropertyValues("billing.enabled=false", "bytechef.scheduler.provider=QUARTZ")
            .run(context -> assertThat(context).doesNotHaveBean("stripeUsageReportingJobDetail"));
    }
}
