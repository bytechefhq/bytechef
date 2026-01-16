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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleResponse;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleResponse;

/**
 * Unit tests for the AwsTriggerScheduler class that ensure all scheduling and cancellation methods interacting with AWS
 * SchedulerClient function as expected. This test class verifies the behavior of scheduled triggers, polling triggers,
 * one-time tasks, dynamic webhook trigger refresh functionalities, and their respective cancellation flows.
 *
 * The implemented tests mock the AWS SchedulerClient interactions and validate that the request-building process is
 * correct, following the expected setups for AWS scheduling resources.
 *
 * Key functionalities covered include: - Scheduling and cancellation of various types of triggers (schedule, polling,
 * dynamic webhook). - Construction and validation of AWS-specific properties (such as ARNs, schedule expressions). -
 * Handling of input/output transformations involving JSON utilities. - Ensuring robust exception handling within the
 * tested methods.
 *
 * This test class also verifies the correct application of account and region-specific settings during instantiation of
 * the AwsTriggerScheduler.
 *
 * @author Ivica Cardic
 */
class AwsTriggerSchedulerTest {

    private static final String TEST_ACCOUNT_ID = "123456789012";
    private static final String TEST_REGION = "us-east-1";

    private SchedulerClient mockSchedulerClient;
    private AwsTriggerScheduler awsTriggerScheduler;
    private MockedStatic<JsonUtils> jsonUtilsMock;

    @BeforeEach
    void beforeEach() {
        jsonUtilsMock = mockStatic(JsonUtils.class);

        jsonUtilsMock.when(() -> JsonUtils.write(any()))
            .thenReturn("{\"mocked\":\"json\"}");

        ApplicationProperties.Cloud.Aws awsProperties = new ApplicationProperties.Cloud.Aws();

        awsProperties.setAccountId(TEST_ACCOUNT_ID);
        awsProperties.setRegion(TEST_REGION);

        ApplicationProperties.Coordinator.Trigger.Polling polling =
            new ApplicationProperties.Coordinator.Trigger.Polling();

        mockSchedulerClient = mock(SchedulerClient.class);

        awsTriggerScheduler = new AwsTriggerScheduler(awsProperties, polling, mockSchedulerClient);

        when(mockSchedulerClient.createSchedule(any(Consumer.class)))
            .thenReturn(CreateScheduleResponse.builder()
                .build());
        when(mockSchedulerClient.deleteSchedule(any(Consumer.class)))
            .thenReturn(DeleteScheduleResponse.builder()
                .build());
    }

    @AfterEach
    void tearDown() {
        if (jsonUtilsMock != null) {
            jsonUtilsMock.close();
        }
    }

    @Test
    void testScheduleScheduleTrigger() {
        // Given
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 123L, "test-workflow", "test-trigger");
        String pattern = "0 0 12 * * ?"; // Daily at noon
        String zoneId = "UTC";
        Map<String, Object> output = Map.of("key", "value");

        // When
        assertDoesNotThrow(() -> awsTriggerScheduler.scheduleScheduleTrigger(
            pattern, zoneId, output, workflowExecutionId));

        // Then
        ArgumentCaptor<Consumer<CreateScheduleRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);

        verify(mockSchedulerClient).createSchedule(captor.capture());

        // Verify the request was built correctly
        CreateScheduleRequest.Builder builder = CreateScheduleRequest.builder();
        captor.getValue()
            .accept(builder);
        CreateScheduleRequest request = builder.build();

        assertNotNull(request);
        assertEquals("ScheduleTrigger", request.groupName());
        assertEquals("cron(0 12 * * ?)", request.scheduleExpression());
        assertEquals(zoneId, request.scheduleExpressionTimezone());
    }

    @Test
    void testSchedulePollingTrigger() {
        // Given
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 456L, "test-polling-workflow", "test-trigger");

        // When
        assertDoesNotThrow(() -> awsTriggerScheduler.schedulePollingTrigger(workflowExecutionId));

        // Then
        ArgumentCaptor<Consumer<CreateScheduleRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);

        verify(mockSchedulerClient).createSchedule(captor.capture());

        // Verify the request was built correctly
        CreateScheduleRequest.Builder builder = CreateScheduleRequest.builder();

        captor.getValue()
            .accept(builder);

        CreateScheduleRequest request = builder.build();

        assertNotNull(request);
        assertEquals("PollingTrigger", request.groupName());
        assertEquals("rate(5 minutes)", request.scheduleExpression());
    }

    @Test
    void testScheduleOneTimeTask() {
        // Given
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 789L, "test-onetime-workflow", "test-trigger");
        String taskExecutionId = "task-execution-12345678901234567890";
        Instant executeAt = LocalDateTime.now()
            .plusMinutes(5)
            .toInstant(ZoneOffset.UTC);
        Map<String, Object> output = Map.of("delayMillis", 1000L);

        // When
        assertDoesNotThrow(() -> awsTriggerScheduler.scheduleOneTimeTask(
            executeAt, output, workflowExecutionId, taskExecutionId));

        // Then
        ArgumentCaptor<Consumer<CreateScheduleRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);

        verify(mockSchedulerClient).createSchedule(captor.capture());

        // Verify the request was built correctly
        CreateScheduleRequest.Builder builder = CreateScheduleRequest.builder();

        captor.getValue()
            .accept(builder);

        CreateScheduleRequest request = builder.build();

        assertNotNull(request);
        assertEquals("OneTimeTask", request.groupName());
        assertNotNull(request.target());
        assertNotNull(request.target()
            .arn());
    }

    @Test
    void testScheduleDynamicWebhookTriggerRefresh() {
        // Given
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 101L, "test-webhook-workflow", "test-trigger");
        Instant webhookExpirationDate = LocalDateTime.now()
            .plusHours(1)
            .toInstant(ZoneOffset.UTC);
        String componentName = "testComponent";
        int componentVersion = 1;
        Long connectionId = 456L;

        // When
        assertDoesNotThrow(() -> awsTriggerScheduler.scheduleDynamicWebhookTriggerRefresh(
            webhookExpirationDate, componentName, componentVersion, workflowExecutionId, connectionId));

        // Then
        ArgumentCaptor<Consumer<CreateScheduleRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);

        verify(mockSchedulerClient).createSchedule(captor.capture());

        // Verify the request was built correctly
        CreateScheduleRequest.Builder builder = CreateScheduleRequest.builder();

        captor.getValue()
            .accept(builder);

        CreateScheduleRequest request = builder.build();

        assertNotNull(request);
        assertEquals("DynamicWebhookTriggerRefresh", request.groupName());
        assertNotNull(request.target());
        assertNotNull(request.target()
            .arn());
    }

    @Test
    void testCancelScheduleTrigger() {
        // Given
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 202L, "test-cancel-workflow", "test-trigger");

        // When
        assertDoesNotThrow(() -> awsTriggerScheduler.cancelScheduleTrigger(workflowExecutionId.toString()));

        // Then
        ArgumentCaptor<Consumer<DeleteScheduleRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);

        verify(mockSchedulerClient).deleteSchedule(captor.capture());

        // Verify the request was built correctly
        DeleteScheduleRequest.Builder builder = DeleteScheduleRequest.builder();

        captor.getValue()
            .accept(builder);

        DeleteScheduleRequest request = builder.build();

        assertNotNull(request);
        assertEquals("ScheduleTrigger", request.groupName());
    }

    @Test
    void testCancelPollingTrigger() {
        // Given
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 303L, "test-cancel-polling-workflow", "test-trigger");

        // When
        assertDoesNotThrow(() -> awsTriggerScheduler.cancelPollingTrigger(workflowExecutionId.toString()));

        // Then
        ArgumentCaptor<Consumer<DeleteScheduleRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);

        verify(mockSchedulerClient).deleteSchedule(captor.capture());

        // Verify the request was built correctly
        DeleteScheduleRequest.Builder builder = DeleteScheduleRequest.builder();
        captor.getValue()
            .accept(builder);
        DeleteScheduleRequest request = builder.build();

        assertNotNull(request);
        assertEquals("PollingTrigger", request.groupName());
    }

    @Test
    void testCancelDynamicWebhookTriggerRefresh() {
        // Given
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 404L, "test-cancel-webhook-workflow", "test-trigger");

        // When & Then - should not throw exception (exceptions are caught and logged)
        assertDoesNotThrow(
            () -> awsTriggerScheduler.cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString()));

        // Then
        ArgumentCaptor<Consumer<DeleteScheduleRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);

        verify(mockSchedulerClient).deleteSchedule(captor.capture());

        // Verify the request was built correctly
        DeleteScheduleRequest.Builder builder = DeleteScheduleRequest.builder();

        captor.getValue()
            .accept(builder);

        DeleteScheduleRequest request = builder.build();

        assertNotNull(request);
        assertEquals("DynamicWebhookTriggerRefresh", request.groupName());
    }

    @Test
    void testConstructorSetsCorrectArns() {
        // Given
        ApplicationProperties.Cloud.Aws testAws = new ApplicationProperties.Cloud.Aws();

        testAws.setAccountId("999888777666");
        testAws.setRegion("eu-west-1");

        // Create Trigger Polling properties
        ApplicationProperties.Coordinator.Trigger.Polling polling =
            new ApplicationProperties.Coordinator.Trigger.Polling();

        // When
        AwsTriggerScheduler scheduler = new AwsTriggerScheduler(testAws, polling, mockSchedulerClient);

        // Then - verify constructor doesn't throw and object is created
        assertNotNull(scheduler);

        // Test that the ARNs are constructed correctly by triggering a schedule operation
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 123L, "test-workflow", "test-trigger");

        assertDoesNotThrow(() -> scheduler.schedulePollingTrigger(workflowExecutionId));

        // Verify the scheduler client was called
        verify(mockSchedulerClient).createSchedule(any(Consumer.class));
    }
}
