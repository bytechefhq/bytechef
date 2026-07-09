package com.bytechef.ee.platform.scheduler.aws;

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

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsBillingSchedulingConstants.SCHEDULER_STRIPE_USAGE_REPORTING_QUEUE;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsBillingSchedulingConstants.STRIPE_USAGE_REPORTING;

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
            log.error("Schedule already exists — no update needed for a static recurring schedule");
        }
    }
}
