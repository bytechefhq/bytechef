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

import com.bytechef.platform.billing.job.StripeUsageReportingJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.TriggerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Matija Petanjek
 */
@Configuration
@ConditionalOnExpression("${bytechef.billing.enabled:false} " +
    "and '${bytechef.coordinator.trigger.scheduler.provider:quartz}'.equals('quartz')")
public class BillingSchedulingConfiguration {

    @Bean
    public JobDetail stripeUsageReportingJobDetail() {
        return JobBuilder.newJob(StripeUsageReportingJob.class)
            .withIdentity("stripeUsageReportingJob")
            .storeDurably()
            .build();
    }

    @Bean
    public CronTrigger stripeUsageReportingTrigger(JobDetail stripeUsageReportingJobDetail) {
        return TriggerBuilder.newTrigger()
            .forJob(stripeUsageReportingJobDetail)
            .withIdentity("stripeUsageReportingTrigger")
            .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?"))
            .build();
    }
}
