/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws;

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsConnectionRefreshSchedulerConstants.CONNECTION_REFRESH;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsConnectionRefreshSchedulerConstants.SCHEDULER_CONNECTION_REFRESH_QUEUE;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants;
import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.ConflictException;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode;
import software.amazon.awssdk.services.scheduler.model.Target;

/**
 * @version ee
 *
 * @author Nikolina Spehar
 */
public class AwsConnectionRefreshScheduler implements ConnectionRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(AwsConnectionRefreshScheduler.class);

    private final SchedulerClient schedulerClient;
    private final String sqsArn;
    private final String roleArn;

    @SuppressFBWarnings("EI")
    public AwsConnectionRefreshScheduler(ApplicationProperties.Cloud.Aws aws, SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;

        String accountId = aws.getAccountId();

        this.sqsArn = "arn:aws:sqs:" + aws.getRegion() + ":" + accountId;
        this.roleArn = "arn:aws:iam::" + accountId + ":role/schedule-role";
    }

    @Override
    public void cancelConnectionRefresh(Long connectionId, String tenantId) {
        try {
            schedulerClient.deleteSchedule(request -> request.clientToken(tenantId + connectionId)
                .groupName(CONNECTION_REFRESH)
                .name(CONNECTION_REFRESH + tenantId + connectionId));
        } catch (RuntimeException e) {
            if (log.isDebugEnabled()) {
                log.debug("Dynamic Webhook Trigger Refresh not defined");
            }
        }
    }

    @Override
    public void scheduleConnectionRefresh(Long connectionId, Instant expiry, String tenantId) {
        String scheduleName = CONNECTION_REFRESH + tenantId + connectionId;
        String clientToken = tenantId + connectionId;

        Instant now = Instant.now();

        Duration between = Duration.between(now, expiry);

        long secondsUntilExpiry = between.getSeconds();

        long minutesUntilExpiry = secondsUntilExpiry / 60;

        long refreshMinutes = Math.max(1, minutesUntilExpiry - 5);

        String scheduleExpression = "rate(" + refreshMinutes + " minutes)";

        log.info(
            "Scheduling connection refresh for connection: {}, tenant: {}, expiry time: {}, refresh time: {}, " +
                "schedule expression: {}, SQS ARN: {}, role ARN: {}",
            connectionId, tenantId, expiry, refreshMinutes, scheduleExpression,
            sqsArn + ":" + SCHEDULER_CONNECTION_REFRESH_QUEUE, roleArn);

        Target sqsTarget = Target.builder()
            .roleArn(roleArn)
            .arn(sqsArn + ":" + SCHEDULER_CONNECTION_REFRESH_QUEUE)
            .input(tenantId + AwsTriggerSchedulerConstants.SPLITTER + connectionId)
            .build();

        try {
            schedulerClient.createSchedule(request -> request.clientToken(clientToken)
                .groupName(CONNECTION_REFRESH)
                .name(scheduleName)
                .scheduleExpression(scheduleExpression)
                .target(sqsTarget)
                .flexibleTimeWindow(mode -> mode.mode(FlexibleTimeWindowMode.OFF))
                .startDate(now.plus(Duration.ofMinutes(1))));

            log.info("Schedule created successfully.");
        } catch (ConflictException e) {
            log.info("Schedule already exists, updating...");

            schedulerClient.updateSchedule(request -> request.clientToken(clientToken)
                .groupName(CONNECTION_REFRESH)
                .name(scheduleName)
                .scheduleExpression(scheduleExpression)
                .target(sqsTarget)
                .flexibleTimeWindow(mode -> mode.mode(FlexibleTimeWindowMode.OFF))
                .startDate(now.plus(Duration.ofMinutes(1))));

            log.info("Schedule updated successfully.");
        }
    }
}
