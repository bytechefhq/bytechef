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

package com.bytechef.platform.coordinator.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.error.ExecutionError;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class SseStreamApplicationEventListenerTest {

    private static final long JOB_ID = 7L;

    private final JobService jobService = mock(JobService.class);
    private final MessageBroker messageBroker = mock(MessageBroker.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TaskFileStorage taskFileStorage = mock(TaskFileStorage.class);
    private final SseStreamApplicationEventListener sseStreamApplicationEventListener =
        new SseStreamApplicationEventListener(jobService, messageBroker, taskExecutionService, taskFileStorage);

    @Test
    void testFailedJobStatusEventCarriesTheFailedTaskErrorMessage() {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(JOB_ID)
            .build();

        taskExecution.setStatus(TaskExecution.Status.FAILED);
        taskExecution.setError(new ExecutionError("Model call failed", List.of()));

        when(taskExecutionService.getJobTaskExecutions(JOB_ID)).thenReturn(List.of(taskExecution));

        sseStreamApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.FAILED));

        SseStreamEvent sseStreamEvent = captureSentEvent();

        assertThat(sseStreamEvent.getPayload()).isEqualTo("FAILED");
        assertThat(sseStreamEvent.getMetadata(SseStreamEvent.METADATA_ERROR_MESSAGE)).isEqualTo("Model call failed");
    }

    @Test
    void testFailedJobStatusEventIsSentWhenTheTaskLookupFails() {
        when(taskExecutionService.getJobTaskExecutions(anyLong())).thenThrow(new IllegalStateException("DB down"));

        sseStreamApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.FAILED));

        SseStreamEvent sseStreamEvent = captureSentEvent();

        assertThat(sseStreamEvent.getPayload()).isEqualTo("FAILED");
        assertThat(sseStreamEvent.getMetadata(SseStreamEvent.METADATA_ERROR_MESSAGE)).isNull();
    }

    @Test
    void testCompletedJobWithoutAWebhookResponseSendsOnlyTheStatusEvent() {
        sseStreamApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        assertThat(captureSentEvent().getPayload()).isEqualTo("COMPLETED");
    }

    @Test
    void testCompletedJobSendsTheLastWebhookResponseAsAResultEventBeforeTheStatusEvent() {
        FileEntry earlierOutput = new FileEntry("earlier.json", "memory://earlier");
        FileEntry laterOutput = new FileEntry("later.json", "memory://later");

        when(taskExecutionService.getJobTaskExecutions(JOB_ID)).thenReturn(
            List.of(
                webhookResponseTaskExecution(laterOutput, Instant.parse("2026-10-17T10:00:02Z")),
                webhookResponseTaskExecution(earlierOutput, Instant.parse("2026-10-17T10:00:01Z"))));
        when(taskFileStorage.readTaskExecutionOutput(earlierOutput)).thenReturn(
            Map.of("body", Map.of("message", "earlier reply")));
        when(taskFileStorage.readTaskExecutionOutput(laterOutput)).thenReturn(
            Map.of("body", Map.of("message", "final reply")));

        sseStreamApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        List<SseStreamEvent> sseStreamEvents = captureSentEvents(2);

        assertThat(sseStreamEvents.getFirst()
            .getEventType()).isEqualTo(SseStreamEvent.EVENT_TYPE_DATA);
        assertThat(sseStreamEvents.getFirst()
            .getPayload()).isEqualTo(Map.of("event", "result", "result", Map.of("message", "final reply")));
        assertThat(sseStreamEvents.getLast()
            .getPayload()).isEqualTo("COMPLETED");
    }

    @Test
    void testCompletedJobSendsARawTextWebhookResponseAsTheResultMessage() {
        FileEntry output = new FileEntry("reply.json", "memory://reply");

        when(taskExecutionService.getJobTaskExecutions(JOB_ID)).thenReturn(
            List.of(webhookResponseTaskExecution(output, Instant.parse("2026-10-17T10:00:01Z"))));
        when(taskFileStorage.readTaskExecutionOutput(output)).thenReturn(Map.of("body", "plain reply"));

        sseStreamApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        assertThat(captureSentEvents(2).getFirst()
            .getPayload()).isEqualTo(Map.of("event", "result", "result", Map.of("message", "plain reply")));
    }

    @Test
    void testCompletedJobStatusEventIsSentWhenTheWebhookResponseLookupFails() {
        when(taskExecutionService.getJobTaskExecutions(anyLong())).thenThrow(new IllegalStateException("DB down"));

        sseStreamApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        assertThat(captureSentEvent().getPayload()).isEqualTo("COMPLETED");
    }

    @Test
    void testStoppedJobStatusEventDoesNotLookUpTheWebhookResponse() {
        sseStreamApplicationEventListener.onApplicationEvent(JobStatusApplicationEvent.suspended(JOB_ID));

        verify(taskExecutionService, never()).getJobTaskExecutions(anyLong());
    }

    @Test
    void testStoppedJobStatusEventOfASuspendedJobIsMarkedSuspended() {
        sseStreamApplicationEventListener.onApplicationEvent(
            JobStatusApplicationEvent.suspended(JOB_ID));

        SseStreamEvent sseStreamEvent = captureSentEvent();

        assertThat(sseStreamEvent.getPayload()).isEqualTo("STOPPED");
        assertThat(sseStreamEvent.getMetadata(SseStreamEvent.METADATA_SUSPENDED)).isEqualTo(true);
    }

    @Test
    void testStoppedJobStatusEventOfAJobWaitingOnASubflowIsMarkedAwaitingSubflow() {
        Job job = new Job();

        job.setStatus(Job.Status.STOPPED);
        job.setMetadata(
            Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1", MetadataConstants.SUBFLOW_RESUME, true));

        when(jobService.fetchJob(JOB_ID)).thenReturn(Optional.of(job));

        sseStreamApplicationEventListener.onApplicationEvent(JobStatusApplicationEvent.suspended(JOB_ID));

        SseStreamEvent sseStreamEvent = captureSentEvent();

        assertThat(sseStreamEvent.getMetadata(SseStreamEvent.METADATA_SUSPENDED)).isEqualTo(true);
        assertThat(sseStreamEvent.getMetadata(SseStreamEvent.METADATA_AWAITING_SUBFLOW)).isEqualTo(true);
    }

    @Test
    void testStoppedJobStatusEventOfASuspendedJobNotWaitingOnASubflowIsNotMarkedAwaitingSubflow() {
        Job job = new Job();

        job.setStatus(Job.Status.STOPPED);
        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1"));

        when(jobService.fetchJob(JOB_ID)).thenReturn(Optional.of(job));

        sseStreamApplicationEventListener.onApplicationEvent(JobStatusApplicationEvent.suspended(JOB_ID));

        assertThat(captureSentEvent().getMetadata(SseStreamEvent.METADATA_AWAITING_SUBFLOW)).isNull();
    }

    @Test
    void testStoppedJobStatusEventOfAJobStoppedWhileRunningIsNotMarkedSuspended() {
        sseStreamApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.STOPPED));

        assertThat(captureSentEvent().getMetadata(SseStreamEvent.METADATA_SUSPENDED)).isEqualTo(false);
    }

    @Test
    void testCompletedJobStatusEventIsNotMarkedSuspended() {
        sseStreamApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        assertThat(captureSentEvent().getMetadata(SseStreamEvent.METADATA_SUSPENDED)).isNull();
    }

    private List<SseStreamEvent> captureSentEvents(int count) {
        ArgumentCaptor<SseStreamEvent> sseStreamEventArgumentCaptor = ArgumentCaptor.forClass(SseStreamEvent.class);

        verify(messageBroker, times(count)).send(
            eq(SseStreamMessageRoute.SSE_STREAM_EVENTS),
            sseStreamEventArgumentCaptor.capture());

        return sseStreamEventArgumentCaptor.getAllValues();
    }

    private static TaskExecution webhookResponseTaskExecution(FileEntry output, Instant endDate) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(JOB_ID)
            .build();

        taskExecution.setEndDate(endDate);
        taskExecution.setMetadata(Map.of(MetadataConstants.WEBHOOK_RESPONSE, true));
        taskExecution.setOutput(output);

        return taskExecution;
    }

    private SseStreamEvent captureSentEvent() {
        ArgumentCaptor<SseStreamEvent> sseStreamEventArgumentCaptor = ArgumentCaptor.forClass(SseStreamEvent.class);

        verify(messageBroker).send(
            eq(SseStreamMessageRoute.SSE_STREAM_EVENTS),
            sseStreamEventArgumentCaptor.capture());

        return sseStreamEventArgumentCaptor.getValue();
    }
}
