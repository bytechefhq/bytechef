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

package com.bytechef.component.delay.action;

import static com.bytechef.component.delay.constant.DelayConstants.MILLIS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class DelaySleepActionTest {

    @Test
    public void testWithDuration() throws InterruptedException {
        // Mock dependencies
        TriggerScheduler triggerScheduler = Mockito.mock(TriggerScheduler.class);
        Parameters parameters = Mockito.mock(Parameters.class);
        ActionContextAware context = Mockito.mock(ActionContextAware.class);

        // Setup parameter mocks
        Mockito.when(parameters.containsKey(eq("duration")))
            .thenReturn(true);
        Mockito.when(parameters.getDuration(eq("duration")))
            .thenReturn(Duration.of(1500, ChronoUnit.MILLIS));

        // Setup context mocks
        Mockito.when(context.getJobId())
            .thenReturn(456L);

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 123L, "test-workflow", "test-trigger");

        Mockito.when(context.getWorkflowId())
            .thenReturn(workflowExecutionId.toString());

        // Execute
        Object result = new DelaySleepAction(triggerScheduler).perform(parameters, parameters, context);

        // Verify
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof Map);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        Assertions.assertTrue(resultMap.containsKey("scheduledAt"));
        Assertions.assertTrue(resultMap.get("scheduledAt") instanceof LocalDateTime);
        Assertions.assertEquals(1500L, resultMap.get("delayMillis"));

        // Verify scheduler was called with correct parameters
        Mockito.verify(triggerScheduler)
            .scheduleOneTimeTask(any(LocalDateTime.class),
                eq(Map.of("delayMillis", 1500L)), any(WorkflowExecutionId.class), eq("456"));
    }

    @Test
    public void testWithMillis() throws InterruptedException {
        // Mock dependencies
        TriggerScheduler triggerScheduler = Mockito.mock(TriggerScheduler.class);
        Parameters parameters = Mockito.mock(Parameters.class);
        ActionContextAware context = Mockito.mock(ActionContextAware.class);

        // Setup parameter mocks
        Mockito.when(parameters.containsKey(eq(MILLIS)))
            .thenReturn(true);
        Mockito.when(parameters.getLong(eq(MILLIS)))
            .thenReturn(500L);

        // Setup context mocks
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 123L, "test-workflow", "test-trigger");
        Mockito.when(context.getJobId())
            .thenReturn(456L);
        Mockito.when(context.getWorkflowId())
            .thenReturn(workflowExecutionId.toString());

        // Execute
        Object result = new DelaySleepAction(triggerScheduler).perform(parameters, parameters, context);

        // Verify
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof Map);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        Assertions.assertTrue(resultMap.containsKey("scheduledAt"));
        Assertions.assertTrue(resultMap.get("scheduledAt") instanceof LocalDateTime);
        Assertions.assertEquals(500L, resultMap.get("delayMillis"));

        // Verify scheduler was called with correct parameters
        Mockito.verify(triggerScheduler)
            .scheduleOneTimeTask(any(LocalDateTime.class),
                eq(Map.of("delayMillis", 500L)), any(WorkflowExecutionId.class), eq("456"));
    }

    @Test
    public void testWithDefaultDelay() throws InterruptedException {
        // Mock dependencies
        TriggerScheduler triggerScheduler = Mockito.mock(TriggerScheduler.class);
        Parameters inputParameters = Mockito.mock(Parameters.class);
        Parameters connectionParameters = Mockito.mock(Parameters.class);
        ActionContextAware context = Mockito.mock(ActionContextAware.class);

        // Ensure the parameters don't contain MILLIS or duration
        Mockito.when(inputParameters.containsKey(MILLIS))
            .thenReturn(false);
        Mockito.when(inputParameters.containsKey("duration"))
            .thenReturn(false);

        // Setup context mocks
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 123L, "test-workflow", "test-trigger");
        Mockito.when(context.getJobId())
            .thenReturn(456L);
        Mockito.when(context.getWorkflowId())
            .thenReturn(workflowExecutionId.toString());

        // Execute
        Object result = new DelaySleepAction(triggerScheduler).perform(inputParameters, connectionParameters, context);

        // Verify
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof Map);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        Assertions.assertTrue(resultMap.containsKey("scheduledAt"));
        Assertions.assertTrue(resultMap.get("scheduledAt") instanceof LocalDateTime);
        Assertions.assertEquals(1000L, resultMap.get("delayMillis")); // Default delay is 1000ms

        // Verify scheduler was called with correct parameters
        Mockito.verify(triggerScheduler)
            .scheduleOneTimeTask(any(LocalDateTime.class),
                eq(Map.of("delayMillis", 1000L)), any(WorkflowExecutionId.class), eq("456"));
    }

    @Test
    public void testWithNullScheduler() throws InterruptedException {
        // Test that when TriggerScheduler is null, it throws a NullPointerException
        Parameters parameters = Mockito.mock(Parameters.class);
        ActionContextAware contextAware = Mockito.mock(ActionContextAware.class);

        Mockito.when(parameters.containsKey(eq(MILLIS)))
            .thenReturn(true);
        Mockito.when(parameters.getLong(eq(MILLIS)))
            .thenReturn(200L);

        // Setup context mocks
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            ModeType.AUTOMATION, 123L, "test-workflow", "test-trigger");
        Mockito.when(contextAware.getJobId())
            .thenReturn(456L);
        Mockito.when(contextAware.getWorkflowId())
            .thenReturn(workflowExecutionId.toString());

        // TriggerScheduler is null, should throw NullPointerException
        Assertions.assertThrows(NullPointerException.class, () -> {
            new DelaySleepAction(null).perform(parameters, parameters, contextAware);
        });
    }
}
