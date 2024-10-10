/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws;

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.DYNAMIC_WEBHOOK_TRIGGER_REFRESH;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SCHEDULER_DYNAMIC_WEBHOOK_TRIGGER_REFRESH_QUEUE;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SCHEDULER_POLLING_TRIGGER_QUEUE;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SCHEDULER_SCHEDULE_TRIGGER_QUEUE;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode;
import software.amazon.awssdk.services.scheduler.model.Target;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
public class AwsTriggerScheduler implements TriggerScheduler {

    private static final Logger log = LoggerFactory.getLogger(AwsTriggerScheduler.class);

    private static final String SCHEDULE_TRIGGER = "ScheduleTrigger";
    private static final String POLLING_TRIGGER = "PollingTrigger";
    private static final String SPLITTER = "|_$plitter_|";

    private final SchedulerClient schedulerClient;
    private final String sqsArn;
    private final String roleArn;

    @SuppressFBWarnings("EI")
    public AwsTriggerScheduler(SchedulerClient schedulerClient, ApplicationProperties.Cloud.Aws aws) {
        this.schedulerClient = schedulerClient;

        String accountId = aws.getAccountId();

        this.sqsArn = "arn:aws:sqs:" + aws.getRegion() + ":" + accountId;
        this.roleArn = "arn:aws:iam::" + accountId + ":role/schedule-role";
    }

    @Override
    public void cancelDynamicWebhookTriggerRefresh(String workflowExecutionId) {
        try {
            schedulerClient.deleteSchedule(request -> request.clientToken(workflowExecutionId.substring(16))
                .groupName(DYNAMIC_WEBHOOK_TRIGGER_REFRESH)
                .name(DYNAMIC_WEBHOOK_TRIGGER_REFRESH + workflowExecutionId.substring(0, 16)));
        } catch (RuntimeException e) {
            if (log.isDebugEnabled()) {
                log.debug("Dynamic Webhook Trigger Refresh not defined");
            }
        }
    }

    @Override
    public void cancelScheduleTrigger(String workflowExecutionId) {
        schedulerClient.deleteSchedule(request -> request.clientToken(workflowExecutionId.substring(16))
            .groupName(SCHEDULE_TRIGGER)
            .name(SCHEDULE_TRIGGER + workflowExecutionId.substring(0, 16)));
    }

    @Override
    public void cancelPollingTrigger(String workflowExecutionId) {
        schedulerClient.deleteSchedule(request -> request.clientToken(workflowExecutionId.substring(16))
            .groupName(POLLING_TRIGGER)
            .name(POLLING_TRIGGER + workflowExecutionId.substring(0, 16)));
    }

    @Override
    public void scheduleDynamicWebhookTriggerRefresh(
        LocalDateTime webhookExpirationDate, String componentName, int componentVersion,
        WorkflowExecutionId workflowExecutionId, Long connectionId) {

        String workflowExecutionIdString = workflowExecutionId.toString();

        Target sqsTarget = Target.builder()
            .roleArn(roleArn)
            .arn(sqsArn + ":" + SCHEDULER_DYNAMIC_WEBHOOK_TRIGGER_REFRESH_QUEUE)
            .input(workflowExecutionIdString + SPLITTER + connectionId)
            .build();

        schedulerClient.createSchedule(request -> request.clientToken(workflowExecutionIdString.substring(16))
            .groupName(DYNAMIC_WEBHOOK_TRIGGER_REFRESH)
            .name(DYNAMIC_WEBHOOK_TRIGGER_REFRESH + workflowExecutionIdString.substring(0, 16))
            .target(sqsTarget)
            .flexibleTimeWindow(mode -> mode.mode(FlexibleTimeWindowMode.OFF))
            .startDate(webhookExpirationDate.toInstant(ZoneOffset.UTC)));
    }

    @Override
    public void schedulePollingTrigger(WorkflowExecutionId workflowExecutionId) {
        String workflowExecutionIdString = workflowExecutionId.toString();

        Target sqsTarget = Target.builder()
            .roleArn(roleArn)
            .arn(sqsArn + ":" + SCHEDULER_POLLING_TRIGGER_QUEUE)
            .input(workflowExecutionIdString)
            .build();

        schedulerClient.createSchedule(request -> request.clientToken(workflowExecutionIdString.substring(16))
            .groupName(POLLING_TRIGGER)
            .name(POLLING_TRIGGER + workflowExecutionIdString.substring(0, 16))
            .target(sqsTarget)
            .flexibleTimeWindow(mode -> mode.mode(FlexibleTimeWindowMode.OFF))
            .startDate(Instant.now())
            .scheduleExpression("rate(5 minutes)"));
    }

    @Override
    public void scheduleScheduleTrigger(
        String pattern, String zoneId, Map<String, Object> output, WorkflowExecutionId workflowExecutionId) {

        String workflowExecutionIdString = workflowExecutionId.toString();

        Target sqsTarget = Target.builder()
            .roleArn(roleArn)
            .arn(sqsArn + ":" + SCHEDULER_SCHEDULE_TRIGGER_QUEUE)
            .input(workflowExecutionIdString + SPLITTER + JsonUtils.write(output))
            .deadLetterConfig(builder -> builder.arn(sqsArn + ":" + SCHEDULER_SCHEDULE_TRIGGER_QUEUE)
                .build())
            .build();

        schedulerClient.createSchedule(request -> request.clientToken(workflowExecutionIdString.substring(16))
            .groupName(SCHEDULE_TRIGGER)
            .name(SCHEDULE_TRIGGER + workflowExecutionIdString.substring(0, 16))
            .target(sqsTarget)
            .flexibleTimeWindow(mode -> mode.mode(FlexibleTimeWindowMode.OFF))
            .scheduleExpressionTimezone(zoneId)
            .startDate(Instant.now())
            .scheduleExpression("cron(" + pattern.substring(2) + ")"));
    }
}
