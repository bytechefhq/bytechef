/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.listener;

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsBillingSchedulingConstants.SCHEDULER_STRIPE_USAGE_REPORTING_QUEUE;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsBillingSchedulingConstants.STRIPE_USAGE_REPORTING_LISTENER_ID;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SCHEDULER_SQS_LISTENER_CONTAINER_FACTORY;

import com.bytechef.platform.billing.service.BillingUsageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.time.Instant;

/**
 * @version ee
 *
 * @author Matija Petanjek
 */
public class StripeUsageReportListener {

    private final BillingUsageService billingUsageService;

    @SuppressFBWarnings("EI")
    public StripeUsageReportListener(BillingUsageService billingUsageService) {
        this.billingUsageService = billingUsageService;
    }

    @SqsListener(
        queueNames = SCHEDULER_STRIPE_USAGE_REPORTING_QUEUE,
        id = STRIPE_USAGE_REPORTING_LISTENER_ID,
        factory = SCHEDULER_SQS_LISTENER_CONTAINER_FACTORY)
    public void onSchedule(String message) {
        billingUsageService.reportUsage(Instant.now());
    }
}
