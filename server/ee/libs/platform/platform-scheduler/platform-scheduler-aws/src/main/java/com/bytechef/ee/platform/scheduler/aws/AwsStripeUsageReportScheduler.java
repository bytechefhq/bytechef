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

package com.bytechef.ee.platform.scheduler.aws;

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsBillingSchedulingConstants.SCHEDULER_STRIPE_USAGE_REPORTING_QUEUE;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsBillingSchedulingConstants.STRIPE_USAGE_REPORTING;

import com.bytechef.config.ApplicationProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.ConflictException;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode;
import software.amazon.awssdk.services.scheduler.model.Target;

/**
 * @author Matija Petanjek
 */
public class AwsStripeUsageReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(AwsStripeUsageReportScheduler.class);

    private final SchedulerClient schedulerClient;
    private final String sqsArn;
    private final String roleArn;

    @SuppressFBWarnings("EI")
    public AwsStripeUsageReportScheduler(
        ApplicationProperties applicationProperties, SchedulerClient schedulerClient) {

        this.schedulerClient = schedulerClient;

        String accountId = applicationProperties.getCloud()
            .getAws()
            .getAccountId();

        this.sqsArn = "arn:aws:sqs:" + applicationProperties.getCloud()
            .getAws()
            .getRegion() + ":" + accountId;
        this.roleArn = "arn:aws:iam::" + accountId + ":role/schedule-role";
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleStripeUsageReporting() {
        Target sqsTarget = Target.builder()
            .roleArn(roleArn)
            .arn(sqsArn + ":" + SCHEDULER_STRIPE_USAGE_REPORTING_QUEUE)
            .input(STRIPE_USAGE_REPORTING)
            .build();

        try {
            schedulerClient.createSchedule(request -> request
                .name(STRIPE_USAGE_REPORTING)
                .groupName(STRIPE_USAGE_REPORTING)
                .scheduleExpression("rate(1 hours)")
                .target(sqsTarget)
                .flexibleTimeWindow(mode -> mode.mode(FlexibleTimeWindowMode.OFF)));
        } catch (ConflictException e) {
            log.warn("Schedule already exists — no update needed for a static recurring schedule");
        }
    }
}
