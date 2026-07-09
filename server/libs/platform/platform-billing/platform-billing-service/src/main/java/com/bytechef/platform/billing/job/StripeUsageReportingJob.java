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

package com.bytechef.platform.billing.job;

import com.bytechef.platform.billing.service.BillingUsageService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Matija Petanjek
 */
public class StripeUsageReportingJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(StripeUsageReportingJob.class);

    private BillingUsageService billingUsageService;

    @Override
    public void execute(JobExecutionContext context) {
        logger.info("Start Stripe usage report");

        billingUsageService.reportUsage(context.getScheduledFireTime()
            .toInstant());
    }

    @Autowired
    public void setBillingUsageService(BillingUsageService billingUsageService) {
        this.billingUsageService = billingUsageService;
    }
}
