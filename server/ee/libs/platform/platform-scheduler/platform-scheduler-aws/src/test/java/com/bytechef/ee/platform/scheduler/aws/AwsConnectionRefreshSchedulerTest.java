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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest.Builder;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleResponse;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleResponse;
import software.amazon.awssdk.services.scheduler.model.UpdateScheduleResponse;

/**
 * @author Nikolina Spehar
 */
class AwsConnectionRefreshSchedulerTest {

    private static final String TEST_ACCOUNT_ID = "123456789012";
    private static final String TEST_REGION = "us-east-1";

    private SchedulerClient mockSchedulerClient;
    private AwsConnectionRefreshScheduler awsConnectionRefreshScheduler;
    private MockedStatic<JsonUtils> jsonUtilsMock;

    @BeforeEach
    void beforeEach() {
//        jsonUtilsMock = mockStatic(JsonUtils.class);
//
//        jsonUtilsMock.when(() -> JsonUtils.write(any()))
//            .thenReturn("{\"mocked\":\"json\"}");
//
//        ApplicationProperties.Cloud.Aws awsProperties = new ApplicationProperties.Cloud.Aws();
//
//        awsProperties.setAccountId(TEST_ACCOUNT_ID);
//        awsProperties.setRegion(TEST_REGION);
//
//        mockSchedulerClient = mock(SchedulerClient.class);
//
//        awsConnectionRefreshScheduler = new AwsConnectionRefreshScheduler(awsProperties, mockSchedulerClient);
//
//        when(mockSchedulerClient.createSchedule(any(Consumer.class)))
//            .thenReturn(CreateScheduleResponse.builder()
//                .build());
//        when(mockSchedulerClient.deleteSchedule(any(Consumer.class)))
//            .thenReturn(DeleteScheduleResponse.builder()
//                .build());
//        when(mockSchedulerClient.updateSchedule(any(Consumer.class)))
//            .thenReturn(UpdateScheduleResponse.builder()
//                .build());
    }

    @AfterEach
    void tearDown() {
        if (jsonUtilsMock != null) {
            jsonUtilsMock.close();
        }
    }

    @Test
    void testCancelConnectionRefresh() {
        // Given
        Long connectionId = 456L;
        String tenantId = "tenantId";

        // When & Then - should not throw exception (exceptions are caught and logged)
        assertDoesNotThrow(
            () -> awsConnectionRefreshScheduler.cancelConnectionRefresh(connectionId, tenantId));

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
    void testScheduleConnectionRefresh() {
        // Given
        Instant connectionExpiry = LocalDateTime.now()
            .plusHours(1)
            .toInstant(ZoneOffset.UTC);
        Long connectionId = 456L;
        String tenantId = "tenantId";

        // When
        assertDoesNotThrow(() -> awsConnectionRefreshScheduler.scheduleConnectionRefresh(
            connectionId, connectionExpiry, tenantId));

        // Then
        ArgumentCaptor<Consumer<Builder>> captor = ArgumentCaptor.forClass(Consumer.class);

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
}
