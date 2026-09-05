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

package com.bytechef.task.dispatcher.subflow.event.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.StopJobEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class SubflowJobStatusEventListenerTest {

    private static final long CHILD_JOB_ID = 200L;
    private static final long PARENT_JOB_ID = 100L;
    private static final long PARENT_TASK_EXECUTION_ID = 10L;

    @Mock
    private Evaluator evaluator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private JobService jobService;

    @Mock
    private TaskExecutionService taskExecutionService;

    @Mock
    private TaskFileStorage taskFileStorage;

    @Test
    void testSuspendedChildLeavesParentRunning() {
        Job childJob = newChildJob(Job.Status.STOPPED);

        childJob.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1"));

        when(jobService.getJob(CHILD_JOB_ID)).thenReturn(childJob);

        newListener().onApplicationEvent(JobStatusApplicationEvent.suspended(CHILD_JOB_ID));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testSuspendedChildStoppedByHandStopsParent() {
        Job childJob = newChildJob(Job.Status.STOPPED);

        childJob.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1"));

        when(jobService.getJob(CHILD_JOB_ID)).thenReturn(childJob);
        when(taskExecutionService.getTaskExecution(PARENT_TASK_EXECUTION_ID)).thenReturn(newParentTaskExecution());

        newListener().onApplicationEvent(new JobStatusApplicationEvent(CHILD_JOB_ID, Job.Status.STOPPED));

        assertEquals(PARENT_JOB_ID, capturePublishedStopJobEvent().getJobId());
    }

    @Test
    void testStoppedChildStopsParent() {
        Job childJob = newChildJob(Job.Status.STOPPED);

        when(jobService.getJob(CHILD_JOB_ID)).thenReturn(childJob);
        when(taskExecutionService.getTaskExecution(PARENT_TASK_EXECUTION_ID)).thenReturn(newParentTaskExecution());

        newListener().onApplicationEvent(new JobStatusApplicationEvent(CHILD_JOB_ID, Job.Status.STOPPED));

        assertEquals(PARENT_JOB_ID, capturePublishedStopJobEvent().getJobId());
    }

    private StopJobEvent capturePublishedStopJobEvent() {
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        return assertInstanceOf(StopJobEvent.class, eventCaptor.getValue());
    }

    private SubflowJobStatusEventListener newListener() {
        return new SubflowJobStatusEventListener(
            evaluator, eventPublisher, jobService, taskExecutionService, taskFileStorage);
    }

    private static Job newChildJob(Job.Status status) {
        Job job = new Job();

        job.setId(CHILD_JOB_ID);
        job.setParentTaskExecutionId(PARENT_TASK_EXECUTION_ID);
        job.setStatus(status);

        return job;
    }

    private static TaskExecution newParentTaskExecution() {
        return TaskExecution.builder()
            .jobId(PARENT_JOB_ID)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "callChild", WorkflowConstants.TYPE, "subflow/v1")))
            .build();
    }
}
