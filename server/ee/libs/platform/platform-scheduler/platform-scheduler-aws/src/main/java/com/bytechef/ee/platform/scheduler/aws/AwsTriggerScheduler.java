/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode;
import software.amazon.awssdk.services.scheduler.model.Target;

public class AwsTriggerScheduler implements TriggerScheduler {

    private static final String SCHEDULE_TRIGGER = "ScheduleTrigger";
    private static final String POLLING_TRIGGER = "PollingTrigger";
    private static final String WEBHOOK_TRIGGER = "DynamicWebhookTriggerRefresh";

    private final SchedulerClient schedulerClient;
    private final String sqsArn;
    private final String roleArn;

    @SuppressFBWarnings("EI")
    public AwsTriggerScheduler(SchedulerClient schedulerClient, ApplicationProperties.Cloud.Aws aws) {
        this.schedulerClient = schedulerClient;

        String accountId = aws.getAccountId();

        this.sqsArn = "arn:aws:sqs:" + aws.getRegion() + ":" + accountId + ":schedule-queue";
        this.roleArn = "arn:aws:iam::" + accountId + ":role/schedule-role";
    }

    @Override
    public void cancelDynamicWebhookTriggerRefresh(String workflowExecutionId) {
        schedulerClient.deleteSchedule(request -> request.clientToken(workflowExecutionId)
            .groupName(WEBHOOK_TRIGGER)
            .name(WEBHOOK_TRIGGER + workflowExecutionId.substring(0,16)));
    }

    @Override
    public void cancelScheduleTrigger(String workflowExecutionId) {
        schedulerClient.deleteSchedule(request -> request.clientToken(workflowExecutionId.substring(16))
            .groupName(SCHEDULE_TRIGGER)
            .name(SCHEDULE_TRIGGER + workflowExecutionId.substring(0,16)));
    }

    @Override
    public void cancelPollingTrigger(String workflowExecutionId) {
        schedulerClient.deleteSchedule(request -> request.clientToken(workflowExecutionId)
            .groupName(POLLING_TRIGGER)
            .name(POLLING_TRIGGER + workflowExecutionId.substring(0,16)));
    }

    @Override
    public void scheduleDynamicWebhookTriggerRefresh(
        LocalDateTime webhookExpirationDate, String componentName, int componentVersion,
        WorkflowExecutionId workflowExecutionId, Long connectionId) {

        Target sqsTarget = Target.builder()
            .roleArn(roleArn)
            .arn(sqsArn)
            .input("ConnectionId: " + connectionId + workflowExecutionId.toString())
            .build();

        schedulerClient.createSchedule(request -> request.clientToken(workflowExecutionId.toString().substring(16))
            .groupName(WEBHOOK_TRIGGER)
            .name(WEBHOOK_TRIGGER + workflowExecutionId.toString().substring(0,16))
            .target(sqsTarget)
            .flexibleTimeWindow(mode -> mode.mode(FlexibleTimeWindowMode.OFF))
            .startDate(webhookExpirationDate.toInstant(ZoneOffset.UTC))); // change when choosing bytechefs aws region
    }

    @Override
    public void scheduleScheduleTrigger(
        String pattern, String zoneId, Map<String, Object> output, WorkflowExecutionId workflowExecutionId) {

        Target sqsTarget = Target.builder()
            .roleArn(roleArn)
            .arn(sqsArn)
            .input(JsonUtils.write(output) + "_Splitter_" + workflowExecutionId.toString())
//            .retryPolicy(RetryPolicy.builder().maximumRetryAttempts().build())
//            .sqsParameters(SqsParameters.builder().messageGroupId().build())
            .deadLetterConfig(builder -> builder.arn(sqsArn).build())
            .build();

        schedulerClient.createSchedule(request -> request.clientToken(workflowExecutionId.toString().substring(16))
            .groupName(SCHEDULE_TRIGGER)
            .name(SCHEDULE_TRIGGER + workflowExecutionId.toString().substring(0,16))
            .target(sqsTarget)
            .flexibleTimeWindow(mode -> mode.mode(FlexibleTimeWindowMode.OFF))
            .scheduleExpressionTimezone(zoneId)
            .startDate(Instant.now())
            .scheduleExpression("cron(" + pattern.substring(2) + ")"));
    }

    @Override
    public void schedulePollingTrigger(WorkflowExecutionId workflowExecutionId) {
        Target sqsTarget = Target.builder()
            .roleArn(roleArn)
            .arn(sqsArn)
            .input(workflowExecutionId.toString())
            .build();

        schedulerClient.createSchedule(request -> request.clientToken(workflowExecutionId.toString().substring(16))
            .groupName(POLLING_TRIGGER)
            .name(POLLING_TRIGGER + workflowExecutionId.toString().substring(0,16))
            .target(sqsTarget)
            .flexibleTimeWindow(mode -> mode.mode(FlexibleTimeWindowMode.OFF))
            .startDate(Instant.now())
            .scheduleExpression("rate(5 minutes)"));
    }
}
