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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class SseStreamApplicationEventListenerTest {

    private static final long JOB_ID = 7L;

    private final MessageBroker messageBroker = mock(MessageBroker.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final SseStreamApplicationEventListener sseStreamApplicationEventListener =
        new SseStreamApplicationEventListener(messageBroker, taskExecutionService);

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
    void testCompletedJobStatusEventDoesNotLookUpTasks() {
        sseStreamApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        assertThat(captureSentEvent().getPayload()).isEqualTo("COMPLETED");

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

    private SseStreamEvent captureSentEvent() {
        ArgumentCaptor<SseStreamEvent> sseStreamEventArgumentCaptor = ArgumentCaptor.forClass(SseStreamEvent.class);

        verify(messageBroker).send(
            eq(SseStreamMessageRoute.SSE_STREAM_EVENTS),
            sseStreamEventArgumentCaptor.capture());

        return sseStreamEventArgumentCaptor.getValue();
    }
}
