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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class SuspendTaskDispatcherPreSendProcessorTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final JobService jobService = mock(JobService.class);
    private final TaskStateService taskStateService = mock(TaskStateService.class);
    private final TriggerScheduler triggerScheduler = mock(TriggerScheduler.class);

    private final SuspendTaskDispatcherPreSendProcessor preSendProcessor =
        new SuspendTaskDispatcherPreSendProcessor(eventPublisher, jobService, taskStateService, triggerScheduler);

    static {
        MapUtils.setObjectMapper(
            JsonMapper.builder()
                .build());
    }

    @Test
    void testCanProcessReturnsTrueWhenJobHasResumeId() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        Job job = new Job();

        job.setId(100L);

        String jobResumeIdString = createJobResumeIdString(100L);

        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, jobResumeIdString));

        when(jobService.getJob(100L))
            .thenReturn(job);

        assertTrue(preSendProcessor.canProcess(taskExecution));
    }

    @Test
    void testCanProcessReturnsFalseWhenJobHasNoResumeId() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        Job job = new Job();

        job.setId(100L);
        job.setMetadata(Map.of());

        when(jobService.getJob(100L))
            .thenReturn(job);

        assertFalse(preSendProcessor.canProcess(taskExecution));
    }

    @Test
    void testCanProcessReturnsTrueForTheSuspendedTaskWhileTheJobIsStopped() {
        assertTrue(preSendProcessor.canProcess(createTaskExecutionOfStoppedJob(Job.Status.STOPPED, 7L)));
    }

    @Test
    void testCanProcessReturnsFalseForASiblingTaskWhileTheJobIsStopped() {
        assertFalse(preSendProcessor.canProcess(createTaskExecutionOfStoppedJob(Job.Status.STOPPED, 8L)));
    }

    @Test
    void testCanProcessReturnsTrueForTheSuspendedTaskOnceItsResumeClaimedTheJob() {
        assertTrue(preSendProcessor.canProcess(createTaskExecutionOfStoppedJob(Job.Status.STARTED, 7L)));
    }

    @Test
    void testCanProcessReturnsFalseForASiblingTaskOnceTheResumeClaimedTheJob() {
        assertFalse(preSendProcessor.canProcess(createTaskExecutionOfStoppedJob(Job.Status.STARTED, 8L)));
    }

    @Test
    void testProcessRecoversSuspendStateAndCleansUp() {
        String jobResumeIdString = createJobResumeIdString(100L);
        Suspend suspend = new Suspend(Map.of("key", "value"), null);

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        Job job = new Job();

        job.setId(100L);
        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, jobResumeIdString));

        when(jobService.getJob(100L))
            .thenReturn(job);
        when(taskStateService.fetchValue(any(JobResumeId.class)))
            .thenReturn(Optional.of(suspend));

        TaskExecution result = preSendProcessor.process(taskExecution);

        assertNotNull(result);
        assertNotNull(result.getMetadata()
            .get(MetadataConstants.SUSPEND));

        verify(jobService).update(any(Job.class));
        verify(taskStateService).delete(any(JobResumeId.class));
    }

    @Test
    void testProcessStopsTheJobOnTheQueuedSuspendOnceTheSuspendItWaitedOnResumes() {
        String firstJobResumeIdString = createJobResumeIdString(100L);
        String secondJobResumeIdString = createJobResumeIdString(100L);

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setId(7L);
        taskExecution.setJobId(100L);

        Job job = new Job();

        job.setId(100L);
        job.setMetadata(
            Map.of(
                MetadataConstants.JOB_RESUME_ID, firstJobResumeIdString,
                MetadataConstants.TASK_EXECUTION_RESUME_ID, 7L,
                MetadataConstants.RESUME_DATA, Map.of("approved", true),
                MetadataConstants.QUEUED_SUSPENDS, List.of(
                    Map.of(
                        MetadataConstants.JOB_RESUME_ID, secondJobResumeIdString,
                        MetadataConstants.TASK_EXECUTION_RESUME_ID, 8))));
        job.setStatus(Job.Status.STARTED);

        when(jobService.getJob(100L))
            .thenReturn(job);
        when(taskStateService.fetchValue(JobResumeId.parse(firstJobResumeIdString)))
            .thenReturn(Optional.of(new Suspend(Map.of(), null)));
        when(taskStateService.fetchValue(JobResumeId.parse(secondJobResumeIdString)))
            .thenReturn(Optional.of(new Suspend(Map.of(MetadataConstants.APPROVAL_RESUME, true), null)));

        TaskExecution result = preSendProcessor.process(taskExecution);

        assertEquals(Map.of("approved", true), result.getMetadata()
            .get(MetadataConstants.RESUME_DATA));
        assertEquals(secondJobResumeIdString, job.getMetadata(MetadataConstants.JOB_RESUME_ID));
        assertEquals(8L, job.getMetadata(MetadataConstants.TASK_EXECUTION_RESUME_ID));
        assertEquals(true, job.getMetadata(MetadataConstants.APPROVAL_RESUME));
        assertNull(job.getMetadata(MetadataConstants.QUEUED_SUSPENDS));

        verify(taskStateService).delete(JobResumeId.parse(firstJobResumeIdString));
        verify(taskStateService, never()).delete(JobResumeId.parse(secondJobResumeIdString));
        verify(jobService).setStatusToStopped(100L);

        ArgumentCaptor<JobStatusApplicationEvent> eventCaptor = ArgumentCaptor.forClass(
            JobStatusApplicationEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertTrue(eventCaptor.getValue()
            .isSuspended());
    }

    @Test
    void testProcessLeavesTheJobRunningWhenNoSuspendIsQueued() {
        processSuspend(new Suspend(Map.of(), null));

        verify(jobService, never()).setStatusToStopped(anyLong());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testProcessCancelsTheTimeoutTaskOfASuspendWithADeadline() {
        processSuspend(new Suspend(Map.of(), Instant.now()
            .plusSeconds(3600)));

        verify(triggerScheduler).cancelOneTimeTask(100L);
    }

    @Test
    void testProcessDoesNotCancelATimeoutTaskForASuspendWithoutADeadline() {
        processSuspend(new Suspend(Map.of(), null));

        verify(triggerScheduler, never()).cancelOneTimeTask(anyLong());
    }

    @Test
    void testProcessResumesTheTaskWhenCancellingTheTimeoutTaskFails() {
        doThrow(new IllegalStateException("Scheduler unavailable")).when(triggerScheduler)
            .cancelOneTimeTask(100L);

        TaskExecution result = processSuspend(new Suspend(Map.of(), Instant.now()
            .plusSeconds(3600)));

        assertNotNull(result.getMetadata()
            .get(MetadataConstants.SUSPEND));

        verify(taskStateService).delete(any(JobResumeId.class));
    }

    @Test
    void testProcessResumesTheTaskWhenTheTriggerSchedulerCannotCancelTheTimeoutTask() {
        doThrow(new UnsupportedOperationException()).when(triggerScheduler)
            .cancelOneTimeTask(100L);

        TaskExecution result = processSuspend(new Suspend(Map.of(), Instant.now()
            .plusSeconds(3600)));

        assertNotNull(result.getMetadata()
            .get(MetadataConstants.SUSPEND));

        verify(taskStateService).delete(any(JobResumeId.class));
    }

    @Test
    void testProcessWithoutATriggerSchedulerStillResumesTheTask() {
        SuspendTaskDispatcherPreSendProcessor preSendProcessorWithoutTriggerScheduler =
            new SuspendTaskDispatcherPreSendProcessor(eventPublisher, jobService, taskStateService, null);

        TaskExecution taskExecution = createSuspendedTaskExecution(new Suspend(Map.of(), Instant.now()
            .plusSeconds(3600)));

        TaskExecution result = preSendProcessorWithoutTriggerScheduler.process(taskExecution);

        assertNotNull(result.getMetadata()
            .get(MetadataConstants.SUSPEND));
    }

    @Test
    void testProcessHandlesMissingSuspendStateAndCancelsTheTimeoutTask() {
        String jobResumeIdString = createJobResumeIdString(100L);

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        Job job = new Job();

        job.setId(100L);
        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, jobResumeIdString));

        when(jobService.getJob(100L))
            .thenReturn(job);
        when(taskStateService.fetchValue(any(JobResumeId.class)))
            .thenReturn(Optional.empty());

        TaskExecution result = preSendProcessor.process(taskExecution);

        assertNotNull(result);
        assertFalse(result.getMetadata()
            .containsKey(MetadataConstants.SUSPEND));

        verify(jobService).update(any(Job.class));
        verify(taskStateService).delete(any(JobResumeId.class));
        verify(triggerScheduler).cancelOneTimeTask(100L);
    }

    private TaskExecution createTaskExecutionOfStoppedJob(Job.Status jobStatus, long taskExecutionId) {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setId(taskExecutionId);
        taskExecution.setJobId(100L);

        Job job = new Job();

        job.setId(100L);
        job.setMetadata(
            Map.of(
                MetadataConstants.JOB_RESUME_ID, createJobResumeIdString(100L),
                MetadataConstants.TASK_EXECUTION_RESUME_ID, 7L));
        job.setStatus(jobStatus);

        when(jobService.getJob(100L))
            .thenReturn(job);

        return taskExecution;
    }

    private static String createJobResumeIdString(long jobId) {
        return EncodingUtils.base64EncodeToString("public:" + jobId + ":" + UUID.randomUUID());
    }

    private TaskExecution processSuspend(Suspend suspend) {
        return preSendProcessor.process(createSuspendedTaskExecution(suspend));
    }

    private TaskExecution createSuspendedTaskExecution(Suspend suspend) {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        Job job = new Job();

        job.setId(100L);
        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, createJobResumeIdString(100L)));

        when(jobService.getJob(100L))
            .thenReturn(job);
        when(taskStateService.fetchValue(any(JobResumeId.class)))
            .thenReturn(Optional.of(suspend));

        return taskExecution;
    }
}
