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

package com.bytechef.atlas.coordinator.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.SuspendedJobs;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TaskStartedApplicationEventListenerTest {

    private static final long JOB_ID = 4567L;
    private static final long TASK_EXECUTION_ID = 12L;

    private final JobService jobService = mock(JobService.class);
    @SuppressWarnings("unchecked")
    private final TaskDispatcher<? super Task> taskDispatcher = mock(TaskDispatcher.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TaskStartedApplicationEventListener taskStartedApplicationEventListener =
        new TaskStartedApplicationEventListener(taskExecutionService, taskDispatcher, jobService);

    @Test
    void testTaskOfStartedJobIsMarkedStarted() {
        TaskExecution taskExecution = startTask(TaskExecution.Status.CREATED, Job.Status.STARTED, Map.of());

        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.STARTED);

        verify(taskDispatcher, never()).dispatch(any());
    }

    @Test
    void testTaskOfSuspendedJobKeepsRunning() {
        TaskExecution taskExecution = startTask(
            TaskExecution.Status.CREATED, Job.Status.STOPPED, Map.of(SuspendedJobs.JOB_RESUME_ID, "resume-id"));

        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.STARTED);

        verify(taskDispatcher, never()).dispatch(any());
    }

    @Test
    void testTaskOfStoppedJobIsCancelled() {
        startTask(TaskExecution.Status.CREATED, Job.Status.STOPPED, Map.of());

        verify(taskDispatcher).dispatch(isA(CancelControlTask.class));
    }

    @Test
    void testCancelledTaskOfSuspendedJobIsCancelled() {
        startTask(
            TaskExecution.Status.CANCELLED, Job.Status.STOPPED, Map.of(SuspendedJobs.JOB_RESUME_ID, "resume-id"));

        verify(taskDispatcher).dispatch(isA(CancelControlTask.class));
    }

    private TaskExecution startTask(
        TaskExecution.Status taskExecutionStatus, Job.Status jobStatus, Map<String, Object> jobMetadata) {

        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(TASK_EXECUTION_ID);
        taskExecution.setJobId(JOB_ID);
        taskExecution.setStatus(taskExecutionStatus);

        Job job = new Job(JOB_ID);

        job.setMetadata(jobMetadata);
        job.setStatus(jobStatus);

        when(taskExecutionService.getTaskExecution(TASK_EXECUTION_ID)).thenReturn(taskExecution);
        when(taskExecutionService.update(any(TaskExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobService.getTaskExecutionJob(TASK_EXECUTION_ID)).thenReturn(job);

        taskStartedApplicationEventListener.onApplicationEvent(new TaskStartedApplicationEvent(TASK_EXECUTION_ID));

        return taskExecution;
    }
}
