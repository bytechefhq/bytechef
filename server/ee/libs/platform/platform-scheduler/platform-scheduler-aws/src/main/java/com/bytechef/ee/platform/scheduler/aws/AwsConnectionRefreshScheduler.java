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
 * @author Igor Beslic
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
                .name(getScheduleName(connectionId, tenantId)));

            log.info("Canceling connection refresh for connection: {}, tenant: {}", connectionId, tenantId);
        } catch (RuntimeException e) {
            log.error("Unable to cancel connection refresh trigger");

            if (log.isDebugEnabled()) {
                log.debug("Connection Refresh Trigger not defined", e);
            }
        }
    }

    @Override
    public void scheduleConnectionRefresh(Long connectionId, Instant tokenExpirationTime, String tenantId) {
        String scheduleName = getScheduleName(connectionId, tenantId);
        String clientToken = tenantId + connectionId;

        String scheduleExpression = getTokenRefreshScheduleExpression(tokenExpirationTime);

        log.info(
            "Schedule configuration for connectionId: {}, tenantId: {}, expiry time: {}, " +
                "schedule expression: {}, SQS ARN: [{}:{}], role ARN: {}",
            connectionId, tenantId, tokenExpirationTime, scheduleExpression, sqsArn, SCHEDULER_CONNECTION_REFRESH_QUEUE,
            roleArn);

        Target sqsTarget = Target.builder()
            .roleArn(roleArn)
            .arn(sqsArn + ":" + SCHEDULER_CONNECTION_REFRESH_QUEUE)
            .input(tenantId + AwsTriggerSchedulerConstants.SPLITTER + connectionId)
            .build();

        Instant schedulingStartDate = Instant.now()
            .plus(Duration.ofMinutes(1));

        try {
            schedulerClient.createSchedule(request -> request.clientToken(clientToken)
                .groupName(CONNECTION_REFRESH)
                .name(scheduleName)
                .scheduleExpression(scheduleExpression)
                .target(sqsTarget)
                .flexibleTimeWindow(mode -> mode.mode(FlexibleTimeWindowMode.OFF))
                .startDate(schedulingStartDate));

            log.info("Schedule created successfully");
        } catch (ConflictException e) {
            log.warn("Schedule already exists, it will be updated");

            if (log.isDebugEnabled()) {
                log.debug("Problem details", e);
            }

            schedulerClient.updateSchedule(request -> request.clientToken(clientToken)
                .groupName(CONNECTION_REFRESH)
                .name(scheduleName)
                .scheduleExpression(scheduleExpression)
                .target(sqsTarget)
                .flexibleTimeWindow(mode -> mode.mode(FlexibleTimeWindowMode.OFF))
                .startDate(schedulingStartDate));

            log.info("Schedule updated successfully.");
        }
    }

    private static String getScheduleName(Long connectionId, String tenantId) {
        return CONNECTION_REFRESH + "_" + tenantId + "_" + connectionId;
    }

    /**
     * Returns aws schedule expression that fires {@link #MINUTES_BEFORE_TOKEN_EXPIRES} minutes before time given by
     * #tokenExpirationTime argument
     *
     * @param tokenExpirationTime the expected time of token expiry
     * @return
     */
    private String getTokenRefreshScheduleExpression(Instant tokenExpirationTime) {
        Duration tokenValidityDuration = Duration.between(Instant.now(), tokenExpirationTime);

        return "rate(" + Math.max(1, (tokenValidityDuration.getSeconds() / 60) - MINUTES_BEFORE_TOKEN_EXPIRES) +
            " minutes)";
    }

    private static final int MINUTES_BEFORE_TOKEN_EXPIRES = 5;
}
