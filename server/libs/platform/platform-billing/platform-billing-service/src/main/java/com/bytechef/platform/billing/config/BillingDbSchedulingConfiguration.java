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

import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.STRIPE_USAGE_REPORT;

import com.bytechef.platform.billing.exception.PaymentClientException;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;
import com.github.kagkarlsson.scheduler.task.schedule.CronStyle;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("${billing.enabled:false} " +
    "and '${bytechef.scheduler.provider:quartz}'.equals('db-scheduler')")
public class BillingDbSchedulingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BillingDbSchedulingConfiguration.class);

    @Bean
    public Task<Void> stripeUsageReportTask(BillingUsageService billingUsageService) {
        return Tasks.recurring(STRIPE_USAGE_REPORT, new CronSchedule("0 0 * * * ?", ZoneOffset.UTC, CronStyle.QUARTZ))
            .execute((taskInstance, executionContext) -> {
                log.info("Start Stripe usage report");

                try {
                    billingUsageService.reportUsage(executionContext.getExecution().executionTime);
                } catch (PaymentClientException paymentClientException) {
                    log.error("Failed to report Stripe usage", paymentClientException);
                }
            });
    }
}
