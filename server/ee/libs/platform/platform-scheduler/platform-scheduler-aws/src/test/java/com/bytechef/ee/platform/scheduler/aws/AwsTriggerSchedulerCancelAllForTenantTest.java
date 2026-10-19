/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.config.ApplicationProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.ListSchedulesRequest;
import software.amazon.awssdk.services.scheduler.model.ListSchedulesResponse;
import software.amazon.awssdk.services.scheduler.model.ScheduleSummary;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AwsTriggerSchedulerCancelAllForTenantTest {

    private static final String SCHEDULE_TRIGGER_000001 = "ScheduleTrigger" + nameTail("000001", "scheduleTrigger");
    private static final String SCHEDULE_TRIGGER_000002 = "ScheduleTrigger" + nameTail("000002", "scheduleTrigger");
    private static final String POLLING_TRIGGER_000001 = "PollingTrigger" + nameTail("000001", "pollingTrigger");
    private static final String POLLING_TRIGGER_000002 = "PollingTrigger" + nameTail("000002", "pollingTrigger");
    private static final String DYNAMIC_WEBHOOK_TRIGGER_REFRESH_000001 =
        "DynamicWebhookTriggerRefresh" + nameTail("000001", "webhookTrigger");

    private final SchedulerClient schedulerClient = mock(SchedulerClient.class);

    private AwsConnectionRefreshScheduler awsConnectionRefreshScheduler;
    private AwsTriggerScheduler awsTriggerScheduler;

    @BeforeEach
    void beforeEach() {
        ApplicationProperties.Cloud.Aws aws = new ApplicationProperties.Cloud.Aws();

        aws.setAccountId("123456789012");
        aws.setRegion("us-east-1");

        awsConnectionRefreshScheduler = new AwsConnectionRefreshScheduler(aws, schedulerClient);
        awsTriggerScheduler = new AwsTriggerScheduler(
            aws, new ApplicationProperties.Coordinator.Trigger.Polling(), schedulerClient);

        Map<String, List<List<String>>> pagesByGroup = Map.of(
            "ScheduleTrigger", List.of(List.of(SCHEDULE_TRIGGER_000001), List.of(SCHEDULE_TRIGGER_000002)),
            "PollingTrigger", List.of(List.of(POLLING_TRIGGER_000002, "PollingTrigger!!!!", POLLING_TRIGGER_000001)),
            "DynamicWebhookTriggerRefresh", List.of(List.of(DYNAMIC_WEBHOOK_TRIGGER_REFRESH_000001)),
            "ConnectionRefresh", List.of(
                List.of("ConnectionRefresh_000001_5", "ConnectionRefresh_0000011_6"),
                List.of("ConnectionRefresh_000002_5", "ConnectionRefresh_000001_7")));

        when(schedulerClient.listSchedules(any(ListSchedulesRequest.class)))
            .thenAnswer(invocation -> {
                ListSchedulesRequest request = invocation.getArgument(0);

                List<List<String>> pages = pagesByGroup.getOrDefault(request.groupName(), List.of(List.of()));

                int pageIndex = request.nextToken() == null ? 0 : Integer.parseInt(request.nextToken());

                List<ScheduleSummary> scheduleSummaries = pages.get(pageIndex)
                    .stream()
                    .filter(name -> request.namePrefix() == null || name.startsWith(request.namePrefix()))
                    .map(name -> ScheduleSummary.builder()
                        .groupName(request.groupName())
                        .name(name)
                        .build())
                    .toList();

                return ListSchedulesResponse.builder()
                    .schedules(scheduleSummaries)
                    .nextToken(pageIndex + 1 < pages.size() ? String.valueOf(pageIndex + 1) : null)
                    .build();
            });
    }

    @Test
    void testCancelAllForTenantDeletesMatchingSchedules() {
        awsTriggerScheduler.cancelAllForTenant("000001");
        awsConnectionRefreshScheduler.cancelAllForTenant("000001");

        ArgumentCaptor<DeleteScheduleRequest> deleteScheduleRequestCaptor = ArgumentCaptor.forClass(
            DeleteScheduleRequest.class);

        verify(schedulerClient, atLeastOnce()).deleteSchedule(deleteScheduleRequestCaptor.capture());

        assertThat(deleteScheduleRequestCaptor.getAllValues())
            .extracting(request -> request.groupName() + "/" + request.name())
            .containsExactlyInAnyOrder(
                "ScheduleTrigger/" + SCHEDULE_TRIGGER_000001,
                "PollingTrigger/" + POLLING_TRIGGER_000001,
                "DynamicWebhookTriggerRefresh/" + DYNAMIC_WEBHOOK_TRIGGER_REFRESH_000001,
                "ConnectionRefresh/ConnectionRefresh_000001_5",
                "ConnectionRefresh/ConnectionRefresh_000001_7");
    }

    private static String nameTail(String tenantId, String triggerName) {
        String workflowExecutionId = EncodingUtils.base64EncodeToString(
            tenantId + ":0:1:" + triggerName + "Uuid:" + triggerName);

        return workflowExecutionId.substring(0, 16);
    }
}
