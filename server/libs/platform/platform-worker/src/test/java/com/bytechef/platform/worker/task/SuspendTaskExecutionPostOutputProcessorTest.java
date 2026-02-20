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

package com.bytechef.platform.worker.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.component.definition.ActionDefinition.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.tenant.TenantContext;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SuspendTaskExecutionPostOutputProcessorTest {

    private final TriggerScheduler triggerScheduler = mock(TriggerScheduler.class);

    @Test
    void testProcessWithSuspendAndExpiresAtSchedulesTask() {
        TenantContext.setCurrentTenantId("public");

        SuspendTaskExecutionPostOutputProcessor processor = new SuspendTaskExecutionPostOutputProcessor(
            triggerScheduler);

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        Instant expiresAt = Instant.now()
            .plusSeconds(3600);
        Map<String, Object> continueParameters = Map.of("param1", "value1");
        Suspend suspend = new Suspend(continueParameters, expiresAt);

        Object result = processor.process(taskExecution, suspend);

        assertSame(suspend, result);
        assertTrue(taskExecution.getMetadata()
            .containsKey(MetadataConstants.JOB_RESUME_ID));
        assertTrue(taskExecution.getMetadata()
            .containsKey(MetadataConstants.SUSPEND));

        verify(triggerScheduler).scheduleOneTimeTask(any(Instant.class), anyMap(), anyLong());
    }

    @Test
    void testProcessWithSuspendNoExpiresAtDoesNotSchedule() {
        TenantContext.setCurrentTenantId("public");

        SuspendTaskExecutionPostOutputProcessor processor = new SuspendTaskExecutionPostOutputProcessor(
            triggerScheduler);

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        Suspend suspend = new Suspend(Map.of(), null);

        Object result = processor.process(taskExecution, suspend);

        assertSame(suspend, result);
        assertTrue(taskExecution.getMetadata()
            .containsKey(MetadataConstants.JOB_RESUME_ID));
        assertTrue(taskExecution.getMetadata()
            .containsKey(MetadataConstants.SUSPEND));

        verify(triggerScheduler, never()).scheduleOneTimeTask(any(Instant.class), anyMap(), anyLong());
    }

    @Test
    void testProcessWithNullSchedulerDoesNotThrow() {
        TenantContext.setCurrentTenantId("public");

        SuspendTaskExecutionPostOutputProcessor processor = new SuspendTaskExecutionPostOutputProcessor(null);

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        Instant expiresAt = Instant.now()
            .plusSeconds(3600);
        Suspend suspend = new Suspend(Map.of(), expiresAt);

        Object result = processor.process(taskExecution, suspend);

        assertSame(suspend, result);
        assertNotNull(taskExecution.getMetadata()
            .get(MetadataConstants.JOB_RESUME_ID));
    }

    @Test
    void testProcessWithNonSuspendOutputPassesThrough() {
        SuspendTaskExecutionPostOutputProcessor processor = new SuspendTaskExecutionPostOutputProcessor(
            triggerScheduler);

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        String regularOutput = "hello";

        Object result = processor.process(taskExecution, regularOutput);

        assertSame(regularOutput, result);
        assertFalse(taskExecution.getMetadata()
            .containsKey(MetadataConstants.JOB_RESUME_ID));

        verify(triggerScheduler, never()).scheduleOneTimeTask(any(Instant.class), anyMap(), anyLong());
    }

    @Test
    void testProcessWithMapOutputPassesThrough() {
        SuspendTaskExecutionPostOutputProcessor processor = new SuspendTaskExecutionPostOutputProcessor(
            triggerScheduler);

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        Map<String, Object> mapOutput = Map.of("result", "data");

        Object result = processor.process(taskExecution, mapOutput);

        assertEquals(mapOutput, result);
        assertFalse(taskExecution.getMetadata()
            .containsKey(MetadataConstants.SUSPEND));
    }
}
