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

package com.bytechef.task.dispatcher.suspend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class SuspendTaskDispatcherTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final JobService jobService = mock(JobService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);

    private final SuspendTaskDispatcher suspendTaskDispatcher = new SuspendTaskDispatcher(
        eventPublisher, jobService, taskExecutionService);

    static {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        JsonUtils.setObjectMapper(objectMapper);
        MapUtils.setObjectMapper(objectMapper);
    }

    @Test
    void testDispatch() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setId(1L);
        taskExecution.setJobId(100L);

        Job job = new Job();

        job.setId(100L);

        Job stoppedJob = new Job();

        stoppedJob.setId(100L);
        stoppedJob.setStatus(Job.Status.STOPPED);

        when(taskExecutionService.update(any(TaskExecution.class)))
            .thenReturn(taskExecution);
        when(jobService.getJob(100L))
            .thenReturn(job);
        when(jobService.setStatusToStopped(100L))
            .thenReturn(stoppedJob);

        suspendTaskDispatcher.dispatch(taskExecution);

        ArgumentCaptor<TaskExecution> taskExecutionCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskExecutionService).update(taskExecutionCaptor.capture());

        TaskExecution updatedTaskExecution = taskExecutionCaptor.getValue();

        assertNotNull(updatedTaskExecution.getStartDate());
        assertEquals(TaskExecution.Status.STARTED, updatedTaskExecution.getStatus());

        verify(jobService).getJob(100L);
        verify(jobService).setStatusToStopped(100L);

        ArgumentCaptor<JobStatusApplicationEvent> eventCaptor = ArgumentCaptor.forClass(
            JobStatusApplicationEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        JobStatusApplicationEvent event = eventCaptor.getValue();

        assertNotNull(event);
    }

    @Test
    void testResolveReturnsSelfForSuspendType() {
        WorkflowTask workflowTask = new WorkflowTask(
            Map.of("name", "test", "type", "suspend/v1"));

        assertNotNull(suspendTaskDispatcher.resolve(workflowTask));
        assertEquals(suspendTaskDispatcher, suspendTaskDispatcher.resolve(workflowTask));
    }

    @Test
    void testResolveReturnsNullForOtherType() {
        WorkflowTask workflowTask = new WorkflowTask(
            Map.of("name", "test", "type", "branch/v1"));

        assertNull(suspendTaskDispatcher.resolve(workflowTask));
    }
}
